//! Conversions between `gmvpn-core` domain types and the FFI DTOs.

use gmvpn_core::subscription::{DecodeOutput, DecodeWarning};
use gmvpn_core::{
    Auth, Profile, Protocol, RealityConfig, Security, SecurityMode, SubscriptionFormat, Transport,
    TransportNetwork,
};

use crate::dto::{
    FfiAuth, FfiDecodeOutput, FfiDecodeWarning, FfiProfile, FfiProtocol, FfiRealityConfig,
    FfiSecurity, FfiSecurityMode, FfiSubscriptionFormat, FfiTransport, FfiTransportNetwork,
};

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
