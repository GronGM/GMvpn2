//! Shared domain layer for GMvpn2. Pure logic: no OS calls, no filesystem,
//! no network. Platform layers wrap this behind FFI.

pub mod error;
pub mod profile;
pub mod subscription;
pub mod uri;
pub mod xray;

pub use error::{Error, Result};
pub use profile::{
    Auth, Profile, Protocol, RealityConfig, Security, SecurityMode, Transport, TransportNetwork,
};
pub use subscription::{DecodeOutput, DecodeWarning, SubscriptionFormat, UriDecodeOutput};
pub use xray::{LogLevel, TunnelOptions};

/// Semantic version of the domain crate, surfaced to clients for diagnostics.
#[must_use]
pub fn version() -> &'static str {
    env!("CARGO_PKG_VERSION")
}
