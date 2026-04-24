//! Profile domain model. Mirrors `/schemas/profile.schema.json`.
//!
//! This is the single Rust-side representation of a server profile. All
//! clients receive and edit profiles through this shape; Xray-core config
//! is derived from it, not the other way around.

use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Protocol {
    Vless,
    Vmess,
    Trojan,
    Shadowsocks,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "lowercase")]
pub enum Auth {
    Vless {
        uuid: Uuid,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        flow: Option<String>,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        encryption: Option<String>,
    },
    Vmess {
        uuid: Uuid,
        #[serde(default)]
        alter_id: u16,
        #[serde(default, skip_serializing_if = "Option::is_none")]
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

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum TransportNetwork {
    Tcp,
    Ws,
    Grpc,
    Http2,
    Quic,
    Kcp,
    Httpupgrade,
    Splithttp,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub struct Transport {
    pub network: Option<TransportNetwork>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub service_name: Option<String>,
}

impl Transport {
    #[must_use]
    pub fn is_empty(&self) -> bool {
        self.network.is_none()
            && self.host.is_none()
            && self.path.is_none()
            && self.service_name.is_none()
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum SecurityMode {
    None,
    Tls,
    Reality,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RealityConfig {
    pub public_key: String,
    pub short_id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub spider_x: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Security {
    pub mode: SecurityMode,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub sni: Option<String>,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub alpn: Vec<String>,
    #[serde(default)]
    pub allow_insecure: bool,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub fingerprint: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub reality: Option<RealityConfig>,
}

impl Default for Security {
    fn default() -> Self {
        Self {
            mode: SecurityMode::None,
            sni: None,
            alpn: Vec::new(),
            allow_insecure: false,
            fingerprint: None,
            reality: None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Profile {
    pub id: Uuid,
    pub name: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub remark: Option<String>,
    pub protocol: Protocol,
    pub server: String,
    pub port: u16,
    pub auth: Auth,
    #[serde(default, skip_serializing_if = "Transport::is_empty")]
    pub transport: Transport,
    #[serde(default)]
    pub security: Security,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub tags: Vec<String>,
}
