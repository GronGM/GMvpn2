# GMvpn2

Modern cross-platform VPN client built on [Xray-core].

Shared domain logic lives in a Rust workspace; each platform ships its own
native UI and system integration.

## Repository layout

```
core/         Xray-core wrapper and per-platform engine builds (Go)
shared/       Rust workspace:
  gmvpn-core  domain logic (profiles, subscriptions, URI parsers, routing)
  gmvpn-ffi   FFI boundary for clients (UniFFI + C-ABI, planned)
clients/      native apps, one dir per platform (android/ios/macos/windows/linux)
schemas/      JSON Schemas — the single source of truth for config shape
docs/         architecture, ADRs, platform notes, persistent memory
```

## Start here

- [`CLAUDE.md`](CLAUDE.md) — stack, principles, conventions
- [`docs/architecture.md`](docs/architecture.md) — layered architecture
- [`docs/memory/project-context.md`](docs/memory/project-context.md) — goals
  and scope
- [`docs/memory/pending-decisions.md`](docs/memory/pending-decisions.md) —
  what is still open

## Shared crate quickstart

```sh
cd shared
make test       # cargo test (workspace)
make clippy     # cargo clippy -D warnings
make fmt-check  # cargo fmt --check

# UniFFI bindings (generated, not committed):
make kotlin     # bindings/kotlin/…
make swift      # bindings/swift/…
make python     # bindings/python/…
```

CI runs fmt, clippy, and tests on every push and PR that touches `shared/`.

## Core (Go) quickstart

```sh
cd core
make test   # go test ./...
make vet
```

`make android` produces `build/gmvpn.aar` via `gomobile bind` once the
Android NDK + gomobile toolchain are installed (see `core/README.md`).

## Android client

```sh
cd clients/android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest

# Requires an emulator or connected device; not part of mandatory CI yet.
./gradlew :app:connectedDebugAndroidTest
```

Requires JDK 17+ and the Android SDK (compileSdk 35).

## Android RC3 APK для тестеров

Текущая ручная тестовая сборка опубликована как GitHub Pre-release
[`android-v1.0.0-rc.3`][android-rc3]. Для установки скачивайте
`GMvpn-android-v1.0.0-rc.3.apk` из release assets. Не скачивайте
GitHub-архивы "Source code" для тестирования Android-приложения:
это снимки исходного кода, а не устанавливаемые APK.

Перед установкой по возможности проверьте APK по файлу
`GMvpn-android-v1.0.0-rc.3.apk.sha256`. Установите APK на тестовое
Android-устройство, подтвердите системный Android VPN permission
dialog, проверьте connect/disconnect/reconnect с тестовым профилем и
оставьте баг или отзыв через GitHub issue templates.

В отчётах не прикрепляйте приватные VPN-профили, subscription URLs,
пароли, токены, приватные ключи, raw logcat, нередактированные
IP-адреса или скриншоты с персональными данными.

The two native artifacts the app links against are produced by a
single script:

```sh
./scripts/build-android-libs.sh
```

CI does the same on every push to `shared/` or `core/` via
`.github/workflows/android-aar.yml`; artifacts are uploaded as
`gmvpn-android-libs-<sha>.zip`. Full instructions in
`clients/android/README.md`.

## Status

Android v1 release candidate packaging has signed GitHub Actions
artifacts for `android-v1.0.0-rc.1`, whose tag remains tied to the
original RC1 source SHA. Post-RC/P1 source now has signed RC3
candidate artifacts for SDK 35, 16 KB native readiness, and release
blocker cleanup validation. This is not a production/public
distribution claim; the RC3 GitHub Pre-release exists only for manual
APK testing, physical validation is pass-limited, Google Play
publication has not started, and final `android-v1.0.0` still requires
an explicit release decision. Testers can download the signed APK from
the [GMvpn Android v1.0.0 RC3 pre-release][android-rc3]. What works
today:

- **Shared Rust core** — profile / subscription / routing models,
  parsers for `vless://`, `vmess://`, `trojan://`, `ss://` (SIP002 +
  legacy), subscription decoder (uri-list, base64-uri-list, SIP008),
  serde JSON aligned with `schemas/`.
- **UniFFI boundary** — `gmvpn-ffi` exposes a typed Kotlin / Swift /
  Python API for the above via `#[uniffi::export]`. Regenerate bindings
  with `make kotlin|swift|python` in `shared/`.
- **Go Xray-core wrapper** — gomobile-friendly `Tunnel` / `StatusListener`
  API, unit-tested. It embeds the pinned Xray-core and a gVisor
  tun2socks bridge for TCP and SOCKS5 UDP ASSOCIATE.
- **Android client** — Gradle project, Compose UI, `VpnService` + foreground
  notification, VPN permission flow, encrypted multi-profile storage,
  subscription import confirmation, per-app routing, reconnect on network
  changes, diagnostics export, and typed tunnel state machine. If native
  artifacts are absent it surfaces an engine-unavailable error instead of
  crashing.
- **Android validation** - debug build/tests, physical TECNO LG8n
  validation, release APK build, and release bundle build have passed.
  UDP-heavy is documented as `pass_limited` because the available test
  was browser WebRTC/STUN plus a 5-minute YouTube/QUIC-style playback
  window, not controlled iperf throughput/loss. IPv6 was
  `not_applicable` on the tested TECNO/network because there was no
  underlying IPv6 default route.
- **Android release packaging** - manual workflow run
  `27632339860` produced signed RC APK/AAB artifacts and checksums on
  2026-06-16. The RC1 tag was later created; GitHub Release was not.
  RC3 workflow run `27643689894` produced the signed APK now attached
  to the GitHub Pre-release for `android-v1.0.0-rc.3`; only the APK and
  SHA-256 checksum are published there for tester download.

Key ADRs:
[0001 Rust shared core](docs/adr/0001-rust-shared-core.md),
[0002 Android first + gomobile](docs/adr/0002-android-first-gomobile.md),
[0003 UniFFI bindings](docs/adr/0003-uniffi-bindings.md),
[0004 Xray-core pin + tun2socks](docs/adr/0004-xray-core-pin.md).

Next up: see [docs/android-release-signing.md](docs/android-release-signing.md),
[docs/android-v1-rc-notes.md](docs/android-v1-rc-notes.md),
[docs/release-roadmap.md](docs/release-roadmap.md), and
[docs/android-device-validation.md](docs/android-device-validation.md)
for the Android v1 release-candidate audit trail, signing workflow,
and remaining distribution steps.

[Xray-core]: https://github.com/XTLS/Xray-core
[android-rc3]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3
