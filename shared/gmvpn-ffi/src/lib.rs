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
    FfiAuth, FfiDecodeOutput, FfiDecodeWarning, FfiLogLevel, FfiProfile, FfiProtocol,
    FfiRealityConfig, FfiSecurity, FfiSecurityMode, FfiSubscriptionFormat, FfiTransport,
    FfiTransportNetwork, FfiTunnelOptions, FfiUriDecodeOutput,
};
pub use error::GmvpnError;

use gmvpn_core::{subscription, uri, xray, SubscriptionFormat};

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

/// String-only counterpart of [`decode_subscription`]. Returns
/// reusable URI strings rather than fully-parsed profiles, so a
/// platform-side library can persist subscription contents as
/// strings and re-parse on demand.
#[uniffi::export]
pub fn decode_subscription_uris(
    body: Vec<u8>,
    format: FfiSubscriptionFormat,
) -> Result<FfiUriDecodeOutput, GmvpnError> {
    let fmt: SubscriptionFormat = format.into();
    let out = subscription::decode_uris(&body, fmt).map_err(GmvpnError::from)?;
    Ok(out.into())
}

/// Build the Xray-core JSON configuration for a profile and tunnel
/// options. Pass the returned string straight to the Go wrapper's
/// `Tunnel.Start(configJSON, tunFD)`.
#[uniffi::export]
pub fn build_xray_config(
    profile: FfiProfile,
    options: FfiTunnelOptions,
) -> Result<String, GmvpnError> {
    let domain_profile: gmvpn_core::Profile = profile.try_into()?;
    let opts: gmvpn_core::TunnelOptions = options.into();
    xray::build_config(&domain_profile, &opts).map_err(GmvpnError::from)
}

/// Default tunnel options. Exposed so callers don't have to know our
/// defaults — only override what they need.
#[uniffi::export]
pub fn default_tunnel_options() -> FfiTunnelOptions {
    FfiTunnelOptions::defaults()
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
    fn decodes_subscription_uris_round_trip() {
        let body = b"vless://11111111-1111-1111-1111-111111111111@a.example:443\nbroken\n".to_vec();
        let out = decode_subscription_uris(body, FfiSubscriptionFormat::UriList).unwrap();
        assert_eq!(out.uris.len(), 1);
        assert!(out.uris[0].starts_with("vless://"));
        assert_eq!(out.warnings.len(), 1);
    }

    #[test]
    fn core_version_is_non_empty() {
        assert!(!core_version().is_empty());
    }

    #[test]
    fn builds_xray_config_for_parsed_profile() {
        let p = parse_profile_uri(
            "vless://11111111-1111-1111-1111-111111111111@host.example:443\
             ?security=reality&sni=www.cf&pbk=PBK&sid=SID&fp=chrome&flow=xtls-rprx-vision"
                .to_string(),
        )
        .unwrap();
        let opts = default_tunnel_options();
        let json = build_xray_config(p, opts).unwrap();

        let v: serde_json::Value = serde_json::from_str(&json).unwrap();
        assert_eq!(v["outbounds"][0]["protocol"], "vless");
        assert_eq!(v["outbounds"][0]["streamSettings"]["security"], "reality");
        assert_eq!(v["inbounds"][0]["protocol"], "socks");
    }

    #[test]
    fn build_xray_config_rejects_bad_uuid_in_dto() {
        let mut p = parse_profile_uri(
            "vless://11111111-1111-1111-1111-111111111111@h.example:443".to_string(),
        )
        .unwrap();
        p.id = "not-a-uuid".into();
        let err = build_xray_config(p, default_tunnel_options()).unwrap_err();
        assert!(matches!(err, GmvpnError::InvalidValue { .. }));
    }
}
