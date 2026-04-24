//! Profile URI parsers, one module per scheme, plus a unified
//! [`parse`] dispatcher that picks the right one by scheme prefix.
//!
//! All parsers return a fully-populated [`Profile`] with a fresh
//! [`uuid::Uuid`] id. They never touch I/O.

use crate::error::{Error, Result};
use crate::profile::Profile;

pub mod shadowsocks;
pub mod trojan;
pub mod vless;
pub mod vmess;

/// Parse any supported profile URI by looking at its scheme.
pub fn parse(input: &str) -> Result<Profile> {
    let scheme = input.split("://").next().unwrap_or("");
    match scheme {
        "vless" => vless::parse(input),
        "vmess" => vmess::parse(input),
        "trojan" => trojan::parse(input),
        "ss" => shadowsocks::parse(input),
        other => Err(Error::UnsupportedProtocol(other.to_string())),
    }
}

/// Shared helpers. `pub(crate)` so the per-scheme modules can share them
/// without leaking them as public API.
pub(crate) fn opt_string(s: String) -> Option<String> {
    if s.is_empty() {
        None
    } else {
        Some(s)
    }
}

pub(crate) fn decode_fragment(fragment: Option<&str>) -> Option<String> {
    fragment
        .map(|f| {
            percent_encoding::percent_decode_str(f)
                .decode_utf8_lossy()
                .into_owned()
        })
        .filter(|s| !s.is_empty())
}
