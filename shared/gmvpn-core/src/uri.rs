//! Profile URI parsers. Currently supports `vless://`; other protocols will
//! be added as real test vectors are collected.
//!
//! Format reference (de-facto standard used by v2rayN / v2rayNG):
//!   vless://<uuid>@<host>:<port>?<params>#<remark>
//!
//! Supported params: type, security, sni, flow, pbk, sid, spx, fp, alpn,
//! path, host, serviceName, encryption.

use percent_encoding::percent_decode_str;
use url::Url;
use uuid::Uuid;

use crate::error::{Error, Result};
use crate::profile::{
    Auth, Profile, Protocol, RealityConfig, Security, SecurityMode, Transport, TransportNetwork,
};

/// Parse a `vless://` URI into a [`Profile`].
pub fn parse_vless(input: &str) -> Result<Profile> {
    let url = Url::parse(input).map_err(|e| Error::InvalidUri(e.to_string()))?;
    if url.scheme() != "vless" {
        return Err(Error::UnsupportedProtocol(url.scheme().to_string()));
    }

    let uuid = Uuid::parse_str(url.username()).map_err(|_| Error::InvalidValue {
        field: "uuid",
        reason: "not a uuid".into(),
    })?;

    let host = url
        .host_str()
        .ok_or(Error::MissingField("host"))?
        .to_string();
    let port = url.port().ok_or(Error::MissingField("port"))?;

    let remark = url
        .fragment()
        .map(|f| percent_decode_str(f).decode_utf8_lossy().into_owned())
        .filter(|s| !s.is_empty());

    let mut network: Option<TransportNetwork> = None;
    let mut security_mode = SecurityMode::None;
    let mut sni: Option<String> = None;
    let mut flow: Option<String> = None;
    let mut encryption: Option<String> = None;
    let mut path: Option<String> = None;
    let mut ws_host: Option<String> = None;
    let mut service_name: Option<String> = None;
    let mut fingerprint: Option<String> = None;
    let mut alpn: Vec<String> = Vec::new();
    let mut reality_pbk: Option<String> = None;
    let mut reality_sid: Option<String> = None;
    let mut reality_spx: Option<String> = None;

    for (key, value) in url.query_pairs() {
        match key.as_ref() {
            "type" => network = map_network(value.as_ref()),
            "security" => security_mode = map_security(value.as_ref()),
            "sni" => sni = opt(value.into_owned()),
            "flow" => flow = opt(value.into_owned()),
            "encryption" => encryption = opt(value.into_owned()),
            "path" => path = opt(value.into_owned()),
            "host" => ws_host = opt(value.into_owned()),
            "serviceName" => service_name = opt(value.into_owned()),
            "fp" => fingerprint = opt(value.into_owned()),
            "alpn" => {
                alpn = value
                    .split(',')
                    .map(|s| s.trim().to_string())
                    .filter(|s| !s.is_empty())
                    .collect();
            }
            "pbk" => reality_pbk = opt(value.into_owned()),
            "sid" => reality_sid = opt(value.into_owned()),
            "spx" => reality_spx = opt(value.into_owned()),
            _ => {}
        }
    }

    let reality = if matches!(security_mode, SecurityMode::Reality) {
        match (reality_pbk, reality_sid) {
            (Some(pbk), Some(sid)) => Some(RealityConfig {
                public_key: pbk,
                short_id: sid,
                spider_x: reality_spx,
            }),
            _ => {
                return Err(Error::InvalidValue {
                    field: "reality",
                    reason: "pbk and sid are required when security=reality".into(),
                });
            }
        }
    } else {
        None
    };

    let transport = Transport {
        network,
        host: ws_host,
        path,
        service_name,
    };

    let security = Security {
        mode: security_mode,
        sni,
        alpn,
        allow_insecure: false,
        fingerprint,
        reality,
    };

    Ok(Profile {
        id: Uuid::new_v4(),
        name: remark.clone().unwrap_or_else(|| format!("{host}:{port}")),
        remark,
        protocol: Protocol::Vless,
        server: host,
        port,
        auth: Auth::Vless {
            uuid,
            flow,
            encryption,
        },
        transport,
        security,
        tags: Vec::new(),
    })
}

fn opt(s: String) -> Option<String> {
    if s.is_empty() {
        None
    } else {
        Some(s)
    }
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

fn map_security(value: &str) -> SecurityMode {
    match value {
        "tls" => SecurityMode::Tls,
        "reality" => SecurityMode::Reality,
        _ => SecurityMode::None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_minimal_vless_tcp() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@example.com:443";
        let p = parse_vless(uri).unwrap();
        assert_eq!(p.server, "example.com");
        assert_eq!(p.port, 443);
        matches!(p.auth, Auth::Vless { .. });
        assert!(matches!(p.security.mode, SecurityMode::None));
    }

    #[test]
    fn parses_vless_reality() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@host.example:443\
                   ?type=tcp&security=reality&sni=www.cloudflare.com&flow=xtls-rprx-vision\
                   &pbk=abc123&sid=deadbeef&fp=chrome#RU-1";
        let p = parse_vless(uri).unwrap();
        assert!(matches!(p.security.mode, SecurityMode::Reality));
        assert_eq!(p.security.sni.as_deref(), Some("www.cloudflare.com"));
        assert_eq!(p.security.fingerprint.as_deref(), Some("chrome"));
        let reality = p.security.reality.as_ref().unwrap();
        assert_eq!(reality.public_key, "abc123");
        assert_eq!(reality.short_id, "deadbeef");
        assert_eq!(p.remark.as_deref(), Some("RU-1"));
        if let Auth::Vless {
            flow: Some(flow), ..
        } = &p.auth
        {
            assert_eq!(flow, "xtls-rprx-vision");
        } else {
            panic!("expected vless auth with flow");
        }
    }

    #[test]
    fn reality_requires_pbk_and_sid() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@host.example:443\
                   ?security=reality&sni=x.example";
        let err = parse_vless(uri).unwrap_err();
        assert!(matches!(
            err,
            Error::InvalidValue {
                field: "reality",
                ..
            }
        ));
    }

    #[test]
    fn rejects_non_vless_scheme() {
        let uri = "trojan://pw@host:443";
        let err = parse_vless(uri).unwrap_err();
        assert!(matches!(err, Error::UnsupportedProtocol(_)));
    }

    #[test]
    fn decodes_percent_encoded_remark() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@host.example:443#%E2%9C%93%20OK";
        let p = parse_vless(uri).unwrap();
        assert_eq!(p.remark.as_deref(), Some("\u{2713} OK"));
    }
}
