//! FFI boundary for GMvpn2 clients.
//!
//! The real Kotlin/Swift bindings will be generated via UniFFI once mobile
//! work starts (see `docs/memory/pending-decisions.md` §3). Until then this
//! crate exposes a minimal JSON-in / JSON-out surface so that the shared
//! core is exercisable from integration tests without committing to a
//! binding tool prematurely.

use gmvpn_core::{uri, Profile};

/// Parse a single profile URI (currently `vless://`) into a JSON string
/// matching `schemas/profile.schema.json`.
///
/// Errors are returned as `Err(message)` so platform layers can surface
/// them verbatim while a typed error enum is pending.
pub fn parse_profile_uri(uri: &str) -> Result<String, String> {
    let profile: Profile = if uri.starts_with("vless://") {
        uri::parse_vless(uri).map_err(|e| e.to_string())?
    } else {
        return Err(format!("unsupported scheme in: {uri}"));
    };
    serde_json::to_string(&profile).map_err(|e| e.to_string())
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
    fn parse_profile_uri_roundtrips_reality() {
        let uri = "vless://11111111-1111-1111-1111-111111111111@host.example:443\
                   ?security=reality&sni=x.example&pbk=abc&sid=de";
        let json = parse_profile_uri(uri).unwrap();
        assert!(json.contains("\"reality\""));
        assert!(json.contains("\"host.example\""));
    }

    #[test]
    fn parse_profile_uri_rejects_unknown_scheme() {
        let err = parse_profile_uri("http://example.com").unwrap_err();
        assert!(err.contains("unsupported"));
    }
}
