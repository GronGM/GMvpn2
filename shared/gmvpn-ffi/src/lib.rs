//! FFI boundary for GMvpn2 clients.
//!
//! The real Kotlin/Swift bindings will be generated via UniFFI once mobile
//! work starts (see `docs/memory/pending-decisions.md` §3). Until then this
//! crate exposes a minimal JSON-in / JSON-out surface so that the shared
//! core is exercisable from integration tests without committing to a
//! binding tool prematurely.

use gmvpn_core::{subscription, uri, SubscriptionFormat};

/// Parse any supported profile URI (vless / vmess / trojan / ss) into a
/// JSON string matching `schemas/profile.schema.json`.
///
/// Errors are returned as `Err(message)` so platform layers can surface
/// them verbatim while a typed error enum is pending.
pub fn parse_profile_uri(uri: &str) -> Result<String, String> {
    let profile = uri::parse(uri).map_err(|e| e.to_string())?;
    serde_json::to_string(&profile).map_err(|e| e.to_string())
}

/// Decode a subscription body into a JSON object with two arrays:
/// `profiles` (matching `schemas/profile.schema.json`) and `warnings`
/// (line-level failures). `format` must be one of:
/// `uri-list`, `base64-uri-list`, `sip008`.
pub fn decode_subscription(body: &[u8], format: &str) -> Result<String, String> {
    let fmt = match format {
        "uri-list" => SubscriptionFormat::UriList,
        "base64-uri-list" => SubscriptionFormat::Base64UriList,
        "sip008" => SubscriptionFormat::Sip008,
        other => return Err(format!("unsupported subscription format: {other}")),
    };

    let out = subscription::decode(body, fmt).map_err(|e| e.to_string())?;

    let json = serde_json::json!({
        "profiles": out.profiles,
        "warnings": out
            .warnings
            .into_iter()
            .map(|w| serde_json::json!({
                "index": w.index,
                "input": w.input,
                "reason": w.reason,
            }))
            .collect::<Vec<_>>(),
    });

    serde_json::to_string(&json).map_err(|e| e.to_string())
}

/// Core version, proxied for diagnostics screens.
#[must_use]
pub fn core_version() -> &'static str {
    gmvpn_core::version()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_vless_reality() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@host.example:443\
                   ?security=reality&sni=x.example&pbk=abc&sid=de";
        let json = parse_profile_uri(uri).unwrap();
        assert!(json.contains("\"reality\""));
        assert!(json.contains("\"host.example\""));
    }

    #[test]
    fn parses_trojan() {
        let json = parse_profile_uri("trojan://pw@t.example:443#Main").unwrap();
        assert!(json.contains("\"trojan\""));
        assert!(json.contains("\"Main\""));
    }

    #[test]
    fn rejects_unknown_scheme() {
        let err = parse_profile_uri("http://example.com").unwrap_err();
        assert!(err.contains("unsupported") || err.contains("http"));
    }

    #[test]
    fn decodes_uri_list_subscription() {
        let body = b"vless://11111111-1111-1111-1111-111111111111@a.example:443\n";
        let json = decode_subscription(body, "uri-list").unwrap();
        assert!(json.contains("\"profiles\""));
        assert!(json.contains("a.example"));
    }

    #[test]
    fn rejects_bad_format() {
        let err = decode_subscription(b"", "clash").unwrap_err();
        assert!(err.contains("unsupported"));
    }
}
