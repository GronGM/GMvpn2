//! Build Xray-core JSON configuration from a [`Profile`].
//!
//! The output of [`build_config`] is exactly the JSON that
//! `core.LoadConfig("json", …)` accepts in `/core/gmvpn`. The shape
//! follows Xray's documented schema; we keep it minimal — one inbound
//! (SOCKS5 on loopback for the tun2socks bridge) and one outbound
//! (the user's profile) plus `direct` and `block` outbounds for the
//! routing module to reach.
//!
//! This module never touches the network or the filesystem.

use serde_json::{json, Map, Value};

use crate::error::Result;
use crate::profile::{
    Auth, Profile, Protocol, Security, SecurityMode, Transport, TransportNetwork,
};

/// Knobs the platform layer wants reflected in the generated config.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TunnelOptions {
    /// Local interface the SOCKS inbound binds to. The tun2socks bridge
    /// runs in the same process / app sandbox, so loopback is the right
    /// default.
    pub socks_listen: String,
    /// Local port for the SOCKS inbound. Pick at runtime to avoid
    /// collisions on shared devices.
    pub socks_port: u16,
    /// `loglevel` value Xray-core honors. `None` → "warning".
    pub log_level: LogLevel,
    /// DNS servers Xray uses while the tunnel is up. Empty → Xray
    /// default (`localhost`), which on Android resolves through the
    /// VPN — usually undesirable. Platform layers should supply at
    /// least one server.
    pub dns_servers: Vec<String>,
    /// Enable destination-override sniffing on the SOCKS inbound. This
    /// is the standard way to make routing rules see real domains
    /// instead of just IPs.
    pub enable_sniffing: bool,
}

impl Default for TunnelOptions {
    fn default() -> Self {
        Self {
            socks_listen: "127.0.0.1".into(),
            socks_port: 10_808,
            log_level: LogLevel::Warning,
            dns_servers: Vec::new(),
            enable_sniffing: true,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LogLevel {
    Debug,
    Info,
    Warning,
    Error,
    None,
}

impl LogLevel {
    fn as_str(self) -> &'static str {
        match self {
            Self::Debug => "debug",
            Self::Info => "info",
            Self::Warning => "warning",
            Self::Error => "error",
            Self::None => "none",
        }
    }
}

/// Build the Xray-core config JSON for the given profile and options.
pub fn build_config(profile: &Profile, opts: &TunnelOptions) -> Result<String> {
    let value = build_config_value(profile, opts);
    serde_json::to_string(&value).map_err(Into::into)
}

/// Pretty-printed variant — useful in tests and diagnostics views.
pub fn build_config_pretty(profile: &Profile, opts: &TunnelOptions) -> Result<String> {
    let value = build_config_value(profile, opts);
    serde_json::to_string_pretty(&value).map_err(Into::into)
}

fn build_config_value(profile: &Profile, opts: &TunnelOptions) -> Value {
    let mut root = Map::new();
    root.insert("log".into(), json!({ "loglevel": opts.log_level.as_str() }));

    if !opts.dns_servers.is_empty() {
        root.insert("dns".into(), json!({ "servers": opts.dns_servers }));
    }

    root.insert("inbounds".into(), json!([build_socks_inbound(opts)]));

    let outbounds = vec![
        build_proxy_outbound(profile),
        json!({ "tag": "direct", "protocol": "freedom", "settings": {} }),
        json!({ "tag": "block",  "protocol": "blackhole", "settings": {} }),
    ];
    root.insert("outbounds".into(), Value::Array(outbounds));

    root.insert(
        "routing".into(),
        json!({
            "domainStrategy": "IPIfNonMatch",
            "rules": [
                {
                    "type": "field",
                    "outboundTag": "block",
                    "ip": ["geoip:private"]
                }
            ]
        }),
    );

    Value::Object(root)
}

fn build_socks_inbound(opts: &TunnelOptions) -> Value {
    let mut inbound = json!({
        "tag": "socks-in",
        "listen": opts.socks_listen,
        "port": opts.socks_port,
        "protocol": "socks",
        "settings": {
            "udp": true,
            "auth": "noauth"
        }
    });
    if opts.enable_sniffing {
        inbound["sniffing"] = json!({
            "enabled": true,
            "destOverride": ["http", "tls", "quic"]
        });
    }
    inbound
}

fn build_proxy_outbound(profile: &Profile) -> Value {
    let mut outbound = json!({
        "tag": "proxy",
        "protocol": protocol_name(profile.protocol),
        "settings": settings_for(profile),
    });
    if let Some(stream) = stream_settings_for(&profile.transport, &profile.security) {
        outbound["streamSettings"] = stream;
    }
    outbound
}

fn protocol_name(p: Protocol) -> &'static str {
    match p {
        Protocol::Vless => "vless",
        Protocol::Vmess => "vmess",
        Protocol::Trojan => "trojan",
        Protocol::Shadowsocks => "shadowsocks",
    }
}

fn settings_for(profile: &Profile) -> Value {
    match (&profile.protocol, &profile.auth) {
        (
            Protocol::Vless,
            Auth::Vless {
                uuid,
                flow,
                encryption,
            },
        ) => {
            let mut user = json!({
                "id": uuid.to_string(),
                "encryption": encryption.clone().unwrap_or_else(|| "none".into()),
            });
            if let Some(flow) = flow {
                user["flow"] = json!(flow);
            }
            json!({
                "vnext": [{
                    "address": profile.server,
                    "port": profile.port,
                    "users": [user],
                }]
            })
        }
        (
            Protocol::Vmess,
            Auth::Vmess {
                uuid,
                alter_id,
                security,
            },
        ) => {
            let mut user = json!({
                "id": uuid.to_string(),
                "alterId": alter_id,
                "security": security.clone().unwrap_or_else(|| "auto".into()),
            });
            // Drop alterId if AEAD; Xray accepts both, but cleaner.
            if *alter_id == 0 {
                if let Value::Object(map) = &mut user {
                    map.remove("alterId");
                }
            }
            json!({
                "vnext": [{
                    "address": profile.server,
                    "port": profile.port,
                    "users": [user],
                }]
            })
        }
        (Protocol::Trojan, Auth::Trojan { password }) => json!({
            "servers": [{
                "address": profile.server,
                "port": profile.port,
                "password": password,
            }]
        }),
        (Protocol::Shadowsocks, Auth::Shadowsocks { method, password }) => json!({
            "servers": [{
                "address": profile.server,
                "port": profile.port,
                "method": method,
                "password": password,
            }]
        }),
        // Mismatched protocol/auth pair would have been rejected during
        // parsing; emit an empty settings block as a defensive fallback.
        _ => json!({}),
    }
}

fn stream_settings_for(transport: &Transport, security: &Security) -> Option<Value> {
    if transport.is_empty() && matches!(security.mode, SecurityMode::None) {
        return None;
    }

    let mut stream = Map::new();

    let network = transport.network.map_or("tcp", network_name);
    stream.insert("network".into(), json!(network));

    match transport.network {
        Some(TransportNetwork::Ws | TransportNetwork::Httpupgrade) => {
            let mut ws = json!({
                "path": transport.path.clone().unwrap_or_else(|| "/".into()),
            });
            if let Some(host) = &transport.host {
                ws["headers"] = json!({ "Host": host });
            }
            let key = if matches!(transport.network, Some(TransportNetwork::Httpupgrade)) {
                "httpupgradeSettings"
            } else {
                "wsSettings"
            };
            stream.insert(key.into(), ws);
        }
        Some(TransportNetwork::Grpc) => {
            stream.insert(
                "grpcSettings".into(),
                json!({
                    "serviceName": transport
                        .service_name
                        .clone()
                        .unwrap_or_default(),
                    "multiMode": false,
                }),
            );
        }
        Some(TransportNetwork::Http2) => {
            let mut h2 = json!({
                "path": transport.path.clone().unwrap_or_else(|| "/".into()),
            });
            if let Some(host) = &transport.host {
                h2["host"] = json!([host]);
            }
            stream.insert("httpSettings".into(), h2);
        }
        _ => {}
    }

    stream.insert("security".into(), json!(security_name(security.mode)));
    match security.mode {
        SecurityMode::Tls => {
            stream.insert("tlsSettings".into(), tls_settings(security));
        }
        SecurityMode::Reality => {
            if let Some(reality) = &security.reality {
                stream.insert(
                    "realitySettings".into(),
                    reality_settings(security, reality),
                );
            }
        }
        SecurityMode::None => {}
    }

    Some(Value::Object(stream))
}

fn network_name(n: TransportNetwork) -> &'static str {
    match n {
        TransportNetwork::Tcp => "tcp",
        TransportNetwork::Ws => "ws",
        TransportNetwork::Grpc => "grpc",
        TransportNetwork::Http2 => "http",
        TransportNetwork::Quic => "quic",
        TransportNetwork::Kcp => "kcp",
        TransportNetwork::Httpupgrade => "httpupgrade",
        TransportNetwork::Splithttp => "splithttp",
    }
}

fn security_name(m: SecurityMode) -> &'static str {
    match m {
        SecurityMode::None => "none",
        SecurityMode::Tls => "tls",
        SecurityMode::Reality => "reality",
    }
}

fn tls_settings(security: &Security) -> Value {
    let mut tls = json!({
        "allowInsecure": security.allow_insecure,
    });
    if let Some(sni) = &security.sni {
        tls["serverName"] = json!(sni);
    }
    if !security.alpn.is_empty() {
        tls["alpn"] = json!(security.alpn);
    }
    if let Some(fp) = &security.fingerprint {
        tls["fingerprint"] = json!(fp);
    }
    tls
}

fn reality_settings(security: &Security, reality: &crate::profile::RealityConfig) -> Value {
    let mut s = json!({
        "publicKey": reality.public_key,
        "shortId": reality.short_id,
        "fingerprint": security
            .fingerprint
            .clone()
            .unwrap_or_else(|| "chrome".into()),
    });
    if let Some(sni) = &security.sni {
        s["serverName"] = json!(sni);
    }
    if let Some(spx) = &reality.spider_x {
        s["spiderX"] = json!(spx);
    }
    s
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::uri;

    fn parse(uri: &str) -> Profile {
        uri::parse(uri).unwrap()
    }

    fn build(profile: &Profile) -> Value {
        let opts = TunnelOptions::default();
        let s = build_config(profile, &opts).unwrap();
        serde_json::from_str(&s).unwrap()
    }

    #[test]
    fn vless_reality_emits_realitysettings() {
        let p = parse(
            "vless://11111111-1111-1111-1111-111111111111@host.example:443\
             ?type=tcp&security=reality&sni=www.cloudflare.com\
             &flow=xtls-rprx-vision&pbk=PBK&sid=SID&fp=chrome",
        );
        let v = build(&p);

        let outbound = &v["outbounds"][0];
        assert_eq!(outbound["protocol"], "vless");
        let user = &outbound["settings"]["vnext"][0]["users"][0];
        assert_eq!(user["flow"], "xtls-rprx-vision");
        assert_eq!(user["encryption"], "none");

        let stream = &outbound["streamSettings"];
        assert_eq!(stream["network"], "tcp");
        assert_eq!(stream["security"], "reality");
        assert_eq!(stream["realitySettings"]["publicKey"], "PBK");
        assert_eq!(stream["realitySettings"]["shortId"], "SID");
        assert_eq!(
            stream["realitySettings"]["serverName"],
            "www.cloudflare.com"
        );
        assert_eq!(stream["realitySettings"]["fingerprint"], "chrome");
    }

    #[test]
    fn vmess_ws_tls_emits_wssettings_and_tlssettings() {
        let json = serde_json::json!({
            "v":"2","ps":"X","add":"vm.example","port":"443",
            "id":"22222222-2222-2222-2222-222222222222","aid":"0",
            "net":"ws","tls":"tls","sni":"vm.example","path":"/chat","host":"cdn.example"
        })
        .to_string();
        let encoded =
            base64::Engine::encode(&base64::engine::general_purpose::STANDARD, json.as_bytes());
        let p = parse(&format!("vmess://{encoded}"));
        let v = build(&p);

        let outbound = &v["outbounds"][0];
        assert_eq!(outbound["protocol"], "vmess");
        let stream = &outbound["streamSettings"];
        assert_eq!(stream["network"], "ws");
        assert_eq!(stream["security"], "tls");
        assert_eq!(stream["wsSettings"]["path"], "/chat");
        assert_eq!(stream["wsSettings"]["headers"]["Host"], "cdn.example");
        assert_eq!(stream["tlsSettings"]["serverName"], "vm.example");
        assert_eq!(stream["tlsSettings"]["allowInsecure"], false);
    }

    #[test]
    fn trojan_default_security_is_tls() {
        let p = parse("trojan://hunter2@t.example:443#main");
        let v = build(&p);
        let outbound = &v["outbounds"][0];
        assert_eq!(outbound["protocol"], "trojan");
        assert_eq!(outbound["settings"]["servers"][0]["password"], "hunter2");
        assert_eq!(outbound["streamSettings"]["security"], "tls");
    }

    #[test]
    fn shadowsocks_has_no_streamsettings() {
        let userinfo = base64::Engine::encode(
            &base64::engine::general_purpose::URL_SAFE_NO_PAD,
            b"aes-256-gcm:pw",
        );
        let p = parse(&format!("ss://{userinfo}@ss.example:8388"));
        let v = build(&p);
        let outbound = &v["outbounds"][0];
        assert_eq!(outbound["protocol"], "shadowsocks");
        assert!(outbound.get("streamSettings").is_none());
        assert_eq!(outbound["settings"]["servers"][0]["method"], "aes-256-gcm");
    }

    #[test]
    fn includes_socks_inbound_with_options() {
        let opts = TunnelOptions {
            socks_listen: "127.0.0.1".into(),
            socks_port: 12_345,
            ..TunnelOptions::default()
        };
        let p = parse("trojan://pw@t.example:443");
        let s = build_config(&p, &opts).unwrap();
        let v: Value = serde_json::from_str(&s).unwrap();
        let inbound = &v["inbounds"][0];
        assert_eq!(inbound["protocol"], "socks");
        assert_eq!(inbound["port"], 12_345);
        assert_eq!(inbound["listen"], "127.0.0.1");
        assert_eq!(inbound["settings"]["udp"], true);
        assert_eq!(inbound["sniffing"]["enabled"], true);
    }

    #[test]
    fn dns_servers_omitted_when_empty() {
        let p = parse("trojan://pw@t.example:443");
        let v = build(&p);
        assert!(v.get("dns").is_none());
    }

    #[test]
    fn dns_servers_emitted_when_configured() {
        let opts = TunnelOptions {
            dns_servers: vec!["1.1.1.1".into(), "8.8.8.8".into()],
            ..TunnelOptions::default()
        };
        let p = parse("trojan://pw@t.example:443");
        let s = build_config(&p, &opts).unwrap();
        let v: Value = serde_json::from_str(&s).unwrap();
        assert_eq!(v["dns"]["servers"][0], "1.1.1.1");
        assert_eq!(v["dns"]["servers"][1], "8.8.8.8");
    }

    #[test]
    fn routing_blocks_private_ip_by_default() {
        let p = parse("trojan://pw@t.example:443");
        let v = build(&p);
        let rule = &v["routing"]["rules"][0];
        assert_eq!(rule["outboundTag"], "block");
        assert_eq!(rule["ip"][0], "geoip:private");
    }

    #[test]
    fn includes_freedom_and_blackhole_outbounds() {
        let p = parse("trojan://pw@t.example:443");
        let v = build(&p);
        let tags: Vec<&str> = v["outbounds"]
            .as_array()
            .unwrap()
            .iter()
            .map(|o| o["tag"].as_str().unwrap())
            .collect();
        assert_eq!(tags, vec!["proxy", "direct", "block"]);
    }
}
