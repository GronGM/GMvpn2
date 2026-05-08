//! Dump deterministic Xray-core JSON configs for cross-language
//! contract testing. Run from the repo root with:
//!
//! ```
//! cargo run -p gmvpn-core --example dump_configs -- \
//!     ../../core/gmvpn/testdata/configs
//! ```
//!
//! The Go contract test in `core/gmvpn` then loads each file through
//! `core.LoadConfig("json", …)` and asserts Xray-core accepts the
//! shape `gmvpn_core::xray::build_config` produces.
//!
//! The example uses fixed UUIDs and short server addresses so the
//! committed fixtures stay byte-stable across regenerations — only a
//! real surface change moves them.

use std::fs;
use std::path::PathBuf;

use gmvpn_core::profile::{
    Auth, Profile, Protocol, RealityConfig, Security, SecurityMode, Transport, TransportNetwork,
};
use gmvpn_core::xray;
use uuid::Uuid;

const FIXED_PROFILE_UUID: Uuid = Uuid::from_u128(0x0000_0000_0000_0000_0000_0000_0000_0001);
const FIXED_AUTH_UUID: Uuid = Uuid::from_u128(0x1111_1111_1111_1111_1111_1111_1111_1111);

fn main() {
    let out_dir: PathBuf = std::env::args()
        .nth(1)
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("core/gmvpn/testdata/configs"));
    fs::create_dir_all(&out_dir).expect("create out dir");

    let opts = gmvpn_core::xray::TunnelOptions {
        socks_listen: "127.0.0.1".into(),
        socks_port: 10808,
        log_level: gmvpn_core::xray::LogLevel::Warning,
        dns_servers: vec!["1.1.1.1".into(), "8.8.8.8".into()],
        enable_sniffing: true,
    };

    for (name, profile) in [
        ("vless_reality", vless_reality()),
        ("vmess_ws_tls", vmess_ws_tls()),
        ("trojan_grpc", trojan_grpc()),
        ("shadowsocks", shadowsocks()),
    ] {
        let json = xray::build_config_pretty(&profile, &opts).expect("build_config");
        let path = out_dir.join(format!("{name}.json"));
        fs::write(&path, json).expect("write fixture");
        println!("wrote {}", path.display());
    }
}

fn base(name: &str, protocol: Protocol, server: &str, port: u16, auth: Auth) -> Profile {
    Profile {
        id: FIXED_PROFILE_UUID,
        name: name.into(),
        remark: Some(name.into()),
        protocol,
        server: server.into(),
        port,
        auth,
        transport: Transport::default(),
        security: Security::default(),
        tags: Vec::new(),
    }
}

fn vless_reality() -> Profile {
    let mut p = base(
        "vless-reality",
        Protocol::Vless,
        "host.example",
        443,
        Auth::Vless {
            uuid: FIXED_AUTH_UUID,
            flow: Some("xtls-rprx-vision".into()),
            encryption: None,
        },
    );
    p.transport = Transport {
        network: Some(TransportNetwork::Tcp),
        ..Transport::default()
    };
    p.security = Security {
        mode: SecurityMode::Reality,
        sni: Some("www.cloudflare.com".into()),
        alpn: Vec::new(),
        allow_insecure: false,
        fingerprint: Some("chrome".into()),
        reality: Some(RealityConfig {
            public_key: "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA".into(),
            short_id: "deadbeef".into(),
            spider_x: None,
        }),
    };
    p
}

fn vmess_ws_tls() -> Profile {
    let mut p = base(
        "vmess-ws-tls",
        Protocol::Vmess,
        "vmess.example",
        443,
        Auth::Vmess {
            uuid: FIXED_AUTH_UUID,
            alter_id: 0,
            security: Some("auto".into()),
        },
    );
    p.transport = Transport {
        network: Some(TransportNetwork::Ws),
        host: Some("cdn.example".into()),
        path: Some("/chat".into()),
        service_name: None,
    };
    p.security = Security {
        mode: SecurityMode::Tls,
        sni: Some("vmess.example".into()),
        alpn: vec!["h2".into(), "http/1.1".into()],
        allow_insecure: false,
        fingerprint: Some("chrome".into()),
        reality: None,
    };
    p
}

fn trojan_grpc() -> Profile {
    let mut p = base(
        "trojan-grpc",
        Protocol::Trojan,
        "trojan.example",
        443,
        Auth::Trojan {
            password: "password-fixed".into(),
        },
    );
    p.transport = Transport {
        network: Some(TransportNetwork::Grpc),
        host: None,
        path: None,
        service_name: Some("gun".into()),
    };
    p.security = Security {
        mode: SecurityMode::Tls,
        sni: Some("trojan.example".into()),
        alpn: vec!["h2".into()],
        allow_insecure: false,
        fingerprint: Some("chrome".into()),
        reality: None,
    };
    p
}

fn shadowsocks() -> Profile {
    base(
        "shadowsocks",
        Protocol::Shadowsocks,
        "ss.example",
        8388,
        Auth::Shadowsocks {
            method: "aes-256-gcm".into(),
            password: "password-fixed".into(),
        },
    )
}
