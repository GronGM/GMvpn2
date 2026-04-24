# ADR 0003: UniFFI for Kotlin / Swift bindings

- Status: accepted
- Date: 2026-04-24
- Supersedes: `docs/memory/pending-decisions.md` §3 (resolved)

## Context

`shared/gmvpn-core` (ADR 0001) is the single source of domain logic.
Android and iOS/macOS clients need a typed, ergonomic API into it —
URI parsing, subscription decoding, and eventually Xray-core config
assembly and runtime state. Options considered:

- **UniFFI** — Mozilla's bindings generator; one Rust crate exposes
  types/functions via derive + proc-macro, generators produce Kotlin,
  Swift, Python, Ruby.
- **`cxx` / custom JNI + Swift FFI** — hand-rolled bindings per
  platform. Maximum control, maximum maintenance cost.
- **JSON at the boundary** — everything passes as strings. Simple, but
  clients re-parse domain types and we lose compile-time checks on both
  sides.

## Decision

Use **UniFFI 0.31** via its proc-macro mode:

- `#[uniffi::export]` on functions.
- `#[derive(uniffi::Record)]` / `#[derive(uniffi::Enum)]` on DTO types.
- `#[derive(uniffi::Error)]` on `GmvpnError`.
- `uniffi::setup_scaffolding!()` at the bottom of `lib.rs`; no UDL file.

DTOs (`FfiProfile`, `FfiAuth`, `FfiSecurity`, `FfiTransport`, …) live in
`gmvpn-ffi` and are distinct from the domain types in `gmvpn-core`.
`From<Domain> for FfiX` conversions bridge the two in one direction.
UUIDs cross the boundary as lowercase 36-char strings.

Bindings are not committed. Developers and CI regenerate them with
`cd shared && make kotlin|swift|python`, reading the compiled
`.so`/`.dylib` directly (UniFFI's library-introspection mode).

## Rationale

- UniFFI's proc-macro mode means the Rust signature *is* the UDL —
  no second file to drift.
- One crate ships bindings for Kotlin, Swift, Python, and Ruby —
  Python is useful for integration tests and tooling without a device.
- Kotlin output uses JNA (no JNI glue to maintain) and idiomatic
  sealed classes / data classes for our enums/records.
- Migrating to hand-rolled FFI later stays possible: the DTOs are
  plain Rust types, not UniFFI-specific.

## Consequences

- `gmvpn-ffi` publishes three crate types (`cdylib` for Android /
  Linux, `staticlib` for iOS/macOS/Windows, `rlib` for in-workspace
  tests).
- `shared/Makefile` owns the binding-generation workflow. `bindings/`
  is gitignored.
- Android `.aar` assembly still requires a separate step (NDK
  cross-compile via `cargo-ndk` + Gradle packaging). Tracked in
  `clients/android/README.md`; not part of this ADR.
- Clippy lints `needless_pass_by_value` and `must_use_candidate` are
  disabled at the file level in `gmvpn-ffi/src/lib.rs` because UniFFI
  requires owned arguments.

## Alternatives revisited

- `cxx`: more work, no Kotlin target.
- Manual JNI + Swift: too much boilerplate for a two-platform shared
  core at this stage.
- JSON boundary: kept as a fallback inside tests, but not the public API.
