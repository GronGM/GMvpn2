# Pending decisions

Open architectural questions. Resolve explicitly (ideally as an ADR in
`docs/adr/`) before code is built on top of the answer.

## 1. First shipping platform — resolved

- Decision: **Android first** (2026-04-24). See ADR 0002.
- Rationale: shortest path from Xray-core to a live tunnel; no store
  gating during early iteration; APK distribution to testers is trivial;
  `VpnService` is the simplest of the platform tunnel primitives.

## 2. Xray-core consumption on mobile — resolved for v1

- Decision: **`gomobile bind` → `.aar`** for Android v1 (2026-04-24).
  See ADR 0002.
- Migration path to a C-ABI shared library stays open if binary size,
  startup latency, or threading control forces it. No domain code is
  affected by the switch — only `/core` and the Android integration.

## 3. Rust → Kotlin / Swift binding tooling — resolved

- Decision: **UniFFI 0.31** (2026-04-24). See ADR 0003.
- `shared/gmvpn-ffi` is a `cdylib` + `staticlib` + `rlib` crate that
  uses UniFFI's proc-macro mode (`#[uniffi::export]` + `uniffi::setup_scaffolding!`);
  no UDL file is maintained.
- Bindings are generated on demand via `cd shared && make kotlin|swift|python`.

## 4. Windows GUI toolkit

- Options: WinUI 3 (modern, MSIX-friendly, steep learning curve), Qt
  (familiar, large runtime), Avalonia / MAUI (broader reach, different
  native integration story), Tauri (web-based UI, small binary, may
  complicate tunnel integration).
- Decision deferred until Windows work starts; shared core is unaffected.

## 5. Linux privilege model

- Options: root systemd service + user tray over UDS; Polkit-elevated
  helper; setuid helper binary.
- Decision deferred until Linux work starts; shared core is unaffected.

## 6. Telemetry / diagnostics

- What, if anything, do we send home? Proposed default: nothing, all
  diagnostics stay on-device, user can export a redacted log bundle on
  demand. Confirm this stance before any network call is added.

## 7. Update mechanism per platform

- Android: Play Store vs self-updating APK vs F-Droid variants.
- iOS / macOS: App Store vs Developer ID + Sparkle (macOS only).
- Windows: MSIX auto-update vs MSI + in-app updater.
- Linux: distro repos vs Flatpak vs AppImage self-update.
- Not needed for v1 but should be decided before first public release.

## 8. tun2socks layer for Android — resolved

- Decision: **gVisor netstack + `golang.org/x/net/proxy` SOCKS5 (TCP)
  + hand-rolled SOCKS5 UDP ASSOCIATE (UDP)**, all written directly
  against the gVisor pin Xray-core pulls (Jan 2026). See ADR 0004 §3.
- TCP path: `tcp.NewForwarder` → `proxy.SOCKS5` dialer → splice with
  `io.CopyBuffer`.
- UDP path: `udp.NewForwarder` → per-session SOCKS5 UDP ASSOCIATE
  (control TCP + side UDP socket to the relay endpoint). Datagrams
  wrapped with the RFC 1928 §7 header on the way out, unwrapped on
  the way back. Idle timeout 60s.
- Implementation in `core/tun2socks/{bridge.go, socks5_udp.go}`.
- Future polish (not blockers):
  - Swap the per-session ASSOCIATE for a pooled control connection
    if profiling shows excessive TCP setup.
  - Add a domain-resolution cache for SOCKS5 reply ATYP=domain.
