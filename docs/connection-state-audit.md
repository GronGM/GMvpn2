# Connection State Audit

## Scope

This audit maps the current Android/Xray connection lifecycle before
introducing the Connection Orchestrator.

This document does not implement runtime behavior changes.

The goal is to identify the current state owners, evidence boundaries,
privacy risks, and test gaps so that a future `ConnectionPlan` and
`ConnectionState` model can be introduced without changing live behavior
accidentally.

## Current state sources

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/TunnelStatus.kt`

- Role: UI-facing tunnel status enum.
- State inputs: stable string status values emitted by the Go wrapper,
  plus Android-only permission/pre-start states.
- State outputs: `TunnelStatus` values consumed by `TunnelController`,
  `MainActivity`, `HomeScreen`, diagnostics, and tests.
- Privacy considerations: status values are safe to expose; detail strings
  attached to errors are separate and must stay redacted.

Current values:

- `Idle`
- `Preparing`
- `Starting`
- `Connected`
- `Reconnecting`
- `Stopping`
- `Error`

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/TunnelController.kt`

- Role: process-wide facade between Compose UI and `GmvpnVpnService`.
- State inputs: VPN permission result, user connect/disconnect requests,
  and service status publications.
- State outputs: `StateFlow<TunnelStatus>` and `StateFlow<String?>` for
  the last user-visible error.
- Privacy considerations: `lastError` can contain engine or parser
  details, so UI and diagnostics consumers must redact or categorize it
  before display or export.

Important behavior:

- `preparePermission()` sets `Preparing` before calling
  `VpnService.prepare()`.
- If permission is already granted, `preparePermission()` returns `null`
  and returns the state to `Idle`; the caller then starts the service.
- `onPermissionDenied()` returns `Preparing` to `Idle`.
- `requestStart()` sets `Starting` and starts `GmvpnVpnService`.
- `requestStop()` sets `Stopping` and starts the service stop path.
- `publishStatus(Error, detail)` stores the last error.
- `publishStatus(Connected)` clears the stored error.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/MainActivity.kt`

- Role: UI orchestration, VPN permission launcher, profile validation,
  diagnostics copy/export, and routing screen navigation.
- State inputs: active profile flow, profile list flow, per-app routing
  flow, tunnel status flow, and last-error flow.
- State outputs: `HomeUiState`, `HomeActions`, diagnostics reports, and
  calls into `TunnelController`.
- Privacy considerations: active profile URIs are used internally for
  parsing and protocol summary, but ordinary UI should expose only safe
  profile names and protocol labels. Diagnostics use redacted summaries
  rather than raw URIs.

Important behavior:

- `handleConnect()` checks for a missing active profile before asking for
  VPN permission.
- `handleConnect()` checks supported URI scheme and parser success before
  asking for VPN permission.
- VPN permission `RESULT_OK` calls `TunnelController.onPermissionGranted()`.
- Permission cancel calls `TunnelController.onPermissionDenied()`.
- Diagnostics export writes a redacted report through a `FileProvider`.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/GmvpnVpnService.kt`

- Role: Android `VpnService`, foreground notification owner, TUN owner,
  engine lifecycle owner, stats loop owner, and network-change reconnect
  owner.
- State inputs: service start/stop intents, persisted active profile,
  per-app routing snapshot, profile parser/config builder results,
  `VpnService.Builder.establish()` result, engine start result, engine
  `StatusListener` events, and `ConnectivityManager` callbacks.
- State outputs: status publications to `TunnelController`, foreground
  notification text, TUN descriptor lifetime, engine start/stop calls,
  and cleanup on failure.
- Privacy considerations: service logs can include exception detail; the
  local `redactForLog()` helper redacts profile URIs, HTTP URLs, UUIDs,
  auth-like query/header values, IPv4-like literals, and host-like
  context strings before logging error details.

Important behavior:

- `onStartCommand(ACTION_START)` calls `handleStart()`.
- `onStartCommand(ACTION_STOP)` calls `handleStop()`.
- Android always-on service startup also calls the start path.
- `onRevoke()` calls the stop path.
- `bringTunnelUp()` reads the active profile from `ProfileStore`.
- Missing or unsupported profiles emit a user-visible error and clean up.
- The service starts foreground only after it has an active profile path
  to start.
- `parseProfileUri()` and `buildXrayConfig()` failures emit errors and
  clean up.
- `establishTun()` builds the Android VPN interface and applies per-app
  routing.
- A `null` `establish()` result emits an error and cleans up.
- `EngineBridge.start()` is called only after a non-null VPN descriptor
  exists.
- After engine start returns, the service publishes `Connected`, starts
  stats polling, and registers network callback handling.
- Engine `StatusListener` events can publish additional mapped states or
  errors.
- Stop and failure paths stop the engine, close the TUN descriptor,
  clear the active profile label, stop foreground, and stop the service.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/EngineBridge.kt`

- Role: reflection bridge to the gomobile-generated Xray wrapper.
- State inputs: config JSON, TUN file descriptor, MTU, runtime SOCKS
  inbound port, and a status callback.
- State outputs: engine start/stop, `isRunning()`, traffic stats, and
  Xray version probing.
- Privacy considerations: reflection errors can contain implementation
  detail. User-facing handling should keep them categorical and redacted.

Important behavior:

- Missing gomobile classes raise `EngineUnavailableException`.
- Start invocation failures raise `EngineStartException`.
- The `StatusListener` proxy forwards raw engine status strings to the
  Android callback.
- `xrayVersionOrNull()` is deliberately soft and returns `null` if the
  artifact is missing.

### `core/gmvpn/tunnel.go`

- Role: Go/Xray engine lifecycle and gomobile API surface.
- State inputs: Xray config JSON, TUN descriptor, MTU, runtime SOCKS
  port, bridge start/stop results.
- State outputs: stable string statuses through `StatusListener`, traffic
  stats, and wrapper/Xray version.
- Privacy considerations: error detail can originate from Xray or bridge
  failures. Android must treat detail as sensitive until redacted or
  categorized.

Stable emitted strings:

- `idle`
- `starting`
- `connected`
- `reconnecting`
- `stopping`
- `error`

### `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/ProfileStore.kt`

- Role: encrypted profile library and active profile selection owner.
- State inputs: imported or manually saved profile URIs, custom names,
  source metadata, active index changes.
- State outputs: decrypted profile entries, active index, active URI, and
  library list flows.
- Privacy considerations: raw URIs are encrypted at rest through
  `KeystoreSecrets`; ordinary UI must use safe display names derived by
  profile-display helpers.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/ProfileDisplayName.kt`

- Role: safe profile display-name derivation and custom-name sanitation.
- State inputs: raw profile URI and user-supplied names.
- State outputs: safe display name and protocol label.
- Privacy considerations: rejects labels that look like URIs, endpoints,
  UUIDs, IP-like values, host-like values, auth-like assignments, or raw
  base64 payloads.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/routing/PerAppRouting.kt`

- Role: per-app routing domain model.
- State inputs: selected mode and package set.
- State outputs: routing snapshot applied during TUN establishment.
- Privacy considerations: package names are not profile secrets, but
  diagnostics and support reports should still avoid over-collecting app
  inventory unless the user explicitly chooses it.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/routing/PerAppRoutingStore.kt`

- Role: persisted per-app routing setting.
- State inputs: mode changes, package toggles, clear actions.
- State outputs: flow and one-shot snapshot used by `GmvpnVpnService`.
- Privacy considerations: stores package names in plain DataStore by
  design; it does not store profile secrets.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/diagnostics/*`

- Role: redacted diagnostics rendering and redaction primitives.
- State inputs: app metadata, Android metadata, tunnel status,
  last-error text, selected protocol type, profile count, optional device
  model, optional profile list for extended diagnostics, and logcat tail.
- State outputs: redacted report text and redacted log/profile summaries.
- Privacy considerations: this is a critical boundary. Raw profile URIs,
  endpoints, UUIDs, auth tokens, raw logs, and raw diagnostics must not
  be exported without redaction.

## Current lifecycle

Current start flow:

1. The user presses Connect in `HomeScreen`.
2. `MainActivity.handleConnect()` checks that an active profile exists.
3. `handleConnect()` checks supported URI scheme.
4. `handleConnect()` parses the active profile URI to reject invalid
   profiles before VPN permission.
5. `TunnelController.preparePermission()` sets `Preparing` and calls
   Android `VpnService.prepare()`.
6. If permission UI is needed, `MainActivity` launches the Android VPN
   permission activity.
7. If the user cancels, `TunnelController.onPermissionDenied()` returns
   `Preparing` to `Idle`.
8. If permission is granted, or was already granted,
   `TunnelController.requestStart()` sets `Starting` and starts
   `GmvpnVpnService`.
9. `GmvpnVpnService.handleStart()` serializes startup through
   `tunnelMutex`.
10. `bringTunnelUp()` reads the active profile from `ProfileStore`.
11. `bringTunnelUp()` validates the scheme again and parses the profile.
12. `bringTunnelUp()` builds the Xray config through UniFFI.
13. `bringTunnelUp()` takes a snapshot of per-app routing.
14. `establishTun()` builds the Android VPN interface and calls
   `VpnService.Builder.establish()`.
15. If the VPN descriptor is null, startup fails and cleans up.
16. If the VPN descriptor is non-null, `EngineBridge.start()` is called
   with config, descriptor, MTU, runtime SOCKS port, and status listener.
17. If the engine is missing or fails to start, startup fails and cleans
   up.
18. When engine start returns, the service publishes `Connected`, starts
   stats polling, and registers default-network callback handling.
19. Engine status events can update the controller again through
   `onEngineStatus()`.
20. UI consumes the `TunnelController.status` flow and maps status to
   card tone, title, body, CTA text, and diagnostics actions.

Current stop flow:

1. The user presses Disconnect.
2. `TunnelController.requestStop()` sets `Stopping` and starts the
   service with stop action.
3. `GmvpnVpnService.handleStop()` unregisters network callback.
4. The service stops stats, stops the engine, closes the TUN descriptor,
   clears the active profile label, publishes `Idle`, stops foreground,
   and stops itself.

Current reconnect/network-change flow:

1. After a successful start, the service registers a default network
   callback.
2. The first suitable underlying network notification is recorded.
3. A later suitable underlying network change triggers
   `reconnectOnNetworkChange()`.
4. Reconnect publishes `Reconnecting`, stops stats, stops the engine,
   closes the TUN descriptor, and calls `bringTunnelUp()` again.
5. Failure during reconnect emits an error and runs failure cleanup.

## Current states

### `TunnelStatus.Idle`

- Source: Kotlin enum in `TunnelStatus.kt`; Go emits `idle`.
- Emitted by: controller reset, permission already-granted pre-start
  return, stop path, service destroy, Go stop path.
- Consumed by: Home UI, diagnostics, tests.
- Evidence: no active connected UI state; stop path closes engine and
  TUN before publishing `Idle`.
- Future confusion risk: this is not the same as a future `Disconnected`
  domain state if the future model needs to distinguish never-started,
  stopped, revoked, and failed-cleaned-up.

### `TunnelStatus.Preparing`

- Source: Kotlin-only enum value.
- Emitted by: `TunnelController.preparePermission()`.
- Consumed by: Home UI as an in-flight state.
- Evidence: Android VPN permission is being prepared or shown.
- Future confusion risk: should map to future `Preparing`, but not to
  engine startup or VPN interface establishment.

### `TunnelStatus.Starting`

- Source: Kotlin enum and Go `starting`.
- Emitted by: `TunnelController.requestStart()` and Go engine start.
- Consumed by: Home UI as an in-flight state.
- Evidence: service start has been requested or engine has begun start.
- Future confusion risk: current `Starting` conflates Android service
  start, TUN establishment, and engine startup. Future states should
  split at least `StartingVpnService` and `StartingEngine`.

### `TunnelStatus.Connected`

- Source: Kotlin enum and Go `connected`.
- Emitted by: `GmvpnVpnService.bringTunnelUp()` after
  `EngineBridge.start()` returns, and possibly by mapped engine status.
- Consumed by: Home UI as protected/connected-looking state,
  diagnostics, tests.
- Evidence: current Android service path requires non-null
  `VpnService.Builder.establish()` and successful engine start before
  publishing `Connected`.
- Future confusion risk: future `Connected` should require explicit
  `ConnectionEvidence`, not only a status string or engine running flag.

### `TunnelStatus.Reconnecting`

- Source: Kotlin enum and Go `reconnecting`.
- Emitted by: network callback path and mapped engine status.
- Consumed by: Home UI as in-flight state.
- Evidence: network handover was detected or engine reported reconnect.
- Future confusion risk: current model does not distinguish degraded
  traffic, reconnect pending, reconnect in progress, and reconnect
  failed.

### `TunnelStatus.Stopping`

- Source: Kotlin enum and Go `stopping`.
- Emitted by: `TunnelController.requestStop()` and Go stop path.
- Consumed by: Home UI as disconnecting in-flight state.
- Evidence: explicit stop requested or engine stop in progress.
- Future confusion risk: should map to future `Disconnecting`, but the
  current process-wide status does not expose cleanup substeps.

### `TunnelStatus.Error`

- Source: Kotlin enum and Go `error`.
- Emitted by: validation failure, TUN establishment failure, engine
  missing/start failure, mapped engine error, and catch-all service
  exceptions.
- Consumed by: Home UI as persistent error, diagnostics category, tests.
- Evidence: a detail string is usually stored as last error.
- Future confusion risk: future `Failed` should carry a typed diagnostic
  category and redaction-safe user message, not raw exception text.

## Connected evidence

Current connected-looking UI is driven by `TunnelStatus.Connected`.

In the normal Android service start path, `Connected` is published only
after:

- an active profile exists;
- the profile scheme is supported;
- the profile parses successfully;
- Xray config builds successfully;
- `VpnService.Builder.establish()` returns a non-null descriptor;
- `EngineBridge.start()` returns without throwing;
- failure cleanup did not run.

Important questions:

- Is engine started treated as connected?
  - Mostly yes in the current internal model, but only after the Android
    service has already established a non-null VPN descriptor and engine
    start returns.
- Is `VpnService.Builder.establish()` result checked?
  - Yes. A null descriptor emits an error and cleanup before connected
    state.
- Is a non-null VPN descriptor required?
  - Yes for the service-managed path.
- Is Android VPN network validation used inside runtime state?
  - No. Android VPN network validation is used as release smoke
    evidence, not as an in-app state input.
- Is traffic probing used inside runtime state?
  - No. Traffic probes are validation/support evidence, not current live
    state.
- Can connected-looking UI appear before VPN path is established?
  - The normal service path should not publish `Connected` before a
    non-null descriptor and successful engine start. However, the
    process-wide status model can still be updated by multiple sources,
    so a future orchestrator should centralize connected evidence and
    forbid optimistic state transitions.
- How did `v1.1.0-rc.1` smoke validate this?
  - The signed RC1 readiness notes tie connected action state to an
    active Android VPN network with `INTERNET` and `VALIDATED`
    capabilities, and reject ADB shell ping as standalone VPN-path
    evidence because per-app routing can make shell traffic
    unrepresentative.

Future `Connected` should be based on explicit `ConnectionEvidence`:

- VPN permission prepared.
- VPN interface established.
- Engine started.
- Immediate fatal error absent.
- Optional Android VPN network visibility.
- Optional traffic probe result.

## Failure paths

### No active profile

- Current behavior: UI `handleConnect()` publishes `Error` before
  permission; service start also checks active profile and emits an
  error if no profile exists.
- User-facing message: missing profile body string.
- Redaction risk: low; no raw profile exists.
- Future diagnostic category: `no_active_profile`.

### Invalid profile

- Current behavior: UI checks supported scheme and parser success before
  permission; service repeats scheme, parser, and config-build checks.
- User-facing message: invalid profile body string or categorized
  profile/config error.
- Redaction risk: medium if parser/config exceptions include raw URI
  fragments. UI uses redaction before display; service logs use local
  redaction.
- Future diagnostic category: `invalid_profile` or `config_build_failed`.

### VPN permission denied or cancelled

- Current behavior: permission result callback calls
  `TunnelController.onPermissionDenied()`, which returns `Preparing` to
  `Idle`.
- User-facing message: no explicit new error is stored by cancellation.
- Redaction risk: low.
- Future diagnostic category: `vpn_permission_denied`.

### Engine unavailable

- Current behavior: `EngineBridge` throws `EngineUnavailableException`
  when gomobile classes are missing; service emits error and cleanup.
- User-facing message: engine missing/unbundled category through current
  error path.
- Redaction risk: low to medium; class names are not secrets, but the UI
  should avoid confusing stack traces.
- Future diagnostic category: `engine_unavailable`.

### `VpnService.establish()` failure

- Current behavior: null descriptor emits an error and cleanup.
- User-facing message: establish returned null.
- Redaction risk: low.
- Future diagnostic category: `vpn_interface_failed`.

### Native engine start failure

- Current behavior: `EngineStartException` detail is emitted and cleanup
  runs.
- User-facing message: engine start failed or underlying cause.
- Redaction risk: medium to high; Xray errors can mention endpoints or
  config detail. Must redact before UI/log/export.
- Future diagnostic category: `engine_start_failed`.

### Per-app routing error

- Current behavior: missing package names during builder application are
  logged and ignored; Android builder mode avoids mixing allowed and
  disallowed applications.
- User-facing message: none for ignored missing packages.
- Redaction risk: low for package names, but support reports should not
  collect a full app inventory by default.
- Future diagnostic category: `routing_configuration_warning` or
  `routing_apply_failed`.

### DNS failure

- Current behavior: not a distinct runtime state; DNS servers are added
  during TUN setup and DNS validation is external/manual.
- User-facing message: likely generic tunnel/network failure if surfaced
  by engine.
- Redaction risk: medium if resolver or endpoint values appear in engine
  details.
- Future diagnostic category: `dns_failed` or `dns_degraded`.

### Disconnect during preparing

- Current behavior: stop requests set `Stopping` and service stop path
  publishes `Idle`; permission cancellation also returns `Preparing` to
  `Idle`.
- User-facing message: no explicit message unless prior error persists.
- Redaction risk: low.
- Future diagnostic category: `cancelled_by_user` or no error if user
  initiated.

### Reconnect after network change

- Current behavior: callback publishes `Reconnecting`, stops engine,
  closes TUN, and calls `bringTunnelUp()` again.
- User-facing message: in-flight reconnect UI; failure emits error.
- Redaction risk: medium if reconnect failure details include endpoint
  context.
- Future diagnostic category: `network_changed_reconnect_failed`.

## Per-app routing notes

Current model:

- `Off`: tunnel everything except the GMvpn app itself.
- `IncludeOnly`: call Android allowed-application APIs for selected
  packages, filtering out GMvpn itself.
- `IncludeOnly` with an empty package set falls back to tunnel everything
  except GMvpn itself.
- `ExcludeListed`: disallow GMvpn itself and each selected package.

Android does not allow mixing allowed and disallowed application APIs for
one VPN builder instance. The current `applyPerAppRouting()` branches are
mutually exclusive except for the intentional self-exclusion behavior.

Selected-apps-only affects smoke testing:

- ADB shell traffic can bypass or differ from the user-app routing path.
- `adb shell ping` must not be used as standalone VPN-path evidence.
- Release smoke should prefer Android VPN network evidence plus user-app
  traffic checks from an app/browser path covered by the selected routing
  mode.

Future Smart Routing implications:

- Routing mode should become part of `ConnectionPlan`.
- Probe evidence should record which app or traffic source is being
  tested.
- Diagnostics should explain routing mode without dumping the full app
  list by default.
- A future degraded state may be routing-specific: VPN active, but a
  selected app is not covered by the plan.

## Redaction risks

Areas that could accidentally expose private data:

- Parser/config-build exceptions may include profile details.
- Xray or bridge error details may include destination context.
- Service log messages can expose error strings unless redacted.
- Diagnostics logcat tail can include raw runtime messages if redaction
  misses a pattern.
- Profile display names can leak endpoints if user-supplied labels are
  not sanitized.
- Subscription failures can leak URL or host context if raw exception
  text is shown.
- Per-app diagnostics can over-collect package names if support reports
  include full app inventories by default.

Sensitive data classes to keep out of ordinary UI, logs, diagnostics,
docs, and support reports:

- raw URI;
- UUID;
- server IP;
- host/domain;
- port;
- subscription URL;
- token/password;
- private key material;
- base64 payload.

## Gap analysis

### Duplicated state ownership

Current state is influenced by UI pre-validation, `TunnelController`,
`GmvpnVpnService`, Go engine status events, network callbacks, and tests.
A future orchestrator should own state transitions and expose a single
state stream.

### Optimistic connected state risk

The current service path requires a non-null TUN descriptor and engine
start success before `Connected`, which is good. The gap is that
`Connected` is still just a status value and not a structured evidence
object. Future code should make fake `Connected` impossible by type.

### Unclear evidence for `Connected`

The current runtime state does not include Android VPN network visibility
or traffic probe evidence. Release smoke uses Android system evidence,
but live state does not.

### Missing `Degraded` state

There is no state for partial success, such as VPN interface and engine
running but DNS failing, route validation limited, selected app not
covered, or reconnect pending with existing traffic uncertainty.

### Diagnostics category gaps

Current diagnostics categorize broad errors. Future categories should
separate profile validation, permission denial, VPN interface failure,
engine unavailable, engine start failure, routing apply failure, DNS
failure, reconnect failure, and user cancellation.

### Permission cancellation handling gaps

The cancellation bug is covered by unit tests and current behavior
returns `Preparing` to `Idle`. A future orchestrator should keep this as
a first-class transition: `Preparing -> Idle` with no engine start and no
connected evidence.

### Routing/probe ambiguity

Per-app routing means shell-level probes are not reliable standalone
evidence. Future tests and diagnostics must identify whether probe
traffic belongs to a routed app path.

### Test gaps

Future implementation needs tests around evidence aggregation,
transition ordering, no fake `Connected`, establish-null failure, engine
started without VPN interface, reconnect cleanup, and redaction of new
diagnostic categories.

## Proposed minimal `ConnectionPlan` skeleton

Conceptual docs-only shape:

```text
ConnectionPlan:
  profileRef
  engine = Xray
  routingMode
  transportMode = Direct
  dnsPolicy
  diagnosticsPolicy
  redactionPolicy
```

`profileRef` should point to a persisted profile entry, not duplicate or
mutate the raw profile URI.

`transportMode = Direct` preserves the current Xray path. Transport
Override is a later layer and should not be implemented before the
connection state foundation exists.

## Proposed minimal `ConnectionState` skeleton

Conceptual docs-only shape:

```text
ConnectionState:
  Idle
  Preparing
  StartingVpnService
  StartingEngine
  Connecting
  Connected
  Degraded
  Failed
  Disconnecting
```

Suggested interpretation:

- `Idle`: no active tunnel lifecycle in progress.
- `Preparing`: profile and permission preparation.
- `StartingVpnService`: Android service start requested.
- `StartingEngine`: TUN exists and engine start is in progress.
- `Connecting`: engine started, waiting for optional validation.
- `Connected`: required evidence satisfied.
- `Degraded`: VPN path exists, but optional validation or route/DNS
  evidence is limited or failed.
- `Failed`: terminal failure with typed, redacted reason.
- `Disconnecting`: explicit teardown or cleanup in progress.

## Proposed minimal `ConnectionEvidence` skeleton

Conceptual docs-only shape:

```text
ConnectionEvidence:
  vpnPermissionPrepared
  vpnInterfaceEstablished
  engineStarted
  androidVpnNetworkVisible
  trafficProbeResult optional
  immediateFatalError absent
```

Evidence rules should start conservative:

- No VPN interface means not `Connected`.
- Engine started without VPN interface means not `Connected`.
- Permission denied means no engine start.
- Immediate fatal error means `Failed`.
- Traffic probe should be optional at first because per-app routing can
  make probe source selection ambiguous.

## Suggested implementation stages

### Stage A: domain model types only

Add `ConnectionPlan`, `ConnectionState`, `ConnectionEvidence`, and typed
failure categories. Do not change runtime behavior.

### Stage B: internal mapping only

Map the current `TunnelStatus` and service lifecycle to the new model
internally. Keep existing UI behavior unchanged.

### Stage C: no fake `Connected` tests

Add tests that prove no connected-looking state appears without VPN
interface evidence and engine-start evidence.

### Stage D: `Degraded` after tests

Introduce `Degraded` only after tests define DNS, routing, reconnect,
and optional probe semantics.

### Stage E: `ConnectionPlan` as Transport Override boundary

Use `ConnectionPlan` as the boundary before adding Transport Override.
Do not start TURN, SSH, sing-box, Hysteria2, or provider mode before the
state foundation is stable.

## Test plan for future implementation

Proposed tests:

- no profile -> `Failed` or safe idle message, no engine start;
- invalid profile -> `Failed`, redacted user message;
- permission denied -> safe idle/failed transition, no engine start;
- `establish()` null -> `Failed`, TUN absent, engine not started;
- engine started without VPN interface -> not `Connected`;
- engine start failure -> `Failed`, cleanup closes TUN;
- disconnect while `Preparing` -> `Idle`, no stale in-flight state;
- disconnect while `StartingEngine` -> cleanup and not `Connected`;
- reconnect clears stale state before publishing new connected evidence;
- network callback does not reconnect on the VPN network itself;
- per-app allow/disallow modes are not mixed;
- empty include-only routing falls back to safe all-apps-except-self
  behavior;
- redaction tests for each new diagnostic category;
- diagnostics categories never include raw profile, endpoint, UUID,
  password, token, subscription URL, or base64 payload;
- Android smoke ties connected-looking state to active VPN network
  evidence and not shell ping alone.

## Non-goals

- No runtime code changes in this audit.
- No UI behavior changes in this audit.
- No TURN.
- No SSH.
- No sing-box.
- No Hysteria2.
- No Provider Mode.
- No Google Play work.
- No release/tag work.
- No APK/AAB or release asset work.
