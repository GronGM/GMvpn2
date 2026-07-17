# ADR 0006: Hysteria2 support through the pinned Xray-core

- Status: accepted
- Date: 2026-07-17
- Builds on: ADR 0004 (Xray-core pin), ADR 0001 (Rust shared core)
- Roadmap: resolves `docs/product-direction.md` §P5
  ("Hysteria2 Through Xray If Supported")

## Context

Real tester subscriptions (and the maintainer's own) mix Xray protocols
with `hysteria2://` entries. Until now the URI decoder rejected
`hysteria2://` as an unknown scheme, so such subscriptions imported
"N-1 of N" with a generic parse-failure warning and no explanation. The
open roadmap question P5 was whether Hysteria2 can be served **through
the pinned Xray-core** rather than by adding a second engine
(sing-box / native hysteria), which would be a large architectural
change touching `/core`, the tunnel data path, and the build.

## Evaluation

Upstream Xray-core **added a Hysteria v2 outbound** in January 2026
(the `Add hysteria2 outbound` change). The pinned engine
`v1.260327.0` (calendar tag → 2026-03-27, see ADR 0004 / `core/VERSIONS.md`)
is later than that addition, so **the pinned build already contains the
hysteria2 outbound** — no engine swap and no pin bump is required for the
base capability.

Documented Xray schema (config layer, verified against upstream
`infra/conf/hysteria.go` and `infra/conf/transport_internet.go`):

- outbound `"protocol": "hysteria"` with `settings.version = 2`,
  `settings.address`, `settings.port`;
- `streamSettings.network = "hysteria"`, `security = "tls"`,
  `tlsSettings.serverName` / `allowInsecure`;
- `streamSettings.hysteriaSettings.version = 2` and
  `hysteriaSettings.auth = <password>` (outbound auth lives here, **not**
  in `settings`).

## Decision

1. **Support Hysteria2 through the pinned Xray-core; do not add a second
   engine.** This keeps ADR 0004's single-engine data path intact.

2. **Model.** Add `Protocol::Hysteria2` and `Auth::Hysteria2 { password }`
   to `gmvpn-core` (and the mirrored FFI DTOs + `schemas/profile.schema.json`).
   Hysteria2 is always TLS-over-QUIC, so `Security.mode` is forced to
   `Tls`; `sni`/`allowInsecure` reuse the existing `Security` fields.

3. **URI parser.** `uri/hysteria2.rs` parses `hysteria2://` and the
   `hy2://` alias: userinfo → auth string (`user:pass` preserved), host +
   port required, `sni`/`peer` → SNI, `insecure`/`allowInsecure` →
   `allow_insecure`, fragment → remark.

4. **Config generation.** `xray.rs` builds the hysteria2 outbound with a
   dedicated builder (`build_hysteria2_outbound`) matching the documented
   schema, because the settings shape differs from the vnext/servers
   protocols.

## Scope and honest limits

- This change makes hysteria2 subscriptions **import** (the "1 of 2"
  report is fixed) and **generates a spec-correct Xray config**. It does
  **not** by itself prove on-device connectivity.
- Per project methodology, **real hysteria2 connectivity must be
  confirmed by a physical smoke against a real hysteria2 endpoint**
  before any release note claims it works. That gate is deliberately not
  marked done here.
- **Not yet modelled** (deferred, optional knobs): explicit `up`/`down`
  bandwidth hints (Xray defaults to BBR), UDP port-hopping (`mport`), and
  Salamander (`obfs`) obfuscation. A hysteria2 server that *requires*
  Salamander obfs will not connect until obfs is wired through the model
  and config. This is a documented limitation, not a silent failure — the
  profile still imports.
- If a future pinned Xray build moves `hysteriaSettings` fields into the
  announced `finalmask`/`quicParams` blocks, revisit this builder and the
  pin per `core/VERSIONS.md`.

## Consequences

- P5 is resolved: Hysteria2 rides the existing Xray engine; sing-box is
  not needed for this protocol and stays out of scope.
- The domain model gains its first non-vnext protocol; future non-vnext
  protocols (e.g. TUIC, also Xray-supported) can follow the same
  dedicated-builder pattern.
- Physical validation with a hysteria2 endpoint becomes the next gate
  before advertising hysteria2 in a tester release.
