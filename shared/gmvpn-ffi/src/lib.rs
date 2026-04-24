//! UniFFI boundary for GMvpn2 clients.
//!
//! This crate exposes a typed, stable API that binds cleanly to Kotlin
//! (Android), Swift (iOS/macOS), Python (tests / tooling), and Ruby.
//! All public items are annotated with `uniffi::export` / the matching
//! derive macros; the scaffolding is set up at the bottom of the file.
//!
//! The types defined here (the `Ffi*` records and enums) are FFI DTOs.
//! They mirror the domain model in `gmvpn-core` but use FFI-friendly
//! representations (for example `String` instead of `uuid::Uuid` and
//! `u32` for numeric fields that would bind awkwardly as `usize`).

// UniFFI requires exported functions to take owned values, not refs,
// so the scaffolding can drop them after the call. These lints fight
// that contract and are irrelevant at a typed FFI boundary.
#![allow(clippy::needless_pass_by_value, clippy::must_use_candidate)]

mod conv;
mod dto;
mod error;

pub use dto::{
    FfiAuth, FfiDecodeOutput, FfiDecodeWarning, FfiProfile, FfiProtocol, FfiRealityConfig,
    FfiSecurity, FfiSecurityMode, FfiSubscriptionFormat, FfiTransport, FfiTransportNetwork,
};
pub use error::GmvpnError;

use gmvpn_core::{subscription, uri, SubscriptionFormat};

/// Parse any supported profile URI (vless / vmess / trojan / ss) into
/// a fully-typed profile record.
#[uniffi::export]
pub fn parse_profile_uri(uri: String) -> Result<FfiProfile, GmvpnError> {
    let profile = uri::parse(&uri).map_err(GmvpnError::from)?;
    Ok(profile.into())
}

/// Decode a fetched subscription body. Partial failures surface as
/// entries in [`FfiDecodeOutput::warnings`]; the call itself only
/// fails on a fundamental problem with the body (non-utf8 uri list,
/// undecodable base64, malformed sip008 document).
#[uniffi::export]
pub fn decode_subscription(
    body: Vec<u8>,
    format: FfiSubscriptionFormat,
) -> Result<FfiDecodeOutput, GmvpnError> {
    let fmt: SubscriptionFormat = format.into();
    let out = subscription::decode(&body, fmt).map_err(GmvpnError::from)?;
    Ok(out.into())
}

/// Semantic version of `gmvpn-core`, surfaced in diagnostics views.
#[uniffi::export]
pub fn core_version() -> String {
    gmvpn_core::version().to_string()
}

uniffi::setup_scaffolding!();

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_vless_reality() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@host.example:443\
                   ?security=reality&sni=x.example&pbk=abc&sid=de"
            .to_string();
        let p = parse_profile_uri(uri).unwrap();
        assert_eq!(p.server, "host.example");
        assert_eq!(p.port, 443);
        assert!(matches!(p.protocol, FfiProtocol::Vless));
        assert!(matches!(p.security.mode, FfiSecurityMode::Reality));
        assert_eq!(p.security.reality.as_ref().unwrap().public_key, "abc");
    }

    #[test]
    fn parses_trojan() {
        let p = parse_profile_uri("trojan://pw@t.example:443#Main".to_string()).unwrap();
        assert!(matches!(p.protocol, FfiProtocol::Trojan));
        assert_eq!(p.remark.as_deref(), Some("Main"));
        if let FfiAuth::Trojan { password } = &p.auth {
            assert_eq!(password, "pw");
        } else {
            panic!("expected trojan auth");
        }
    }

    #[test]
    fn rejects_unknown_scheme() {
        let err = parse_profile_uri("http://example.com".to_string()).unwrap_err();
        assert!(matches!(err, GmvpnError::UnsupportedProtocol { .. }));
    }

    #[test]
    fn decodes_uri_list_subscription() {
        let body = b"vless://11111111-1111-1111-1111-111111111111@a.example:443\n".to_vec();
        let out = decode_subscription(body, FfiSubscriptionFormat::UriList).unwrap();
        assert_eq!(out.profiles.len(), 1);
        assert_eq!(out.profiles[0].server, "a.example");
        assert!(out.warnings.is_empty());
    }

    #[test]
    fn surfaces_warnings_for_bad_lines() {
        let body =
            b"vless://11111111-1111-1111-1111-111111111111@a.example:443\nnot-a-uri\n".to_vec();
        let out = decode_subscription(body, FfiSubscriptionFormat::UriList).unwrap();
        assert_eq!(out.profiles.len(), 1);
        assert_eq!(out.warnings.len(), 1);
        assert_eq!(out.warnings[0].input, "not-a-uri");
    }

    #[test]
    fn core_version_is_non_empty() {
        assert!(!core_version().is_empty());
    }
}
