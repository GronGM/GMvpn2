# Project context

## Goal

Deliver a modern, cross-platform VPN client built on Xray-core that is
clearly above the commodity "list of servers + connect button" tier: thoughtful
architecture, real native integration, safe defaults, good UX.

## Target protocols (first wave)

- VLESS + Reality (default recommendation for new profiles)
- VMess
- Trojan
- Shadowsocks (with plugins later)
- Subscription import (URI list, base64, SIP008 for SS)

## Stack decisions (stable)

- **Engine:** Xray-core, consumed as a library / bundled binary. No fork.
- **Shared domain layer:** Rust (Variant A). Single source of truth for:
  - profile / subscription / routing models
  - URI parsers (vless://, vmess://, trojan://, ss://)
  - subscription fetch + decode
  - routing rule evaluation
  - logging + metrics shaping
- **FFI:** UniFFI for Kotlin and Swift; C-ABI header for Windows and Linux.
- **UI:** native per platform. Shared UI framework is explicitly rejected for
  v1 because tunnel integration (VpnService, NEPacketTunnelProvider, WinTUN)
  is deeply platform-specific.

## Architectural principles

1. Strict split: domain (Rust) / platform bridge (FFI) / system integration
   (native) / UI (native).
2. Configuration is data, not code. JSON schemas under `/schemas` are the
   contract between shared core and every client.
3. Kill switch and leak protection (DNS, IPv6, LAN) are first-class features,
   not options bolted on later.
4. Observability from day one: structured logs with severity, redactable
   fields (no raw UUIDs / passwords), and an in-app diagnostics view.
5. No silent network calls. Every outbound request from the client itself
   (subscription fetch, update check, telemetry) is listed and user-visible.

## Out of scope for v1

- Built-in server / relay hosting.
- Custom protocols not supported by Xray-core.
- Browser extensions.
- Mesh / multi-hop beyond what Xray-core routing supports natively.

## Success criteria for the first milestone

- One platform (candidate: Android) can import a VLESS+Reality profile, start
  and stop a tunnel, survive network changes, and pass a manual leak check
  (no DNS leak, no IPv6 leak, kill switch holds on tunnel drop).
- Shared Rust core has tests for URI parsing and subscription decoding.
- CI runs `cargo fmt --check`, `cargo clippy -D warnings`, `cargo test`.
