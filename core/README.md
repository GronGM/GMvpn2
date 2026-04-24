# /core — Xray-core wrapper

This directory will hold the per-platform build of Xray-core plus a thin Go
shim that exposes the minimum surface our clients need. It is intentionally
empty until a platform picks it up.

## Responsibilities

- Fetch and pin a specific Xray-core version (tag or commit).
- Produce platform artifacts:
  - Android: `.aar` via `gomobile bind` (initial path) or `.so` + C-ABI.
  - iOS / macOS: `.xcframework` via `gomobile bind`.
  - Windows: `.exe` (service-managed subprocess) or DLL.
  - Linux: static or dynamic binary.
- Expose to the platform layer:
  - start / stop tunnel with a config JSON and a TUN fd
  - traffic stats (per-outbound up/down)
  - log stream
  - runtime DNS changes

## Non-goals

- **No domain logic.** Routing rules, profile models, subscription handling
  all live in `shared/gmvpn-core`. This layer only runs the engine.
- **No patching of Xray-core.** If we need a change upstream, send a PR or
  wrap the behavior externally.

## Open items

See `docs/memory/pending-decisions.md` §2 for the consumption strategy
(gomobile bind vs. shared library) and the pinned Xray-core version.
