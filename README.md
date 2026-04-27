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
```

Requires JDK 17+ and the Android SDK (compileSdk 34).

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

Early scaffolding. What works today:

- **Shared Rust core** — profile / subscription / routing models,
  parsers for `vless://`, `vmess://`, `trojan://`, `ss://` (SIP002 +
  legacy), subscription decoder (uri-list, base64-uri-list, SIP008),
  serde JSON aligned with `schemas/`.
- **UniFFI boundary** — `gmvpn-ffi` exposes a typed Kotlin / Swift /
  Python API for the above via `#[uniffi::export]`. Regenerate bindings
  with `make kotlin|swift|python` in `shared/`.
- **Go Xray-core wrapper** — gomobile-friendly `Tunnel` / `StatusListener`
  API, unit-tested. Engine hook returns `ErrNotImplemented` until
  Xray-core is pinned and wired.
- **Android client** — Gradle project, Compose UI, `VpnService` + foreground
  notification, VPN permission flow, typed tunnel state machine. Shows
  "engine not wired" until `core/build/gmvpn.aar` is dropped in.

Key ADRs:
[0001 Rust shared core](docs/adr/0001-rust-shared-core.md),
[0002 Android first + gomobile](docs/adr/0002-android-first-gomobile.md),
[0003 UniFFI bindings](docs/adr/0003-uniffi-bindings.md).

Next up: pin Xray-core version, produce the first `gmvpn.aar`, wire it
into `GmvpnVpnService.handleStart()` and get the first real tunnel up on
a device.

[Xray-core]: https://github.com/XTLS/Xray-core
