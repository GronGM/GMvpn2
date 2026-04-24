//! Shared domain layer for GMvpn2. Pure logic: no OS calls, no filesystem,
//! no network. Platform layers wrap this behind FFI.

pub mod error;
pub mod profile;
pub mod uri;

pub use error::{Error, Result};
pub use profile::{
    Auth, Profile, Protocol, RealityConfig, Security, SecurityMode, Transport, TransportNetwork,
};

/// Semantic version of the domain crate, surfaced to clients for diagnostics.
#[must_use]
pub fn version() -> &'static str {
    env!("CARGO_PKG_VERSION")
}
