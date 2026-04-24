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

## Status

Early scaffolding. The shared core currently implements:

- Profile / subscription / routing domain models.
- `vless://` URI parser (including Reality params) with unit tests.
- JSON-serializable types aligned with `schemas/`.

Next up: Android client scaffold + Xray-core Android artifact. See the
pending decisions doc for the order.

[Xray-core]: https://github.com/XTLS/Xray-core
