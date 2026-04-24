# Architecture

## Layers

```
 ┌───────────────────────────────────────────────────────────────┐
 │ UI (native)                                                   │
 │   Android (Kotlin)   iOS/macOS (Swift)   Windows (WinUI/Qt)   │
 │   Linux (Qt/GTK)                                              │
 ├───────────────────────────────────────────────────────────────┤
 │ Platform integration (native)                                 │
 │   VpnService / NEPacketTunnelProvider / WinTUN service / TUN  │
 │   system DNS / routes / kill switch / foreground service      │
 ├───────────────────────────────────────────────────────────────┤
 │ FFI surface (gmvpn-ffi)                                       │
 │   UniFFI (Kotlin, Swift) + C-ABI (Windows, Linux)             │
 ├───────────────────────────────────────────────────────────────┤
 │ Shared domain (gmvpn-core, Rust)                              │
 │   profiles · subscriptions · URI parsers · routing ·          │
 │   config assembly · logging · diagnostics                     │
 ├───────────────────────────────────────────────────────────────┤
 │ Engine (Xray-core, Go)                                        │
 │   inbound/outbound, TLS/Reality, routing executor             │
 └───────────────────────────────────────────────────────────────┘
```

## Responsibility split

### gmvpn-core (Rust)

- Parse and validate user input: `vless://`, `vmess://`, `trojan://`,
  `ss://`, subscription URLs.
- Hold the domain model: `Profile`, `Subscription`, `RoutingRuleset`,
  `AppSettings`.
- Build Xray-core configuration from a `Profile` (`OutboundObject`,
  `InboundObject`, `RoutingObject`).
- Evaluate routing decisions where the host needs them before Xray is
  running (e.g. per-app routing on Android, split tunnel UI previews).
- Emit structured log events with severity and redaction hints.
- **Does not** touch the OS network stack, filesystem paths the platform
  hasn't given it, or any platform-specific API. It is pure logic over
  inputs.

### gmvpn-ffi (Rust)

- Expose a narrow, versioned API to native clients.
- UniFFI definitions for Android (Kotlin) and Apple platforms (Swift).
- A hand-rolled C-ABI for Windows and Linux.
- Serializes errors into typed variants; no panics across the boundary.

### Platform integration (native)

- Owns everything the OS won't let a library touch:
  - acquiring the TUN fd / packet tunnel
  - setting routes and DNS
  - running the foreground service / system extension
  - kill switch enforcement at the OS level
  - secure storage (Keychain / Keystore / DPAPI / libsecret)
- Consumes JSON produced by `gmvpn-core` and hands it to Xray-core.

### Xray-core (Go)

- The actual VPN engine. We consume it, we don't modify it.
- Each platform ships its own build (see
  `docs/memory/pending-decisions.md` §2).

## Data flow: "connect" path (Android example)

1. User taps Connect in the Kotlin UI.
2. Kotlin calls `gmvpn-ffi::start_tunnel(profile_id)` via UniFFI.
3. `gmvpn-core` loads the profile, assembles Xray-core config JSON,
   returns it to the platform layer along with the required tunnel
   parameters (MTU, addresses, DNS, routes).
4. Kotlin's `VpnService` establishes the TUN interface with those
   parameters, starts the foreground service, and hands the TUN fd to
   Xray-core (via gomobile binding or native `.so`).
5. Xray-core starts inbound/outbound and begins processing packets.
6. `gmvpn-core` surfaces status events (`Connecting`, `Connected`,
   `Reconnecting`, `Error`) through an FFI event stream back to the UI.

## Error handling

- Every FFI entry point returns `Result<T, GmvpnError>` with a closed
  enum of error kinds (invalid config, network failure, permission
  denied, engine crash, etc.).
- Panics inside Rust are caught at the FFI boundary and turned into
  `GmvpnError::Internal` with a redacted message.
- Platform layers translate `GmvpnError` into user-facing copy.

## Configuration contract

Single source of truth: `/schemas/*.json`. Any change to profile,
subscription, or routing shape is a schema change first, a code change
second. Consumers:

- `gmvpn-core` — generates Rust types from the schemas (serde).
- Platform UIs — generate view models / validation from the schemas.

## Logging and diagnostics

- Rust core emits structured log events with fields
  `{ ts, level, module, event, redacted: bool, fields }`.
- Platform layers append system-level events (permission requests,
  TUN setup, DNS changes).
- User-initiated "Export diagnostics" produces a redacted bundle: no
  raw server addresses, no UUIDs, no passwords — enough to debug.

## Security posture

- Kill switch is a platform-layer responsibility, enforced by OS
  primitives (Android "Block connections without VPN", iOS
  `includeAllNetworks`, Windows firewall rules, Linux nftables).
- DNS always goes through the tunnel while connected.
- IPv6 either tunneled or blocked; never silently leaked.
- Secrets never cross the FFI boundary as plain strings when avoidable;
  platform stores hold them and `gmvpn-core` receives handles.
