# ADR 0001: Rust as the shared domain layer

- Status: accepted
- Date: 2026-04-24

## Context

GMvpn2 targets Windows, macOS, Linux, iOS, and Android. The engine is
Xray-core (Go). Without a shared layer, each client would duplicate
URI parsers, subscription logic, routing models, and config assembly.
Three options were considered:

- **A. Rust shared layer**, exposed via FFI (UniFFI + C-ABI).
- **B. Kotlin Multiplatform shared layer.**
- **C. No shared layer beyond JSON schemas.**

## Decision

Variant **A**: a Rust workspace (`gmvpn-core` domain + `gmvpn-ffi`
boundary) is the one place where domain logic lives.

## Rationale

- Rust produces static / dynamic libraries that link cleanly into every
  target platform, including Windows and Linux, which Kotlin
  Multiplatform does not serve well.
- Strong type system and explicit error handling are a good fit for
  security-sensitive code paths (config parsing, routing).
- UniFFI gives Kotlin and Swift bindings without hand-writing JNI or
  Swift FFI.
- C-ABI covers Windows and Linux uniformly.
- No runtime of its own beyond `std`; binary size is predictable.

## Consequences

- We take on a multi-target Rust build (android-ndk, apple toolchains,
  msvc, musl for Linux).
- Contributors need to read Rust; the UI layers stay in their native
  languages.
- FFI boundary must be kept narrow. Domain types do not leak across
  FFI; only serializable DTOs and typed errors do.
- Tooling decision for bindings (UniFFI vs. alternatives) is recorded
  separately in `docs/memory/pending-decisions.md` §3.
