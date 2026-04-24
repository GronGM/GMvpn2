# ADR 0002: Android first, Xray-core via `gomobile bind`

- Status: accepted
- Date: 2026-04-24
- Supersedes: `docs/memory/pending-decisions.md` §1 and §2 (resolved)

## Context

The repo has a stable shared Rust core (ADR 0001). To put code in users'
hands we need a shipping client. Five platforms are in scope long-term;
we must pick one to lead with.

On mobile, Xray-core (Go) can be consumed two ways:

- **gomobile bind** produces an `.aar` / `.xcframework` the host app can
  call directly.
- **C-ABI shared library** built from Xray-core (`.so` / `.dylib`) linked
  from native code with a hand-written shim.

## Decision

1. **Android is the first shipping platform.**
2. **Xray-core is consumed on Android via `gomobile bind`** in v1.

## Rationale

### Android first

- `android.net.VpnService` is the simplest of the five tunnel primitives:
  no entitlement request to Apple, no kernel driver signing, no Polkit.
- No store gating during bootstrap: APKs can be side-loaded by testers.
- Distribution options (Play, F-Droid, direct APK) are all available
  later and don't block v1.
- The shared Rust core already covers parsing and models; Android can
  exercise it end-to-end on the first real tunnel.

### `gomobile bind` for v1

- Produces a Kotlin-callable `.aar` in one build step; no JNI or C-ABI
  plumbing to maintain alongside the tunnel work.
- Xray-core builds cleanly under gomobile; the `v2rayNG` ecosystem
  proves the approach is viable at scale.
- Downsides (binary size, limited gomobile type system) are acceptable
  for a first release. If they bite, we switch to C-ABI behind an
  unchanged `gmvpn-core`/`clients/android` boundary.

## Consequences

- `/core` becomes a Go module with a gomobile-friendly API surface.
- `/clients/android` consumes the `.aar` from `/core` and the Rust
  `.aar` / `.so` from `/shared/gmvpn-ffi` (once UniFFI bindings land).
- Build prerequisites for full Android compile: JDK 17+, Android SDK
  (compileSdk 34+), Go 1.21+, `gomobile`, optionally Rust with NDK
  targets. Documented in `clients/android/README.md`.
- CI will gain an Android lane once the wrapper `.aar` is produced;
  until then Android is validated manually on developer machines.

## Non-decisions (explicit)

- iOS approach is **not** decided here. iOS will need its own path
  (possibly also gomobile for parity, possibly C-ABI) and its own ADR.
- Xray-core **version pinning** is tracked in `core/VERSIONS.md`, not
  in this ADR; bumping the version does not require a new ADR.
