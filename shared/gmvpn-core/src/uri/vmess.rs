//! `vmess://` URI parser.
//!
//! Format reference (v2rayN / v2rayNG convention):
//!   vmess://BASE64(JSON)
//!
//! The JSON payload carries `add`, `port`, `id`, `net`, `tls`, and other
//! fields documented at
//! https://github.com/2dust/v2rayN/wiki/Description-of-VMess-share-link.

use base64::engine::general_purpose::STANDARD as B64_STD;
use base64::engine::general_purpose::STANDARD_NO_PAD as B64_STD_NO_PAD;
use base64::Engine;
use serde::Deserialize;
use uuid::Uuid;

use crate::error::{Error, Result};
use crate::profile::{
    Auth, Profile, Protocol, Security, SecurityMode, Transport, TransportNetwork,
};

#[derive(Debug, Deserialize)]
struct RawVmess {
    #[serde(default)]
    ps: Option<String>,
    add: String,
    #[serde(deserialize_with = "port_de")]
    port: u16,
    id: String,
    #[serde(default)]
    aid: Option<StringOrInt>,
    #[serde(default)]
    scy: Option<String>,
    #[serde(default)]
    net: Option<String>,
    #[serde(default)]
    host: Option<String>,
    #[serde(default)]
    path: Option<String>,
    #[serde(default)]
    tls: Option<String>,
    #[serde(default)]
    sni: Option<String>,
    #[serde(default)]
    alpn: Option<String>,
    #[serde(default)]
    fp: Option<String>,
}

#[derive(Debug, Deserialize)]
#[serde(untagged)]
enum StringOrInt {
    Str(String),
    Int(i64),
}

impl StringOrInt {
    fn to_u16(&self) -> u16 {
        match self {
            Self::Int(n) => u16::try_from(*n).unwrap_or(0),
            Self::Str(s) => s.trim().parse().unwrap_or(0),
        }
    }
}

fn port_de<'de, D: serde::Deserializer<'de>>(d: D) -> std::result::Result<u16, D::Error> {
    let raw = StringOrInt::deserialize(d)?;
    Ok(raw.to_u16())
}

/// Parse a `vmess://` URI into a [`Profile`].
pub fn parse(input: &str) -> Result<Profile> {
    let body = input
        .strip_prefix("vmess://")
        .ok_or_else(|| Error::UnsupportedProtocol("vmess".to_string()))?;

    let decoded = decode_b64(body)?;
    let text = std::str::from_utf8(&decoded)
        .map_err(|e| Error::Decode(format!("vmess payload is not utf-8: {e}")))?;

    let raw: RawVmess =
        serde_json::from_str(text).map_err(|e| Error::Decode(format!("vmess json: {e}")))?;

    if raw.port == 0 {
        return Err(Error::InvalidValue {
            field: "port",
            reason: "vmess port is zero or unparseable".into(),
        });
    }

    let uuid = Uuid::parse_str(&raw.id).map_err(|_| Error::InvalidValue {
        field: "id",
        reason: "vmess id is not a uuid".into(),
    })?;

    let network = raw.net.as_deref().and_then(map_network);
    let transport = Transport {
        network,
        host: raw.host.clone().filter(|s| !s.is_empty()),
        path: raw.path.clone().filter(|s| !s.is_empty()),
        service_name: None,
    };

    let security_mode = match raw.tls.as_deref().unwrap_or("") {
        "tls" => SecurityMode::Tls,
        "reality" => SecurityMode::Reality,
        _ => SecurityMode::None,
    };

    let alpn = raw
        .alpn
        .as_deref()
        .map(|s| {
            s.split(',')
                .map(|p| p.trim().to_string())
                .filter(|p| !p.is_empty())
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();

    let security = Security {
        mode: security_mode,
        sni: raw.sni.clone().filter(|s| !s.is_empty()),
        alpn,
        allow_insecure: false,
        fingerprint: raw.fp.clone().filter(|s| !s.is_empty()),
        reality: None,
    };

    let aid = raw.aid.as_ref().map_or(0, StringOrInt::to_u16);
    let auth = Auth::Vmess {
        uuid,
        alter_id: aid,
        security: raw.scy.clone().filter(|s| !s.is_empty()),
    };

    let remark = raw.ps.clone().filter(|s| !s.is_empty());

    Ok(Profile {
        id: Uuid::new_v4(),
        name: remark
            .clone()
            .unwrap_or_else(|| format!("{}:{}", raw.add, raw.port)),
        remark,
        protocol: Protocol::Vmess,
        server: raw.add,
        port: raw.port,
        auth,
        transport,
        security,
        tags: Vec::new(),
    })
}

fn decode_b64(s: &str) -> Result<Vec<u8>> {
    // vmess links are sometimes padded and sometimes not. Try both.
    let cleaned: String = s.chars().filter(|c| !c.is_whitespace()).collect();
    if let Ok(bytes) = B64_STD.decode(&cleaned) {
        return Ok(bytes);
    }
    B64_STD_NO_PAD
        .decode(&cleaned)
        .map_err(|e| Error::Decode(format!("vmess base64: {e}")))
}

fn map_network(value: &str) -> Option<TransportNetwork> {
    match value {
        "tcp" => Some(TransportNetwork::Tcp),
        "ws" => Some(TransportNetwork::Ws),
        "grpc" => Some(TransportNetwork::Grpc),
        "http" | "h2" => Some(TransportNetwork::Http2),
        "quic" => Some(TransportNetwork::Quic),
        "kcp" => Some(TransportNetwork::Kcp),
        "httpupgrade" => Some(TransportNetwork::Httpupgrade),
        "splithttp" => Some(TransportNetwork::Splithttp),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use base64::engine::general_purpose::STANDARD as B64;

    fn encode(json: &str) -> String {
        format!("vmess://{}", B64.encode(json))
    }

    #[test]
    fn parses_minimal_vmess() {
        let json = r#"{
            "v":"2","ps":"HK-1","add":"vmess.example","port":"443",
            "id":"11111111-1111-1111-1111-111111111111","aid":"0",
            "net":"ws","tls":"tls","sni":"vmess.example","path":"/chat"
        }"#;
        let p = parse(&encode(json)).unwrap();
        assert_eq!(p.protocol, Protocol::Vmess);
        assert_eq!(p.server, "vmess.example");
        assert_eq!(p.port, 443);
        assert_eq!(p.remark.as_deref(), Some("HK-1"));
        assert!(matches!(p.security.mode, SecurityMode::Tls));
        assert_eq!(p.transport.network, Some(TransportNetwork::Ws));
        assert_eq!(p.transport.path.as_deref(), Some("/chat"));
        if let Auth::Vmess { alter_id, .. } = &p.auth {
            assert_eq!(*alter_id, 0);
        } else {
            panic!("expected vmess auth");
        }
    }

    #[test]
    fn accepts_numeric_port_and_aid() {
        let json = r#"{
            "v":"2","add":"h","port":8443,"id":"11111111-1111-1111-1111-111111111111",
            "aid":2,"net":"tcp"
        }"#;
        let p = parse(&encode(json)).unwrap();
        assert_eq!(p.port, 8443);
        if let Auth::Vmess { alter_id, .. } = &p.auth {
            assert_eq!(*alter_id, 2);
        } else {
            panic!("expected vmess auth");
        }
    }

    #[test]
    fn rejects_non_base64_payload() {
        let err = parse("vmess://not base64!!!").unwrap_err();
        assert!(matches!(err, Error::Decode(_)));
    }

    #[test]
    fn rejects_bad_uuid() {
        let json = r#"{"add":"h","port":"443","id":"not-a-uuid","net":"tcp"}"#;
        let err = parse(&encode(json)).unwrap_err();
        assert!(matches!(err, Error::InvalidValue { field: "id", .. }));
    }

    #[test]
    fn rejects_zero_port() {
        let json = r#"{"add":"h","port":"0","id":"11111111-1111-1111-1111-111111111111"}"#;
        let err = parse(&encode(json)).unwrap_err();
        assert!(matches!(err, Error::InvalidValue { field: "port", .. }));
    }
}
