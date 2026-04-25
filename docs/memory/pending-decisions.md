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

## 8. tun2socks layer for Android

- API committed: `core/tun2socks.Bridge.Start(tunFD, mtu, socks5Addr)
  / Stop() / IsRunning()`. `gmvpn.Tunnel.Start` drives it. Current
  implementation is a validating stub that returns
  `ErrNotImplemented`.
- Engine options:
  - **gVisor netstack + Go SOCKS5 client** written directly against
    the gVisor version Xray-core pulls (Jan 2026). Single Go binary,
    no extra `.so`, no version conflict. Most work but most control.
  - **`xjasonlyu/tun2socks/v2`** — latest release v2.6.0 (May 2025)
    is currently incompatible: it pins gVisor to a May-2025 build
    while Xray-core pins Jan 2026, and the `udp.ForwarderHandler`
    signature changed in between. Blocked until upstream bumps gVisor
    or we accept maintenance burden of patching it.
  - **`hev-socks5-tunnel`** (C). Smallest binary. Adds a C/JNI
    pipeline.
  - **`badvpn-tun2socks`**. Legacy fallback only.
- Leaning: write a minimal gVisor netstack bridge against the shared
  gVisor pin. See ADR 0004 §3.
