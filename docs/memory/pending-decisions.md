# Pending decisions

Open architectural questions. Resolve explicitly (ideally as an ADR in
`docs/adr/`) before code is built on top of the answer.

## 1. First shipping platform

- Options: Android first, Windows first, or both in parallel.
- Leaning: **Android first** — fastest path from Xray-core to a working
  tunnel, no App Store gating, easy distribution via APK for early
  testers.
- Decision needed: confirm Android as the first target, or override.

## 2. Xray-core consumption on mobile

- Option A: `gomobile bind` → `.aar` (Android) / `.xcframework` (iOS).
  Simpler build, larger binary, harder to strip.
- Option B: build Xray-core as a shared library with a C-ABI shim, link
  from native code. More control over size and threading, more build
  plumbing.
- Leaning: A for v1, migrate to B if size/perf forces it.

## 3. Rust → Kotlin / Swift binding tooling

- Options: UniFFI (from Mozilla), `cxx`, custom JNI/Swift FFI, Mozilla's
  `ffi-support` + hand-rolled bindings.
- Leaning: **UniFFI** — best cost/benefit for a two-platform domain API.
  Confirm before the first FFI function is written.

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
