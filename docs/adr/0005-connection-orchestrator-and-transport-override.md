# ADR 0005: Connection Orchestrator and Runtime Transport Override

## Status

Proposed.

## Context

GMvpn2 currently has Android VPN and Xray client functionality.

The project also has a GitHub RC release flow for signed APK tester
builds.

The next product direction requires one-button connection orchestration.

The user should not need to create duplicate profiles.

The user should not need to run multiple apps.

The user should not need to understand transport internals before they
can connect.

Future transport work needs a stable domain model first.

Without that model, TURN, SSH, local-forward, or optional engine work
would spread connection logic across UI, services, engine wrappers, and
diagnostics.

That would make state handling harder.

It would also increase the risk of fake `Connected` states and secret
leaks in diagnostics.

## Decision

Introduce a conceptual `ConnectionPlan`.

Introduce a conceptual Runtime Transport Override Layer.

Do this before adding advanced transports.

The orchestrator should own:

- connection planning;
- permission-aware startup;
- transport setup;
- engine startup;
- connection state;
- cancellation;
- disconnect cleanup;
- diagnostics categories;
- redaction boundaries.

The UI should consume safe state.

The UI should not infer connection success only from an engine process
starting.

The saved profile should remain unchanged during advanced transport
attempts.

Runtime generated configs should be ephemeral.

## ConnectionPlan

`ConnectionPlan` is the safe runtime description of one connection
attempt.

Fields:

- `profile_id`;
- `engine`;
- `routing_mode`;
- `transport_mode`;
- `dns_policy`;
- `diagnostics_policy`;
- `redaction_policy`.

`engine` is `Xray` now.

`engine` may support `SingBox` later only if a future ADR and
implementation explicitly add it.

`ConnectionPlan` must not expose raw profile contents to UI or logs.

`ConnectionPlan` must not contain display-ready secrets.

Unsafe data must stay inside internal runtime-only structures.

## ConnectionState

The orchestrator should expose a small connection state model.

States:

- `Idle`;
- `Preparing`;
- `StartingTransport`;
- `StartingEngine`;
- `Connecting`;
- `Connected`;
- `Degraded`;
- `Failed`;
- `Disconnecting`.

State rules:

- VPN permission cancellation must not lead to `Connected`.
- Invalid profile validation must not lead to `Connected`.
- Engine process startup alone must not lead to normal `Connected`.
- Disconnect must cancel in-flight startup work.
- A failed transport setup must produce a safe `Failed` or `Degraded`
  state.
- Diagnostics must receive only safe state and redacted failure
  category.

## TransportMode

Initial conceptual modes:

- `Direct`;
- `LocalForwardExperimental`;
- `Hysteria2ViaXray`;
- `TurnExperimental`;
- `SshExperimentalLater`.

`Direct` is the default path.

`LocalForwardExperimental` is future work.

`Hysteria2ViaXray` is future work and depends on pinned Xray support.

`TurnExperimental` is future work and depends on orchestrator,
transport override, licensing review, security review, privacy review,
and controlled validation.

`SshExperimentalLater` is future work and must not be implemented before
the orchestrator model exists.

## RuntimeEndpointOverride

`RuntimeEndpointOverride` is an ephemeral runtime-only override.

Fields:

- `dial_host`;
- `dial_port`;
- `original_endpoint_internal_only`;
- `preserve_tls_sni`;
- `preserve_reality_fields`;
- `session_id`.

`dial_host` is runtime-only.

`dial_port` is runtime-only.

`original_endpoint_internal_only` must never become ordinary UI text.

`preserve_tls_sni` tells the runtime whether the original SNI must stay
unchanged.

`preserve_reality_fields` tells the runtime whether Reality fields must
stay unchanged.

`session_id` is local and ephemeral.

## Rules

Stored profiles are immutable during connection.

Runtime generated configs are ephemeral.

Original endpoints are internal-only.

Original endpoints are redacted in UI, diagnostics, docs, logs, and
issue reports.

The UI must not show a normal `Connected` state unless the VPN path is
established according to the current connection model.

Advanced transports must not mutate saved profile data.

TURN must not be implemented before the orchestrator model is in place.

SSH must not be implemented before the orchestrator model is in place.

Xray remains the primary engine.

Per-app routing is baseline product functionality.

Per-app routing is not premium.

No hardcoded TURN endpoint is allowed.

No hardcoded SSH endpoint is allowed.

No hardcoded provider endpoint is allowed.

No public VK/Yandex bypass wording is allowed.

No Google Play work is created by this ADR.

## Consequences

### Positive

- One saved profile can support multiple runtime transport choices.
- The normal UI can keep one Connect action.
- Diagnostics can become more consistent.
- State transitions can be tested directly.
- Permission cancellation can be handled in one place.
- Fake connected states become easier to prevent.
- Provider Mode becomes easier later.
- Advanced transports can be validated independently.
- Redaction boundaries become clearer.

### Negative

- The app gains an additional domain model.
- The tunnel startup path needs more state-machine tests.
- Runtime configuration needs careful lifetime management.
- Diagnostics need strict redaction discipline.
- Advanced transport experiments may take longer to expose in UI.

## Non-Goals

This ADR does not implement TURN.

This ADR does not implement sing-box.

This ADR does not implement SSH.

This ADR does not implement Provider Mode.

This ADR does not change release flow.

This ADR does not create a release.

This ADR does not create a tag.

This ADR does not upload assets.

This ADR does not publish Google Play.

This ADR does not commit APKs, AABs, raw diagnostics, screenshots,
profiles, subscription URLs, endpoint values, passwords, tokens, or
keys.
