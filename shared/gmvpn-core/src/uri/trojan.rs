//! `trojan://` URI parser.
//!
//! Format: `trojan://<password>@<host>:<port>?<params>#<remark>`.
//!
//! Supported params: sni, alpn, type (network), host, path, serviceName,
//! allowInsecure, fp, security.

use percent_encoding::percent_decode_str;
use url::Url;
use uuid::Uuid;

use crate::error::{Error, Result};
use crate::profile::{
    Auth, Profile, Protocol, Security, SecurityMode, Transport, TransportNetwork,
};
use crate::uri::{decode_fragment, opt_string};

/// Parse a `trojan://` URI into a [`Profile`].
pub fn parse(input: &str) -> Result<Profile> {
    let url = Url::parse(input).map_err(|e| Error::InvalidUri(e.to_string()))?;
    if url.scheme() != "trojan" {
        return Err(Error::UnsupportedProtocol(url.scheme().to_string()));
    }

    let raw_password = url.username();
    if raw_password.is_empty() {
        return Err(Error::MissingField("password"));
    }
    let password = percent_decode_str(raw_password)
        .decode_utf8_lossy()
        .into_owned();

    let host = url
        .host_str()
        .ok_or(Error::MissingField("host"))?
        .to_string();
    let port = url.port().ok_or(Error::MissingField("port"))?;
    let remark = decode_fragment(url.fragment());

    let mut network: Option<TransportNetwork> = None;
    let mut sni: Option<String> = None;
    let mut alpn: Vec<String> = Vec::new();
    let mut path: Option<String> = None;
    let mut ws_host: Option<String> = None;
    let mut service_name: Option<String> = None;
    let mut fingerprint: Option<String> = None;
    let mut allow_insecure = false;
    // Trojan is TLS by default; only honor `security=none` as a downgrade.
    let mut security_mode = SecurityMode::Tls;

    for (key, value) in url.query_pairs() {
        match key.as_ref() {
            "type" => network = map_network(value.as_ref()),
            "security" => {
                security_mode = match value.as_ref() {
                    "none" => SecurityMode::None,
                    "reality" => SecurityMode::Reality,
                    _ => SecurityMode::Tls,
                };
            }
            "sni" | "peer" => sni = opt_string(value.into_owned()),
            "alpn" => {
                alpn = value
                    .split(',')
                    .map(|s| s.trim().to_string())
                    .filter(|s| !s.is_empty())
                    .collect();
            }
            "path" => path = opt_string(value.into_owned()),
            "host" => ws_host = opt_string(value.into_owned()),
            "serviceName" => service_name = opt_string(value.into_owned()),
            "fp" => fingerprint = opt_string(value.into_owned()),
            "allowInsecure" => {
                allow_insecure = matches!(value.as_ref(), "1" | "true");
            }
            _ => {}
        }
    }

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
        allow_insecure,
        fingerprint,
        reality: None,
    };

    Ok(Profile {
        id: Uuid::new_v4(),
        name: remark.clone().unwrap_or_else(|| format!("{host}:{port}")),
        remark,
        protocol: Protocol::Trojan,
        server: host,
        port,
        auth: Auth::Trojan { password },
        transport,
        security,
        tags: Vec::new(),
    })
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

    #[test]
    fn parses_minimal_trojan() {
        let p = parse("trojan://hunter2@trojan.example:443#main").unwrap();
        assert_eq!(p.protocol, Protocol::Trojan);
        assert_eq!(p.server, "trojan.example");
        assert_eq!(p.port, 443);
        assert_eq!(p.remark.as_deref(), Some("main"));
        assert!(matches!(p.security.mode, SecurityMode::Tls));
        if let Auth::Trojan { password } = &p.auth {
            assert_eq!(password, "hunter2");
        } else {
            panic!("expected trojan auth");
        }
    }

    #[test]
    fn parses_percent_encoded_password() {
        let p = parse("trojan://p%40ss%2Fword@t.example:443").unwrap();
        if let Auth::Trojan { password } = &p.auth {
            assert_eq!(password, "p@ss/word");
        } else {
            panic!("expected trojan auth");
        }
    }

    #[test]
    fn honors_ws_transport_and_alpn() {
        let p = parse(
            "trojan://pw@t.example:443?type=ws&path=/chat&host=cdn.example\
             &alpn=h2,http/1.1&sni=t.example",
        )
        .unwrap();
        assert_eq!(p.transport.network, Some(TransportNetwork::Ws));
        assert_eq!(p.transport.path.as_deref(), Some("/chat"));
        assert_eq!(p.transport.host.as_deref(), Some("cdn.example"));
        assert_eq!(p.security.alpn, vec!["h2", "http/1.1"]);
        assert_eq!(p.security.sni.as_deref(), Some("t.example"));
    }

    #[test]
    fn rejects_missing_password() {
        let err = parse("trojan://@t.example:443").unwrap_err();
        assert!(matches!(err, Error::MissingField("password")));
    }

    #[test]
    fn rejects_wrong_scheme() {
        let err = parse("vless://x@h:1").unwrap_err();
        assert!(matches!(err, Error::UnsupportedProtocol(_)));
    }
}
