//! `hysteria2://` (and `hy2://` alias) URI parser.
//!
//! Format reference (de-facto standard used by hysteria2 clients):
//!   hysteria2://<auth>@<host>:<port>?<params>#<remark>
//!
//! The userinfo before `@` is the auth string (a token, or `user:pass`).
//! Supported params: sni (or peer), insecure (0/1 | true/false), obfs,
//! obfs-password. Hysteria2 is always TLS-over-QUIC, so security mode is
//! forced to TLS.
//!
//! Transport-specific knobs (up/down bandwidth, port hopping, Salamander
//! obfs) are intentionally not modelled yet: Xray uses BBR by default and
//! the majority of hysteria2 servers work with auth + TLS + SNI alone.
//! See `docs/adr/0006-hysteria2-through-xray.md`.

use percent_encoding::percent_decode_str;
use url::Url;

use crate::error::{Error, Result};
use crate::profile::{Auth, Profile, Protocol, Security, SecurityMode};
use crate::uri::decode_fragment;
use uuid::Uuid;

/// Parse a `hysteria2://` / `hy2://` URI into a [`Profile`].
pub fn parse(input: &str) -> Result<Profile> {
    let url = Url::parse(input).map_err(|e| Error::InvalidUri(e.to_string()))?;
    if url.scheme() != "hysteria2" && url.scheme() != "hy2" {
        return Err(Error::UnsupportedProtocol(url.scheme().to_string()));
    }

    // Auth = the whole userinfo, URL-decoded. `user:pass` is preserved as
    // `user:pass`; a bare token is preserved as-is. Empty userinfo is a
    // valid (auth-less) hysteria2 endpoint.
    let user = percent_decode_str(url.username())
        .decode_utf8_lossy()
        .into_owned();
    let password = match url.password() {
        Some(pass) if !pass.is_empty() => {
            let pass = percent_decode_str(pass).decode_utf8_lossy().into_owned();
            format!("{user}:{pass}")
        }
        _ => user,
    };

    let host = url
        .host_str()
        .ok_or(Error::MissingField("host"))?
        .to_string();
    let port = url.port().ok_or(Error::MissingField("port"))?;

    let remark = decode_fragment(url.fragment());

    let mut sni: Option<String> = None;
    let mut allow_insecure = false;
    for (key, value) in url.query_pairs() {
        match key.as_ref() {
            "sni" | "peer" => {
                let v = value.into_owned();
                if !v.is_empty() {
                    sni = Some(v);
                }
            }
            "insecure" | "allowInsecure" => {
                allow_insecure = matches!(value.as_ref(), "1" | "true");
            }
            _ => {}
        }
    }

    let security = Security {
        mode: SecurityMode::Tls,
        sni,
        alpn: Vec::new(),
        allow_insecure,
        fingerprint: None,
        reality: None,
    };

    Ok(Profile {
        id: Uuid::new_v4(),
        name: remark.clone().unwrap_or_else(|| format!("{host}:{port}")),
        remark,
        protocol: Protocol::Hysteria2,
        server: host,
        port,
        auth: Auth::Hysteria2 { password },
        transport: crate::profile::Transport::default(),
        security,
        tags: Vec::new(),
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_minimal_hysteria2() {
        let p = parse("hysteria2://letmein@hy2.example:443").unwrap();
        assert_eq!(p.protocol, Protocol::Hysteria2);
        assert_eq!(p.server, "hy2.example");
        assert_eq!(p.port, 443);
        assert!(matches!(p.security.mode, SecurityMode::Tls));
        if let Auth::Hysteria2 { password } = &p.auth {
            assert_eq!(password, "letmein");
        } else {
            panic!("expected hysteria2 auth");
        }
    }

    #[test]
    fn accepts_hy2_alias_and_params() {
        let p = parse("hy2://tok@h.example:8443?sni=cdn.example&insecure=1#RU-1").unwrap();
        assert_eq!(p.protocol, Protocol::Hysteria2);
        assert_eq!(p.security.sni.as_deref(), Some("cdn.example"));
        assert!(p.security.allow_insecure);
        assert_eq!(p.remark.as_deref(), Some("RU-1"));
    }

    #[test]
    fn preserves_user_colon_pass_auth() {
        let p = parse("hysteria2://alice:s3cret@h.example:443").unwrap();
        if let Auth::Hysteria2 { password } = &p.auth {
            assert_eq!(password, "alice:s3cret");
        } else {
            panic!("expected hysteria2 auth");
        }
    }

    #[test]
    fn requires_port() {
        let err = parse("hysteria2://tok@h.example").unwrap_err();
        assert!(matches!(err, Error::MissingField("port")));
    }

    #[test]
    fn rejects_wrong_scheme() {
        let err = parse("trojan://pw@h.example:443").unwrap_err();
        assert!(matches!(err, Error::UnsupportedProtocol(_)));
    }

    #[test]
    fn peer_is_sni_alias() {
        let p = parse("hysteria2://tok@h.example:443?peer=sni.example").unwrap();
        assert_eq!(p.security.sni.as_deref(), Some("sni.example"));
    }
}
