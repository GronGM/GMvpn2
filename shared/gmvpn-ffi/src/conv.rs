//! Conversions between `gmvpn-core` domain types and the FFI DTOs.

use gmvpn_core::subscription::{DecodeOutput, DecodeWarning};
use gmvpn_core::{
    Auth, LogLevel, Profile, Protocol, RealityConfig, Security, SecurityMode, SubscriptionFormat,
    Transport, TransportNetwork, TunnelOptions,
};
use uuid::Uuid;

use crate::dto::{
    FfiAuth, FfiDecodeOutput, FfiDecodeWarning, FfiLogLevel, FfiProfile, FfiProtocol,
    FfiRealityConfig, FfiSecurity, FfiSecurityMode, FfiSubscriptionFormat, FfiTransport,
    FfiTransportNetwork, FfiTunnelOptions,
};
use crate::error::GmvpnError;

impl From<Protocol> for FfiProtocol {
    fn from(value: Protocol) -> Self {
        match value {
            Protocol::Vless => Self::Vless,
            Protocol::Vmess => Self::Vmess,
            Protocol::Trojan => Self::Trojan,
            Protocol::Shadowsocks => Self::Shadowsocks,
        }
    }
}

impl From<TransportNetwork> for FfiTransportNetwork {
    fn from(value: TransportNetwork) -> Self {
        match value {
            TransportNetwork::Tcp => Self::Tcp,
            TransportNetwork::Ws => Self::Ws,
            TransportNetwork::Grpc => Self::Grpc,
            TransportNetwork::Http2 => Self::Http2,
            TransportNetwork::Quic => Self::Quic,
            TransportNetwork::Kcp => Self::Kcp,
            TransportNetwork::Httpupgrade => Self::Httpupgrade,
            TransportNetwork::Splithttp => Self::Splithttp,
        }
    }
}

impl From<SecurityMode> for FfiSecurityMode {
    fn from(value: SecurityMode) -> Self {
        match value {
            SecurityMode::None => Self::None,
            SecurityMode::Tls => Self::Tls,
            SecurityMode::Reality => Self::Reality,
        }
    }
}

impl From<RealityConfig> for FfiRealityConfig {
    fn from(value: RealityConfig) -> Self {
        Self {
            public_key: value.public_key,
            short_id: value.short_id,
            spider_x: value.spider_x,
        }
    }
}

impl From<Transport> for FfiTransport {
    fn from(value: Transport) -> Self {
        Self {
            network: value.network.map(Into::into),
            host: value.host,
            path: value.path,
            service_name: value.service_name,
        }
    }
}

impl From<Security> for FfiSecurity {
    fn from(value: Security) -> Self {
        Self {
            mode: value.mode.into(),
            sni: value.sni,
            alpn: value.alpn,
            allow_insecure: value.allow_insecure,
            fingerprint: value.fingerprint,
            reality: value.reality.map(Into::into),
        }
    }
}

impl From<Auth> for FfiAuth {
    fn from(value: Auth) -> Self {
        match value {
            Auth::Vless {
                uuid,
                flow,
                encryption,
            } => Self::Vless {
                uuid: uuid.to_string(),
                flow,
                encryption,
            },
            Auth::Vmess {
                uuid,
                alter_id,
                security,
            } => Self::Vmess {
                uuid: uuid.to_string(),
                alter_id,
                security,
            },
            Auth::Trojan { password } => Self::Trojan { password },
            Auth::Shadowsocks { method, password } => Self::Shadowsocks { method, password },
        }
    }
}

impl From<Profile> for FfiProfile {
    fn from(value: Profile) -> Self {
        Self {
            id: value.id.to_string(),
            name: value.name,
            remark: value.remark,
            protocol: value.protocol.into(),
            server: value.server,
            port: value.port,
            auth: value.auth.into(),
            transport: value.transport.into(),
            security: value.security.into(),
            tags: value.tags,
        }
    }
}

impl From<FfiSubscriptionFormat> for SubscriptionFormat {
    fn from(value: FfiSubscriptionFormat) -> Self {
        match value {
            FfiSubscriptionFormat::UriList => Self::UriList,
            FfiSubscriptionFormat::Base64UriList => Self::Base64UriList,
            FfiSubscriptionFormat::Sip008 => Self::Sip008,
        }
    }
}

impl From<DecodeWarning> for FfiDecodeWarning {
    fn from(value: DecodeWarning) -> Self {
        Self {
            index: u32::try_from(value.index).unwrap_or(u32::MAX),
            input: value.input,
            reason: value.reason,
        }
    }
}

impl From<DecodeOutput> for FfiDecodeOutput {
    fn from(value: DecodeOutput) -> Self {
        Self {
            profiles: value.profiles.into_iter().map(Into::into).collect(),
            warnings: value.warnings.into_iter().map(Into::into).collect(),
        }
    }
}

// ---------- FFI → domain (used by build_xray_config) ----------

impl From<FfiLogLevel> for LogLevel {
    fn from(value: FfiLogLevel) -> Self {
        match value {
            FfiLogLevel::Debug => Self::Debug,
            FfiLogLevel::Info => Self::Info,
            FfiLogLevel::Warning => Self::Warning,
            FfiLogLevel::Error => Self::Error,
            FfiLogLevel::None => Self::None,
        }
    }
}

impl From<FfiTunnelOptions> for TunnelOptions {
    fn from(value: FfiTunnelOptions) -> Self {
        Self {
            socks_listen: value.socks_listen,
            socks_port: value.socks_port,
            log_level: value.log_level.into(),
            dns_servers: value.dns_servers,
            enable_sniffing: value.enable_sniffing,
        }
    }
}

impl From<FfiProtocol> for Protocol {
    fn from(value: FfiProtocol) -> Self {
        match value {
            FfiProtocol::Vless => Self::Vless,
            FfiProtocol::Vmess => Self::Vmess,
            FfiProtocol::Trojan => Self::Trojan,
            FfiProtocol::Shadowsocks => Self::Shadowsocks,
        }
    }
}

impl From<FfiTransportNetwork> for TransportNetwork {
    fn from(value: FfiTransportNetwork) -> Self {
        match value {
            FfiTransportNetwork::Tcp => Self::Tcp,
            FfiTransportNetwork::Ws => Self::Ws,
            FfiTransportNetwork::Grpc => Self::Grpc,
            FfiTransportNetwork::Http2 => Self::Http2,
            FfiTransportNetwork::Quic => Self::Quic,
            FfiTransportNetwork::Kcp => Self::Kcp,
            FfiTransportNetwork::Httpupgrade => Self::Httpupgrade,
            FfiTransportNetwork::Splithttp => Self::Splithttp,
        }
    }
}

impl From<FfiSecurityMode> for SecurityMode {
    fn from(value: FfiSecurityMode) -> Self {
        match value {
            FfiSecurityMode::None => Self::None,
            FfiSecurityMode::Tls => Self::Tls,
            FfiSecurityMode::Reality => Self::Reality,
        }
    }
}

impl From<FfiRealityConfig> for RealityConfig {
    fn from(value: FfiRealityConfig) -> Self {
        Self {
            public_key: value.public_key,
            short_id: value.short_id,
            spider_x: value.spider_x,
        }
    }
}

impl From<FfiTransport> for Transport {
    fn from(value: FfiTransport) -> Self {
        Self {
            network: value.network.map(Into::into),
            host: value.host,
            path: value.path,
            service_name: value.service_name,
        }
    }
}

impl From<FfiSecurity> for Security {
    fn from(value: FfiSecurity) -> Self {
        Self {
            mode: value.mode.into(),
            sni: value.sni,
            alpn: value.alpn,
            allow_insecure: value.allow_insecure,
            fingerprint: value.fingerprint,
            reality: value.reality.map(Into::into),
        }
    }
}

impl TryFrom<FfiAuth> for Auth {
    type Error = GmvpnError;

    fn try_from(value: FfiAuth) -> Result<Self, Self::Error> {
        Ok(match value {
            FfiAuth::Vless {
                uuid,
                flow,
                encryption,
            } => Self::Vless {
                uuid: parse_uuid(&uuid, "auth.uuid")?,
                flow,
                encryption,
            },
            FfiAuth::Vmess {
                uuid,
                alter_id,
                security,
            } => Self::Vmess {
                uuid: parse_uuid(&uuid, "auth.uuid")?,
                alter_id,
                security,
            },
            FfiAuth::Trojan { password } => Self::Trojan { password },
            FfiAuth::Shadowsocks { method, password } => Self::Shadowsocks { method, password },
        })
    }
}

impl TryFrom<FfiProfile> for Profile {
    type Error = GmvpnError;

    fn try_from(value: FfiProfile) -> Result<Self, Self::Error> {
        Ok(Self {
            id: parse_uuid(&value.id, "id")?,
            name: value.name,
            remark: value.remark,
            protocol: value.protocol.into(),
            server: value.server,
            port: value.port,
            auth: value.auth.try_into()?,
            transport: value.transport.into(),
            security: value.security.into(),
            tags: value.tags,
        })
    }
}

fn parse_uuid(s: &str, field: &'static str) -> Result<Uuid, GmvpnError> {
    Uuid::parse_str(s).map_err(|e| GmvpnError::InvalidValue {
        field: field.into(),
        reason: e.to_string(),
    })
}
