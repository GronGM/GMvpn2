//! FFI data-transfer types. One-to-one with the domain model in
//! `gmvpn-core`, but with representations UniFFI understands directly
//! (e.g. `String` for UUIDs, plain C-like enums for transport and
//! security modes).

/// Wire protocol a profile speaks.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum FfiProtocol {
    Vless,
    Vmess,
    Trojan,
    Shadowsocks,
}

/// Transport network under the chosen protocol.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum FfiTransportNetwork {
    Tcp,
    Ws,
    Grpc,
    Http2,
    Quic,
    Kcp,
    Httpupgrade,
    Splithttp,
}

/// TLS mode applied to the transport.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum FfiSecurityMode {
    None,
    Tls,
    Reality,
}

/// Reality-specific fields. Only present when `FfiSecurity.mode == Reality`.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiRealityConfig {
    pub public_key: String,
    pub short_id: String,
    pub spider_x: Option<String>,
}

/// Transport-level parameters (ws path, grpc service name, …).
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiTransport {
    pub network: Option<FfiTransportNetwork>,
    pub host: Option<String>,
    pub path: Option<String>,
    pub service_name: Option<String>,
}

/// TLS / Reality parameters.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiSecurity {
    pub mode: FfiSecurityMode,
    pub sni: Option<String>,
    pub alpn: Vec<String>,
    pub allow_insecure: bool,
    pub fingerprint: Option<String>,
    pub reality: Option<FfiRealityConfig>,
}

/// Authentication material, tagged by protocol. UUIDs are passed as
/// lowercase 36-char strings.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum FfiAuth {
    Vless {
        uuid: String,
        flow: Option<String>,
        encryption: Option<String>,
    },
    Vmess {
        uuid: String,
        alter_id: u16,
        security: Option<String>,
    },
    Trojan {
        password: String,
    },
    Shadowsocks {
        method: String,
        password: String,
    },
}

/// One server profile. Matches `schemas/profile.schema.json` (the JSON
/// shape is still available for storage — this is the live in-memory
/// type clients work with).
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiProfile {
    /// Stable client-side UUID (v4).
    pub id: String,
    pub name: String,
    pub remark: Option<String>,
    pub protocol: FfiProtocol,
    pub server: String,
    pub port: u16,
    pub auth: FfiAuth,
    pub transport: FfiTransport,
    pub security: FfiSecurity,
    pub tags: Vec<String>,
}

/// Wire format of a subscription response.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiSubscriptionFormat {
    /// Plain text, one URI per line (`#` introduces a comment).
    UriList,
    /// Base64 of a uri-list body.
    Base64UriList,
    /// SIP008 JSON (Shadowsocks).
    Sip008,
}

/// Per-entry failure recorded while decoding a subscription.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiDecodeWarning {
    /// Zero-based line or element index.
    pub index: u32,
    /// Offending input, trimmed.
    pub input: String,
    /// Human-readable reason.
    pub reason: String,
}

/// Output of a successful `decode_subscription` call.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiDecodeOutput {
    pub profiles: Vec<FfiProfile>,
    pub warnings: Vec<FfiDecodeWarning>,
}
