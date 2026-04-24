//! Decode subscription bodies into a flat list of profiles.
//!
//! This module never makes network calls. Callers (platform layers)
//! fetch the HTTP body, hand us the bytes and the declared format, and
//! receive back parsed profiles. Partial failures are reported alongside
//! the successes so one bad URI never loses the whole subscription.

use base64::engine::general_purpose::{STANDARD, STANDARD_NO_PAD, URL_SAFE, URL_SAFE_NO_PAD};
use base64::Engine;
use serde::Deserialize;
use uuid::Uuid;

use crate::error::{Error, Result};
use crate::profile::{Auth, Profile, Protocol, Security, Transport};
use crate::uri;

/// Wire format of a subscription response.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SubscriptionFormat {
    /// One URI per line, `#` introduces a comment.
    UriList,
    /// Base64 encoding of the above — common "classic" subscription.
    Base64UriList,
    /// SIP008 JSON document (Shadowsocks-only).
    Sip008,
}

/// A single parsing failure encountered while decoding a subscription.
/// Returned alongside successfully-parsed profiles so the caller can
/// surface a "imported N / M failed" summary.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DecodeWarning {
    /// Zero-based index of the input (line number for uri-list,
    /// element index for sip008).
    pub index: usize,
    /// The offending input, trimmed.
    pub input: String,
    /// Human-readable reason.
    pub reason: String,
}

/// Result of a successful decode.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub struct DecodeOutput {
    pub profiles: Vec<Profile>,
    pub warnings: Vec<DecodeWarning>,
}

/// Decode a subscription body.
pub fn decode(body: &[u8], format: SubscriptionFormat) -> Result<DecodeOutput> {
    match format {
        SubscriptionFormat::UriList => decode_uri_list(body),
        SubscriptionFormat::Base64UriList => {
            let inner = decode_b64_any(body)?;
            decode_uri_list(&inner)
        }
        SubscriptionFormat::Sip008 => decode_sip008(body),
    }
}

fn decode_uri_list(body: &[u8]) -> Result<DecodeOutput> {
    let text =
        std::str::from_utf8(body).map_err(|e| Error::Decode(format!("subscription utf-8: {e}")))?;

    let mut out = DecodeOutput::default();
    for (idx, raw) in text.lines().enumerate() {
        let trimmed = raw.trim();
        if trimmed.is_empty() || trimmed.starts_with('#') {
            continue;
        }
        match uri::parse(trimmed) {
            Ok(profile) => out.profiles.push(profile),
            Err(e) => out.warnings.push(DecodeWarning {
                index: idx,
                input: truncate(trimmed, 120),
                reason: e.to_string(),
            }),
        }
    }
    Ok(out)
}

#[derive(Debug, Deserialize)]
struct Sip008Doc {
    servers: Vec<Sip008Server>,
}

#[derive(Debug, Deserialize)]
struct Sip008Server {
    server: String,
    server_port: u16,
    method: String,
    password: String,
    #[serde(default)]
    remarks: Option<String>,
}

fn decode_sip008(body: &[u8]) -> Result<DecodeOutput> {
    let doc: Sip008Doc =
        serde_json::from_slice(body).map_err(|e| Error::Decode(format!("sip008 json: {e}")))?;

    let mut out = DecodeOutput::default();
    for (idx, s) in doc.servers.into_iter().enumerate() {
        if s.server.is_empty() || s.server_port == 0 || s.method.is_empty() || s.password.is_empty()
        {
            out.warnings.push(DecodeWarning {
                index: idx,
                input: format!("{}:{}", s.server, s.server_port),
                reason: "sip008 entry missing required field".into(),
            });
            continue;
        }
        let remark = s.remarks.filter(|r| !r.is_empty());
        let name = remark
            .clone()
            .unwrap_or_else(|| format!("{}:{}", s.server, s.server_port));
        out.profiles.push(Profile {
            id: Uuid::new_v4(),
            name,
            remark,
            protocol: Protocol::Shadowsocks,
            server: s.server,
            port: s.server_port,
            auth: Auth::Shadowsocks {
                method: s.method,
                password: s.password,
            },
            transport: Transport::default(),
            security: Security::default(),
            tags: Vec::new(),
        });
    }
    Ok(out)
}

fn decode_b64_any(body: &[u8]) -> Result<Vec<u8>> {
    let cleaned: Vec<u8> = body
        .iter()
        .copied()
        .filter(|b| !b.is_ascii_whitespace())
        .collect();
    for engine in [&STANDARD, &STANDARD_NO_PAD, &URL_SAFE, &URL_SAFE_NO_PAD] {
        if let Ok(bytes) = engine.decode(&cleaned) {
            return Ok(bytes);
        }
    }
    Err(Error::Decode(
        "subscription base64 decode failed".to_string(),
    ))
}

fn truncate(s: &str, max: usize) -> String {
    if s.len() <= max {
        s.to_string()
    } else {
        let mut cut = max;
        while !s.is_char_boundary(cut) {
            cut -= 1;
        }
        format!("{}…", &s[..cut])
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn decodes_uri_list_with_comments_and_bad_line() {
        let body = concat!(
            "# free servers\n",
            "vless://11111111-1111-1111-1111-111111111111@a.example:443\n",
            "\n",
            "trojan://pw@b.example:443#B\n",
            "not-a-uri\n",
        );
        let out = decode(body.as_bytes(), SubscriptionFormat::UriList).unwrap();
        assert_eq!(out.profiles.len(), 2);
        assert_eq!(out.warnings.len(), 1);
        assert_eq!(out.warnings[0].input, "not-a-uri");
    }

    #[test]
    fn decodes_base64_uri_list() {
        let inner = "vless://11111111-1111-1111-1111-111111111111@a.example:443\n";
        let encoded = STANDARD.encode(inner);
        let out = decode(encoded.as_bytes(), SubscriptionFormat::Base64UriList).unwrap();
        assert_eq!(out.profiles.len(), 1);
        assert_eq!(out.profiles[0].server, "a.example");
    }

    #[test]
    fn decodes_sip008() {
        let json = br#"{
            "version":1,
            "servers":[
              {"server":"ss1.example","server_port":8388,"method":"aes-256-gcm","password":"pw","remarks":"JP"},
              {"server":"","server_port":0,"method":"","password":""}
            ]
        }"#;
        let out = decode(json, SubscriptionFormat::Sip008).unwrap();
        assert_eq!(out.profiles.len(), 1);
        assert_eq!(out.warnings.len(), 1);
        let p = &out.profiles[0];
        assert_eq!(p.protocol, Protocol::Shadowsocks);
        assert_eq!(p.port, 8388);
        assert_eq!(p.remark.as_deref(), Some("JP"));
    }

    #[test]
    fn rejects_non_base64_classic_subscription() {
        // Leading zeros — not valid base64 of an all-URI body, and the
        // fallback engines should all fail too.
        let body = b"!!! not base64 at all !!!";
        let err = decode(body, SubscriptionFormat::Base64UriList).unwrap_err();
        assert!(matches!(err, Error::Decode(_)));
    }

    #[test]
    fn rejects_malformed_sip008() {
        let err = decode(b"{", SubscriptionFormat::Sip008).unwrap_err();
        assert!(matches!(err, Error::Decode(_)));
    }
}
