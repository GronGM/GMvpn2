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
cargo test
cargo clippy --workspace --all-targets -- -D warnings
cargo fmt --all --check
```

CI runs the same three checks on every push and PR that touches `shared/`.

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

Requires JDK 17+ and the Android SDK (compileSdk 34). Full build
instructions in `clients/android/README.md`.

## Status

Early scaffolding. What works today:

- **Shared Rust core** — profile / subscription / routing models,
  `vless://` parser (with Reality), serde JSON aligned with `schemas/`.
- **Go Xray-core wrapper** — gomobile-friendly `Tunnel` / `StatusListener`
  API, unit-tested. Engine hook returns `ErrNotImplemented` until
  Xray-core is pinned and wired.
- **Android client** — Gradle project, Compose UI, `VpnService` + foreground
  notification, VPN permission flow, typed tunnel state machine. Shows
  "engine not wired" until `core/build/gmvpn.aar` is dropped in.

Key ADRs: [0001 Rust shared core](docs/adr/0001-rust-shared-core.md),
[0002 Android first + gomobile](docs/adr/0002-android-first-gomobile.md).

Next up: pin Xray-core version, produce the first `gmvpn.aar`, wire it
into `GmvpnVpnService.handleStart()` and get the first real tunnel up on
a device.

[Xray-core]: https://github.com/XTLS/Xray-core
