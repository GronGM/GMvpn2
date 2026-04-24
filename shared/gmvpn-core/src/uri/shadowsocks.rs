//! `ss://` URI parser. Supports two wire forms:
//!
//! - **SIP002** (current):
//!   `ss://BASE64URL(method:password)@host:port[/?plugin=…][#remark]`
//!   The userinfo part is URL-safe base64 of `method:password`; host and
//!   port are plaintext.
//!
//! - **Legacy**:
//!   `ss://BASE64(method:password@host:port)[#remark]`
//!   The whole authority is base64-encoded.

use base64::engine::general_purpose::{STANDARD, STANDARD_NO_PAD, URL_SAFE, URL_SAFE_NO_PAD};
use base64::Engine;
use uuid::Uuid;

use crate::error::{Error, Result};
use crate::profile::{Auth, Profile, Protocol, Security, Transport};

/// Parse an `ss://` URI into a [`Profile`].
pub fn parse(input: &str) -> Result<Profile> {
    let body = input.strip_prefix("ss://").ok_or_else(|| {
        Error::UnsupportedProtocol(input.split("://").next().unwrap_or("").to_string())
    })?;

    // SIP002 is identified by a `@` separating the base64-userinfo from
    // the plaintext authority.
    if body.contains('@') {
        parse_sip002(body)
    } else {
        parse_legacy(body)
    }
}

fn parse_sip002(body: &str) -> Result<Profile> {
    let (userinfo, rest) = body.split_once('@').ok_or(Error::Decode(
        "ss SIP002 payload missing '@' separator".to_string(),
    ))?;

    // `rest` is `host:port[/?plugin=…][#remark]` — split off fragment
    // and path/query segments in order.
    let (rest, fragment) = match rest.split_once('#') {
        Some((r, f)) => (r, Some(f)),
        None => (rest, None),
    };
    let authority = rest.split(['/', '?']).next().unwrap_or(rest);

    let (host, port) = split_host_port(authority)?;

    let decoded = decode_b64_any(userinfo)?;
    let text = std::str::from_utf8(&decoded)
        .map_err(|e| Error::Decode(format!("ss userinfo utf-8: {e}")))?;
    let (method, password) = split_method_password(text)?;

    let remark = fragment
        .map(|f| {
            percent_encoding::percent_decode_str(f)
                .decode_utf8_lossy()
                .into_owned()
        })
        .filter(|s| !s.is_empty());

    Ok(assemble(host, port, method, password, remark))
}

fn parse_legacy(input: &str) -> Result<Profile> {
    let without_scheme = input.strip_prefix("ss://").unwrap_or(input);
    // Split the fragment (`#remark`) off before decoding base64.
    let (encoded, fragment) = match without_scheme.split_once('#') {
        Some((e, f)) => (e, Some(f)),
        None => (without_scheme, None),
    };
    // Drop any trailing `/?plugin=…` — legacy links rarely have it, but
    // guard anyway.
    let encoded = encoded.split(['/', '?']).next().unwrap_or(encoded);

    let decoded = decode_b64_any(encoded)?;
    let text = std::str::from_utf8(&decoded)
        .map_err(|e| Error::Decode(format!("ss legacy utf-8: {e}")))?;

    // Expected form: method:password@host:port
    let (creds, authority) = text.rsplit_once('@').ok_or(Error::Decode(
        "ss legacy payload missing '@' separator".to_string(),
    ))?;
    let (method, password) = split_method_password(creds)?;
    let (host, port) = split_host_port(authority)?;

    let remark = fragment
        .map(|f| {
            percent_encoding::percent_decode_str(f)
                .decode_utf8_lossy()
                .into_owned()
        })
        .filter(|s| !s.is_empty());

    Ok(assemble(host, port, method, password, remark))
}

fn assemble(
    host: String,
    port: u16,
    method: String,
    password: String,
    remark: Option<String>,
) -> Profile {
    Profile {
        id: Uuid::new_v4(),
        name: remark.clone().unwrap_or_else(|| format!("{host}:{port}")),
        remark,
        protocol: Protocol::Shadowsocks,
        server: host,
        port,
        auth: Auth::Shadowsocks { method, password },
        transport: Transport::default(),
        security: Security::default(),
        tags: Vec::new(),
    }
}

fn split_method_password(text: &str) -> Result<(String, String)> {
    text.split_once(':')
        .map(|(m, p)| (m.to_string(), p.to_string()))
        .filter(|(m, p)| !m.is_empty() && !p.is_empty())
        .ok_or(Error::Decode(
            "ss userinfo missing 'method:password' shape".to_string(),
        ))
}

fn split_host_port(text: &str) -> Result<(String, u16)> {
    let (host, port_str) = text.rsplit_once(':').ok_or(Error::Decode(
        "ss legacy authority missing port".to_string(),
    ))?;
    let port: u16 = port_str.parse().map_err(|_| Error::InvalidValue {
        field: "port",
        reason: format!("not a u16: {port_str}"),
    })?;
    if host.is_empty() {
        return Err(Error::MissingField("host"));
    }
    Ok((host.to_string(), port))
}

fn decode_b64_any(s: &str) -> Result<Vec<u8>> {
    let cleaned: String = s.chars().filter(|c| !c.is_whitespace()).collect();
    for engine in [&URL_SAFE, &URL_SAFE_NO_PAD, &STANDARD, &STANDARD_NO_PAD] {
        if let Ok(bytes) = engine.decode(&cleaned) {
            return Ok(bytes);
        }
    }
    Err(Error::Decode(format!("ss base64: {cleaned}")))
}

#[cfg(test)]
mod tests {
    use super::*;
    use base64::engine::general_purpose::URL_SAFE_NO_PAD as B64;

    #[test]
    fn parses_sip002() {
        let userinfo = B64.encode(b"chacha20-ietf-poly1305:hunter2");
        let uri = format!("ss://{userinfo}@ss.example:8388#JP-1");
        let p = parse(&uri).unwrap();
        assert_eq!(p.protocol, Protocol::Shadowsocks);
        assert_eq!(p.server, "ss.example");
        assert_eq!(p.port, 8388);
        assert_eq!(p.remark.as_deref(), Some("JP-1"));
        if let Auth::Shadowsocks { method, password } = &p.auth {
            assert_eq!(method, "chacha20-ietf-poly1305");
            assert_eq!(password, "hunter2");
        } else {
            panic!("expected shadowsocks auth");
        }
    }

    #[test]
    fn parses_legacy() {
        // method:password@host:port, base64 of the whole authority
        let payload =
            base64::engine::general_purpose::STANDARD.encode(b"aes-256-gcm:pw@ss.example:8388");
        let uri = format!("ss://{payload}#legacy");
        let p = parse(&uri).unwrap();
        assert_eq!(p.server, "ss.example");
        assert_eq!(p.port, 8388);
        if let Auth::Shadowsocks { method, password } = &p.auth {
            assert_eq!(method, "aes-256-gcm");
            assert_eq!(password, "pw");
        } else {
            panic!("expected shadowsocks auth");
        }
    }

    #[test]
    fn rejects_wrong_scheme() {
        let err = parse("trojan://pw@h:1").unwrap_err();
        assert!(matches!(err, Error::UnsupportedProtocol(_)));
    }

    #[test]
    fn rejects_non_base64_userinfo() {
        let err = parse("ss://not*base64@ss.example:8388").unwrap_err();
        assert!(matches!(err, Error::Decode(_)));
    }

    #[test]
    fn rejects_malformed_legacy_body() {
        let payload = base64::engine::general_purpose::STANDARD.encode(b"garbage");
        let err = parse(&format!("ss://{payload}")).unwrap_err();
        assert!(matches!(err, Error::Decode(_)));
    }
}
