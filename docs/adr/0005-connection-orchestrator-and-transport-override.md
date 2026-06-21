# ADR 0005: Connection Orchestrator and Runtime Transport Override

## Status

Proposed.

## Context

GMvpn2 currently prioritizes GitHub APK tester releases, privacy-safe UI,
and reliable Android VPN behavior. Xray is the primary engine and should
remain the main runtime path while the product matures.

Future work needs a way to introduce fallback transports without turning
the UI into a protocol picker or scattering retry logic across screens,
services, and engine calls. The product should keep the user-facing flow
simple while allowing the runtime to make structured connection choices.

## Decision

Introduce a conceptual `ConnectionPlan` and Runtime Transport Override
Layer before adding advanced transports.

The orchestrator will own:

- connection planning;
- permission-aware startup state;
- transport mode selection;
- safe failure categories;
- cancellation and cleanup;
- redacted diagnostics inputs.

Transport fallback will be represented by a transport override layer
instead of by adding unrelated protocol-specific branches directly to the
UI or tunnel service.

## ConnectionPlan

`ConnectionPlan` should be the safe runtime description of one connection
attempt.

Fields:

- `profile_id`: local profile identifier, not raw profile content.
- `engine`: Xray now, SingBox later only if explicitly added.
- `routing_mode`: app routing policy reference.
- `transport_mode`: selected `TransportMode`.
- `dns_policy`: DNS behavior reference.
- `diagnostics_policy`: safe diagnostic category/report policy.
- `redaction_policy`: policy used before UI, logs, copy, or export.

The plan must not contain raw URI, subscription URL, UUID, password,
token, key, server IP, hostname, domain, or port for UI/logging paths.

## ConnectionState

The orchestrator should report a small state machine:

- `Idle`
- `Preparing`
- `StartingTransport`
- `StartingEngine`
- `Connecting`
- `Connected`
- `Degraded`
- `Failed`
- `Disconnecting`

Rules:

- VPN permission cancellation moves back to `Idle` or `Failed`, never to
  `Connected`.
- Failed profile validation moves to `Failed`, not a fake connected
  state.
- Disconnect cancels any in-flight startup work.
- Diagnostics receive only redacted state and safe error category.

## TransportMode

Initial transport modes:

- `Direct`
- `LocalForwardExperimental`
- `Hysteria2ViaXray`
- `TurnExperimental`
- `SshExperimentalLater`

`Direct` remains the default. Other modes require explicit
implementation, licensing/security review where applicable, and
validation before they can be enabled in runtime UI.

## RuntimeEndpointOverride

`RuntimeEndpointOverride` represents a non-persistent runtime transport
override.

Fields:

- `dial_host`: runtime-only dial host.
- `dial_port`: runtime-only dial port.
- `original_endpoint_internal_only`: original endpoint reference kept out
  of UI/logging paths.
- `preserve_tls_sni`: whether TLS SNI must remain unchanged.
- `preserve_reality_fields`: whether Reality fields must remain
  unchanged.
- `session_id`: local ephemeral session identifier.

Rules:

- Stored profiles are immutable during connection.
- Runtime generated configs are ephemeral.
- Original endpoints are internal-only and redacted.
- UI must not show normal Connected state unless the VPN path is
  established according to the current connection model.
- Advanced transports must not mutate saved profile data.
- TURN/SSH must be implemented only after the orchestrator model is in
  place.

## Rules

- Xray remains the primary engine.
- Per-app routing is baseline functionality, not a premium tier.
- Smart Routing and Human Diagnostics come before protocol sprawl.
- No TURN, SSH, or provider endpoint is hardcoded.
- TURN must not be branded as a bypass for any named service.
- Adding a new transport requires controlled validation evidence.
- Runtime override values must not be committed or printed.
- Any future release still requires normal signed workflow, artifact
  verification, physical smoke, and explicit tag/release approval.

## Consequences

### Positive

- Keeps the UI simple: one profile, one Connect action.
- Gives runtime code a clear place for retries and fallback choices.
- Preserves privacy by separating safe display data from raw profile
  contents.
- Lets the project validate fallback modes independently.
- Reduces pressure to add new protocols before the current engine path is
  mature.

### Negative

- Adds an architectural layer before new transport features become
  visible.
- Requires careful testing of state transitions and cancellation paths.
- May delay experiments with TURN, SSH, or alternate engines until the
  orchestrator contract is stable.

## Non-Goals

- Implementing the orchestrator in this docs branch.
- Adding TURN, SSH, Hysteria2, sing-box, provider mode, or new VPN
  protocols now.
- Changing the current signed `android-v1.1.0-rc.1` release.
- Creating or moving tags.
- Publishing Google Play.
- Committing endpoint values, profile data, screenshots, APKs, AABs, or
  raw diagnostics.
