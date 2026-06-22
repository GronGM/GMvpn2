# Connection Runtime Wiring Plan

## Scope

This document describes the safe staged plan for wiring the new
`ConnectionState` domain model into runtime.

This document does not implement runtime behavior changes.

## Current baseline

- GitHub pre-release `android-v1.1.0-rc.1` exists.
- The `ConnectionPlan`, `ConnectionEvidence`, and `ConnectionState`
  domain model is merged.
- The `TunnelStatus` to `ConnectionState` mapper is merged.
- The mapper is not wired into UI or service runtime.
- Runtime still uses the existing `TunnelStatus` behavior.
- No release, tag, GitHub Release asset, or Google Play work is part of
  this plan.

## Why runtime wiring must be staged

The VPN UI must not show a normal `Connected` state based only on engine
startup.

`VpnService` and VPN interface evidence must be part of the future state
model before `ConnectionState.Connected` is used as a user-visible state.

Android VPN network validation and traffic probes have different
meanings:

- Android VPN network visibility is stronger Android-side evidence that
  the platform sees a VPN network.
- Traffic probes are validation evidence for a specific source, route,
  and policy.

Per-app routing makes naive `adb shell ping` probes unreliable as
standalone VPN-path evidence because shell UID traffic can differ from
user app traffic.

Any runtime wiring must preserve existing app behavior until tests prove
otherwise.

## Runtime wiring principles

- No visible UI behavior change in the first runtime shadow phase.
- No release or version bump for shadow-only
  work.
- No raw profile or endpoint data in state, logs, diagnostics, or tests.
- No Transport Override until `ConnectionState` shadowing is stable.
- No TURN, Hysteria2, sing-box, or SSH work in this wiring plan.
- Keep Xray as the primary engine.
- Preserve existing `TunnelStatus` as the UI source until explicitly
  approved.
- New `ConnectionState` can be computed as a shadow state first.

## Proposed staged implementation

### Stage 0: Already done

- Product direction docs.
- ADR 0005.
- Connection state audit.
- Connection domain model.
- `TunnelStatus` to `ConnectionState` mapper.

### Stage 1: Shadow state only

Goal:

Compute `ConnectionState` next to existing `TunnelStatus`, but do not
show it in UI and do not change runtime decisions.

Allowed:

- internal private property or test-only projection;
- unit tests;
- no UI consumption;
- no diagnostics export exposure yet.

Not allowed:

- changing button labels;
- changing notification state;
- changing foreground service state;
- changing connect, disconnect, or reconnect behavior.

Acceptance:

- existing tests still pass;
- app behavior unchanged;
- no-fake-`Connected` tests pass;
- mapper remains redaction-safe.

### Stage 2: Internal diagnostics preview

Goal:

Use `ConnectionState` only to prepare future redacted diagnostics
categories.

Allowed:

- typed categories only;
- no raw lower-layer error text;
- no endpoint, UUID, token, or password fields.

Not allowed:

- user-visible UI change;
- export of raw profile data;
- release without physical smoke.

Acceptance:

- redaction tests;
- diagnostics snapshot tests;
- manual privacy scan.

### Stage 3: UI state comparison/debug only

Goal:

Compare old `TunnelStatus` and new `ConnectionState` in debug-only or
test-only paths.

Allowed:

- test-only assertions;
- internal logs only if fully redacted and disabled in release;
- no user-visible `Connected` semantics change.

Not allowed:

- changing production UI state source;
- changing release notification state;
- changing connect, disconnect, or reconnect behavior.

Acceptance:

- no fake `Connected`;
- no stale `Preparing`;
- no raw data in logs.

### Stage 4: Controlled UI adoption

Goal:

Only after tests and physical smoke, start using `ConnectionState` for
selected UI labels.

Allowed:

- separate explicit PR only;
- physical real-profile smoke required;
- privacy UI scan required;
- no release tag until validated.

Not allowed:

- changing transport behavior;
- adding TURN, SSH, or Hysteria2;
- using traffic probe alone as `Connected` evidence.

Acceptance:

- install and launch pass;
- real-profile connect, disconnect, and reconnect pass;
- Android VPN network validation pass;
- UI privacy scan pass;
- crash and ANR markers are 0.

### Stage 5: Transport Override readiness

Goal:

Only after `ConnectionState` is stable, use it as a boundary before
LocalForward, TURN, or SSH work.

Allowed:

- docs and ADR updates;
- feature flags;
- user or provider supplied transport only.

Not allowed:

- hardcoded third-party TURN endpoints;
- VK/Yandex bypass wording;
- mutating saved profiles.

## Evidence model for Connected

Future minimum evidence:

- `vpnPermissionPrepared`;
- `vpnInterfaceEstablished`;
- `engineStarted`;
- `immediateFailure` absent.

Stronger evidence:

- `androidVpnNetworkVisible`;
- `trafficProbe` passed.

Important rules:

- A traffic probe failure may become `Degraded`, but must not erase
  minimum interface and engine evidence by itself.
- Engine startup alone is never enough for normal `Connected`.

## Failure mapping plan

Future typed categories:

- `NoProfile`;
- `InvalidProfile`;
- `VpnPermissionDenied`;
- `VpnInterfaceNotEstablished`;
- `EngineUnavailable`;
- `EngineStartFailed`;
- `DnsFailure`;
- `ServerUnreachable`;
- `HandshakeFailure`;
- `PerAppRoutingInvalid`;
- `NetworkChangedReconnectFailed`;
- `Unknown`.

Do not include raw error text in the first wiring phase.

## Per-app routing caution

- Android allow-list and disallow-list are separate runtime modes.
- Do not mix both modes in one plan.
- Selected-apps-only changes how smoke tests should be interpreted.
- `adb shell ping` is not enough as standalone VPN-path evidence.

## Redaction requirements

Never expose:

- raw URI;
- UUID;
- server IP;
- host or domain;
- port;
- subscription URL;
- token or password;
- private key;
- raw diagnostics;
- device dumps;
- screenshots with private data.

## Tests required before any runtime wiring PR

- existing domain tests;
- mapper tests;
- no fake `Connected`;
- no stale `Preparing`;
- disconnect while `Preparing`;
- reconnect clears stale state;
- permission denied;
- establish null or failure;
- engine started without VPN interface;
- per-app allow/disallow separation;
- redaction tests.

## Manual validation required before UI adoption

- signed or debug install;
- real-profile connect;
- internet and VPN path validation;
- disconnect;
- reconnect;
- UI privacy scan;
- crash and ANR markers;
- diagnostics redaction;
- accepted limitations clearly documented.

## Non-goals

- No runtime code changes in this document.
- No release or tag work.
- No Google Play work.
- No TURN.
- No SSH.
- No sing-box.
- No Hysteria2.
- No Transport Override implementation.
- No mutation of saved profiles.
