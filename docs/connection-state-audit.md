# Connection State Audit

## Scope

This audit maps the current Android/Xray connection lifecycle before
introducing the Connection Orchestrator.

This document does not implement runtime behavior changes.

This document does not change UI behavior.

This document does not approve release, tag, asset, or Google Play work.

The purpose is to identify the current state owners, evidence boundaries,
privacy risks, and test gaps so future `ConnectionPlan` and
`ConnectionState` work can start from a clear baseline.

The audit is intentionally written as plain LF-delimited Markdown so raw
GitHub view remains readable.

## Current state sources

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/TunnelStatus.kt`

Role:

- Defines the current UI-facing tunnel status enum.
- Maps stable Go wrapper status strings into Kotlin values.
- Adds Android-only pre-engine states.

State inputs:

- Go wrapper strings such as `idle`, `starting`, `connected`,
  `reconnecting`, `stopping`, and `error`.
- Android controller transitions for permission and startup.

State outputs:

- `TunnelStatus` values consumed by UI, diagnostics, tests, and the
  process-wide controller.

Privacy considerations:

- The enum values are safe to display.
- Error detail strings are not part of the enum and must be treated as
  sensitive until redacted or categorized.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/TunnelController.kt`

Role:

- Acts as the process-wide facade around the VPN tunnel.
- Keeps UI away from direct service and engine calls.
- Owns the current `StateFlow<TunnelStatus>` and `StateFlow<String?>`
  for last error.

State inputs:

- User connect and disconnect requests.
- Android VPN permission result.
- Status publications from `GmvpnVpnService`.
- Error detail from the service or validation path.

State outputs:

- Current status flow.
- Last error flow.
- Start and stop intents targeting `GmvpnVpnService`.

Privacy considerations:

- `lastError` can contain parser, engine, or service detail.
- UI must redact visible error text.
- Diagnostics should categorize or redact error detail.

Important current behavior:

- `preparePermission()` sets `Preparing` before calling
  `VpnService.prepare()`.
- If permission is already granted, `preparePermission()` returns `null`
  and resets the status back to `Idle` before the caller starts service.
- `onPermissionDenied()` returns `Preparing` to `Idle`.
- `requestStart()` sets `Starting` and starts the service.
- `requestStop()` sets `Stopping` and starts the stop action.
- `publishStatus(Error, detail)` stores the last error.
- `publishStatus(Connected)` clears the last error.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/MainActivity.kt`

Role:

- Wires Compose state to profile storage, routing storage, tunnel status,
  import flows, diagnostics, and permission result handling.
- Owns the current Android VPN permission launcher.
- Performs first-line profile validation before permission and service
  startup.

State inputs:

- Active profile URI from `ProfileStore`.
- Profile library entries from `ProfileStore`.
- Routing mode from `PerAppRoutingStore`.
- Tunnel status from `TunnelController`.
- Last error from `TunnelController`.
- Subscription import state.

State outputs:

- `HomeUiState` for `HomeScreen`.
- `HomeActions` callbacks.
- Calls to `TunnelController.preparePermission()`.
- Calls to `TunnelController.requestStart()`.
- Calls to `TunnelController.requestStop()`.
- Redacted diagnostics reports.

Privacy considerations:

- The active URI is used internally for parsing and protocol summary.
- Ordinary UI should display safe profile names and protocol labels only.
- Diagnostics should export categories and protocol type, not raw profile
  values.

Important current behavior:

- Missing active profile is rejected before permission.
- Unsupported profile scheme is rejected before permission.
- Parser failure is rejected before permission.
- Permission `RESULT_OK` continues into tunnel start.
- Permission cancel calls `TunnelController.onPermissionDenied()`.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/GmvpnVpnService.kt`

Role:

- Owns the Android `VpnService` lifecycle.
- Owns foreground notification setup and teardown.
- Owns the TUN `ParcelFileDescriptor` lifetime.
- Owns the Android side of engine start and stop.
- Owns default-network callback registration and reconnect.

State inputs:

- `ACTION_START` and `ACTION_STOP` intents.
- Android always-on VPN service start action.
- Active profile URI from encrypted profile storage.
- Per-app routing snapshot.
- UniFFI profile parser result.
- UniFFI Xray config build result.
- `VpnService.Builder.establish()` result.
- `EngineBridge.start()` result.
- Engine `StatusListener` events.
- `ConnectivityManager.NetworkCallback` events.

State outputs:

- Status publications to `TunnelController`.
- Error publications to `TunnelController`.
- Foreground notification content.
- Engine lifecycle calls.
- TUN descriptor creation and closure.
- Stats polling lifecycle.

Privacy considerations:

- Service logs must not print raw profile or endpoint data.
- `emitError()` logs redacted detail through a local redaction helper.
- User-visible error text still needs UI-level redaction.
- Engine details should be assumed sensitive until categorized.

Important current behavior:

- `handleStart()` serializes startup through `tunnelMutex`.
- `bringTunnelUp()` reads the active profile.
- No active profile emits a user-visible error and cleanup.
- Unsupported scheme emits a user-visible error and cleanup.
- Parser failure emits an error and cleanup.
- Config build failure emits an error and cleanup.
- `establishTun()` builds the Android VPN interface.
- `establishTun()` applies per-app routing before `establish()`.
- A null VPN descriptor emits an error and cleanup.
- `EngineBridge.start()` runs only after a non-null descriptor exists.
- Engine missing or engine start failure emits an error and cleanup.
- After engine start returns, service publishes `Connected`.
- Stop path stops stats, stops engine, closes TUN, publishes `Idle`, and
  stops foreground service.
- Reconnect path publishes `Reconnecting`, tears down engine/TUN, and
  calls startup again.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/EngineBridge.kt`

Role:

- Bridges Android Kotlin to gomobile-generated Xray wrapper classes.
- Uses reflection so the app can compile without directly depending on
  generated gomobile classes.
- Creates the `StatusListener` proxy used by Go code.

State inputs:

- Xray config JSON.
- TUN file descriptor.
- MTU.
- Runtime SOCKS inbound port.
- Status callback.

State outputs:

- Engine start.
- Engine stop.
- Engine running probe.
- Traffic stats probe.
- Xray version soft probe.

Privacy considerations:

- Reflection errors can include implementation detail.
- Engine start errors can include sensitive runtime detail.
- Callers should expose categorical, redacted messages.

Important current behavior:

- Missing gomobile classes throw `EngineUnavailableException`.
- Start invocation failure throws `EngineStartException`.
- `xrayVersionOrNull()` returns `null` instead of crashing when the
  native artifact is absent.

### `core/gmvpn/tunnel.go`

Role:

- Owns the Go side of Xray-core and tun2socks bridge lifecycle.
- Exposes a gomobile-friendly `Tunnel` API.
- Emits stable status strings through `StatusListener`.

State inputs:

- Xray config JSON.
- TUN file descriptor.
- MTU.
- SOCKS inbound port.
- Bridge start result.
- Engine start result.
- Bridge stop result.
- Engine close result.

State outputs:

- `starting`.
- `connected`.
- `stopping`.
- `idle`.
- `error`.
- Traffic stats.

Privacy considerations:

- Errors can originate from config loading, Xray startup, bridge startup,
  bridge stop, or engine close.
- Android must treat detail as sensitive.

Important current behavior:

- Empty config is rejected.
- Invalid TUN descriptor is rejected.
- Invalid SOCKS port is rejected.
- Xray config load failure emits `error`.
- Xray instance creation failure emits `error`.
- Xray start failure emits `error`.
- Bridge start failure tears Xray down and emits `error`.
- Only after Xray and bridge startup succeed does Go emit `connected`.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/ProfileStore.kt`

Role:

- Owns encrypted profile library persistence.
- Owns active profile index persistence.
- Migrates older single-profile storage into the library model.

State inputs:

- Manual profile saves.
- Subscription import results.
- Rename operations.
- Active profile selection.
- Remove and clear operations.

State outputs:

- Decrypted profile entries flow.
- Decrypted URI library flow.
- Active index flow.
- Active URI flow.

Privacy considerations:

- Raw profile URIs are encrypted at rest through `KeystoreSecrets`.
- Decrypted values must stay inside trusted runtime paths.
- Ordinary UI must use safe display summaries.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/ProfileDisplayName.kt`

Role:

- Derives safe profile display names.
- Sanitizes user-supplied custom names.
- Provides protocol labels for ordinary UI.

State inputs:

- Raw profile URI.
- Fragment labels.
- VMess display labels.
- User custom names.

State outputs:

- Safe display name.
- Safe protocol label.
- Safe fallback name.

Privacy considerations:

- Rejects labels that look like endpoints, URI-like strings, UUIDs,
  IP-like values, host-like values, auth assignments, or raw base64.
- This is a key privacy boundary for saved profile UI.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/routing/PerAppRouting.kt`

Role:

- Defines the current per-app routing model.
- Represents mode and selected package set.

State inputs:

- Routing mode.
- Selected package names.

State outputs:

- Routing mode and packages consumed by `GmvpnVpnService`.

Privacy considerations:

- Package names are not VPN profile secrets.
- Full app inventory should still not be collected into support reports
  by default.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/routing/PerAppRoutingStore.kt`

Role:

- Persists per-app routing settings.
- Provides a flow for UI and a one-shot snapshot for TUN establishment.

State inputs:

- Mode changes.
- Package toggles.
- Clear selection action.

State outputs:

- `Flow<PerAppRouting>`.
- `snapshot()` result for service startup.

Privacy considerations:

- Stores package names in plain DataStore by design.
- Does not store raw profiles or endpoint values.

### `clients/android/app/src/main/kotlin/com/gmvpn/client/diagnostics/*`

Role:

- Renders redacted diagnostics reports.
- Redacts profile URIs, URLs, UUIDs, auth-like fields, IP-like literals,
  and host-like context.
- Provides error categorization for user-friendly reports.

State inputs:

- App version.
- Version code.
- Package name.
- Android release and API level.
- Optional device manufacturer and model.
- Tunnel status.
- Last error.
- Selected protocol type.
- Profile count.
- Optional profile list for extended diagnostics.
- Optional logcat tail.

State outputs:

- Redacted diagnostics text.
- Last error category.
- Redacted profile summaries.
- Redacted log text.

Privacy considerations:

- Raw profile values must never be exported.
- Raw logs must be redacted before sharing.
- Device model is optional and user-controlled in the short report path.

## Current lifecycle

### Start flow

1. User presses Connect in `HomeScreen`.
2. `MainActivity.handleConnect()` checks whether an active profile exists.
3. Missing profile publishes `TunnelStatus.Error` and returns.
4. Unsupported URI scheme publishes `TunnelStatus.Error` and returns.
5. Parser failure publishes `TunnelStatus.Error` and returns.
6. `TunnelController.preparePermission()` sets `Preparing`.
7. Android `VpnService.prepare()` is called.
8. If permission UI is required, Android permission activity is launched.
9. If user cancels permission, `onPermissionDenied()` returns the state to
   `Idle`.
10. If permission is already granted, the controller returns to `Idle` and
    the caller immediately requests service start.
11. If permission is granted from UI, `onPermissionGranted()` requests
    service start.
12. `requestStart()` sets `Starting` and starts `GmvpnVpnService`.
13. `GmvpnVpnService.handleStart()` enters `tunnelMutex`.
14. `bringTunnelUp()` reads the active profile URI.
15. `bringTunnelUp()` validates URI scheme.
16. `bringTunnelUp()` parses the profile.
17. `bringTunnelUp()` builds the Xray config.
18. `bringTunnelUp()` snapshots per-app routing.
19. `establishTun()` configures Android VPN builder.
20. Per-app routing is applied.
21. `VpnService.Builder.establish()` is called.
22. Null VPN descriptor fails startup and triggers cleanup.
23. Non-null VPN descriptor is stored as `tunInterface`.
24. `EngineBridge.start()` is called with config, TUN descriptor, MTU,
    SOCKS port, and status listener.
25. Missing engine fails startup and triggers cleanup.
26. Engine start failure triggers cleanup.
27. Successful engine start returns to the service.
28. Service publishes `TunnelStatus.Connected`.
29. Service starts stats polling.
30. Service registers default-network callback.
31. UI observes `TunnelStatus.Connected` and shows connected-looking state.

### Stop flow

1. User presses Disconnect.
2. `TunnelController.requestStop()` sets `Stopping`.
3. Stop action is sent to `GmvpnVpnService`.
4. Service unregisters default-network callback.
5. Service stops stats polling.
6. Service stops the engine.
7. Service closes the TUN descriptor.
8. Service clears active notification profile label.
9. Service publishes `Idle`.
10. Service stops foreground mode.
11. Service stops itself.

### Reconnect and network-change flow

1. After successful start, service registers a default network callback.
2. First suitable underlying network is recorded.
3. Later suitable underlying network change triggers reconnect.
4. Service publishes `Reconnecting`.
5. Service stops stats polling.
6. Service stops engine.
7. Service closes TUN descriptor.
8. Service calls `bringTunnelUp()` again.
9. Successful reconnect publishes `Connected` through the same evidence
   boundary as normal start.
10. Failed reconnect emits an error and runs cleanup.

## Current states

### `Idle`

Source:

- Kotlin `TunnelStatus.Idle`.
- Go `idle` string.

Emitted by:

- Controller reset.
- Permission already-granted pre-start handoff.
- Permission cancel from `Preparing`.
- Stop path after cleanup.
- Service destroy.
- Go stop path.

Consumed by:

- Home UI.
- Diagnostics reports.
- Unit and smoke tests.

Evidence:

- No active connected-looking UI state.
- Stop path closes engine and TUN before publishing `Idle`.

Future confusion risk:

- A future model may need to distinguish never-started, disconnected,
  cancelled, revoked, and failed-cleaned-up states.

### `Preparing`

Source:

- Kotlin-only `TunnelStatus.Preparing`.

Emitted by:

- `TunnelController.preparePermission()`.

Consumed by:

- Home UI as an in-flight state.
- Permission cancellation tests.

Evidence:

- Android VPN permission is being prepared or shown.

Future confusion risk:

- `Preparing` must not imply service start.
- `Preparing` must not imply engine start.
- `Preparing` must not imply VPN interface creation.
- `Preparing` must not imply `Connected`.

### `Starting`

Source:

- Kotlin `TunnelStatus.Starting`.
- Go `starting` string.

Emitted by:

- `TunnelController.requestStart()`.
- Go engine startup.

Consumed by:

- Home UI as an in-flight connection state.
- Diagnostics reports.

Evidence:

- Service start has been requested, or engine has started its startup
  sequence.

Future confusion risk:

- Current `Starting` covers too much ground.
- Future state should split service startup, VPN interface setup, and
  engine startup.

### `Connected`

Source:

- Kotlin `TunnelStatus.Connected`.
- Go `connected` string.

Emitted by:

- `GmvpnVpnService.bringTunnelUp()` after a non-null VPN descriptor and
  successful `EngineBridge.start()`.
- Go engine after Xray and bridge startup succeed.
- `onEngineStatus()` when mapped from Go status.

Consumed by:

- Home UI as protected or connected-looking state.
- Diagnostics reports.
- Tests and physical smoke reports.

Evidence:

- Current Android service path requires profile validation, config build,
  non-null VPN interface descriptor, and successful engine start.

Future confusion risk:

- Future `Connected` should not be inferred from engine running alone.
- Future `Connected` should require explicit `ConnectionEvidence`.

### `Reconnecting`

Source:

- Kotlin `TunnelStatus.Reconnecting`.
- Go `reconnecting` string.

Emitted by:

- Android network callback path.
- Go status mapping if emitted by engine.

Consumed by:

- Home UI as an in-flight state.
- Diagnostics reports.

Evidence:

- Underlying network changed or was lost.
- Service is attempting to re-establish tunnel state.

Future confusion risk:

- Reconnecting does not say whether old traffic path is still valid.
- Reconnecting does not distinguish degraded connectivity from complete
  reconnect.

### `Stopping`

Source:

- Kotlin `TunnelStatus.Stopping`.
- Go `stopping` string.

Emitted by:

- `TunnelController.requestStop()`.
- Go stop path.

Consumed by:

- Home UI as disconnecting state.
- Diagnostics reports.

Evidence:

- Explicit stop has been requested or engine stop is in progress.

Future confusion risk:

- Future `Disconnecting` should describe cleanup progress more precisely.

### `Error`

Source:

- Kotlin `TunnelStatus.Error`.
- Go `error` string.

Emitted by:

- Missing active profile.
- Invalid profile.
- Parser failure.
- Config build failure.
- Null VPN descriptor.
- Missing engine artifact.
- Engine start failure.
- Engine status error.
- Reconnect failure.
- Catch-all service exception path.

Consumed by:

- Home UI as persistent user-visible error.
- Diagnostics category mapping.
- Unit and smoke tests.

Evidence:

- A failure detail is usually stored in `TunnelController.lastError`.

Future confusion risk:

- Future `Failed` should carry a typed diagnostic category and redacted
  user message rather than raw exception text.

## Connected evidence

`Preparing` is not `Connected`.

Service start is not `Connected`.

Engine startup beginning is not `Connected`.

Engine started by itself should not become the only future evidence for
`Connected`.

The current Android service path publishes `Connected` only after these
runtime events have happened:

- active profile exists;
- profile scheme is supported;
- profile parser succeeded;
- Xray config build succeeded;
- Android VPN builder returned a non-null descriptor;
- `EngineBridge.start()` returned without throwing;
- immediate fatal cleanup did not run.

The current model does not use Android VPN network validation as a live
state input.

The current model does not use browser or traffic probing as a live state
input.

`android-v1.1.0-rc.1` smoke validation used Android VPN network evidence:

- connected UI action state was observed;
- active Android VPN network was observed;
- the VPN network had `INTERNET` and `VALIDATED` capabilities;
- disconnect removed the active VPN network;
- reconnect restored an active validated VPN network.

`adb shell ping` is not accepted as standalone VPN-path evidence because
per-app routing can make shell UID traffic differ from user app traffic.

A future `ConnectionState` should explicitly separate evidence:

- permission prepared;
- service start requested;
- VPN interface established;
- engine started;
- Android VPN network visible;
- optional traffic probe result;
- immediate fatal error absent.

## Failure paths

### No profile

Current behavior:

- `MainActivity.handleConnect()` rejects before permission.
- Service startup also rejects if active profile is absent.
- No fake `Connected` state should appear.

Future diagnostic category:

- `no_active_profile`.

Redaction risk:

- Low, because no raw profile exists.

### Invalid profile

Current behavior:

- Unsupported or unparseable active profile is rejected before
  permission.
- Service path repeats validation.

Future diagnostic category:

- `invalid_profile`.

Redaction risk:

- Medium, because parser details can include profile-derived text.

### Permission denied or cancelled

Current behavior:

- Permission result callback calls `onPermissionDenied()`.
- `Preparing` returns to `Idle`.
- Engine start is not requested from the denied path.

Future diagnostic category:

- `vpn_permission_denied`.

Redaction risk:

- Low.

### Unsupported URI

Current behavior:

- `hasSupportedProfileScheme()` rejects unsupported schemes in UI and
  service paths.

Future diagnostic category:

- `unsupported_profile_scheme`.

Redaction risk:

- Medium if raw URI text is echoed.

### Parser or build-config failure

Current behavior:

- Parser exceptions and config build exceptions emit errors and cleanup.

Future diagnostic category:

- `profile_parse_failed`.
- `config_build_failed`.

Redaction risk:

- High unless exception text is categorized or redacted.

### VPN establish null or failure

Current behavior:

- Null descriptor from `VpnService.Builder.establish()` emits an error.
- Cleanup runs.
- Engine start is not attempted after null descriptor.

Future diagnostic category:

- `vpn_interface_failed`.

Redaction risk:

- Low.

### Engine unavailable

Current behavior:

- Missing gomobile classes raise `EngineUnavailableException`.
- Service emits error and cleanup.

Future diagnostic category:

- `engine_unavailable`.

Redaction risk:

- Low to medium. Class names are not profile secrets, but raw stack
  traces should not be shown to users.

### Engine start failure

Current behavior:

- `EngineStartException` emits error detail and cleanup.

Future diagnostic category:

- `engine_start_failed`.

Redaction risk:

- High, because engine details can include destination context or config
  fragments.

### Per-app routing failure

Current behavior:

- Missing package names during builder application are logged and
  ignored.
- Android allowed and disallowed modes are not mixed in one builder.

Future diagnostic category:

- `routing_apply_failed`.
- `routing_configuration_warning`.

Redaction risk:

- Low for package names, but full app lists should not be collected by
  default.

### Disconnect during preparing

Current behavior:

- Permission cancellation returns `Preparing` to `Idle`.
- Explicit stop request uses the stop path and should end in `Idle`.

Future diagnostic category:

- `cancelled_by_user` when user initiated.
- No error category if the result is expected cancellation.

Redaction risk:

- Low.

### Reconnect failure

Current behavior:

- Network-change reconnect publishes `Reconnecting`.
- It stops engine, closes TUN, and attempts startup again.
- Failure emits error and cleanup.

Future diagnostic category:

- `network_changed_reconnect_failed`.

Redaction risk:

- Medium to high if engine or route errors include endpoint context.

## Per-app routing notes

Current allow-list and disallow-list handling:

- `Off` tunnels all apps except GMvpn itself.
- `IncludeOnly` uses allowed-application APIs for selected packages.
- Empty `IncludeOnly` falls back to tunnel all apps except GMvpn itself.
- `ExcludeListed` disallows GMvpn itself and each selected package.

Android VPN builder does not allow mixing allowed and disallowed
application APIs in one plan.

A future runtime `ConnectionPlan` must keep the modes mutually exclusive.

A future runtime `ConnectionPlan` must record whether a validation probe
belongs to a routed app path.

`adb shell ping` is not standalone evidence for per-app VPN path.

Smoke tests should account for UID and app routing policy:

- shell traffic can differ from app traffic;
- browser traffic can differ from shell traffic;
- selected-apps-only mode can exclude the test source;
- validation should identify the traffic source.

Future Smart Routing should build on this model instead of adding a
parallel routing state owner.

## Redaction risks

Do not expose these values in ordinary UI, logs, diagnostics, docs, or
support summaries:

- raw URI;
- UUID;
- server IP;
- host/domain;
- port;
- subscription URL;
- token/password;
- base64 payload;
- raw diagnostics.

Risk areas:

- parser exceptions;
- config builder exceptions;
- Xray errors;
- bridge errors;
- service logs;
- diagnostics logcat tail;
- imported profile labels;
- custom profile names;
- subscription fetch failures;
- future transport errors.

Mitigation direction:

- prefer typed categories;
- redact free-form text before display;
- keep raw profile values out of docs and support workflows;
- test every new diagnostic category with redaction cases.

## Gap analysis

### Duplicated state ownership

Current state is influenced by UI validation, `TunnelController`,
`GmvpnVpnService`, Go engine status, network callbacks, and tests.

A future orchestrator should own transition ordering.

### Optimistic connected state risk

The current service path is conservative, but the model still exposes
`Connected` as a plain status.

A future model should make fake `Connected` impossible by requiring
structured evidence.

### Missing `Degraded` state

There is no state for partial success.

Examples that may need `Degraded` later:

- VPN interface exists but DNS validation is limited;
- engine runs but traffic probe fails;
- reconnect is pending after network loss;
- selected app is not covered by routing mode;
- IPv6 behavior is blocked or untested.

### Unclear `Connected` evidence boundary

Current runtime does not encode Android VPN network visibility or traffic
probe result.

Release smoke documents this evidence externally.

Future state should separate required evidence from optional validation.

### Diagnostics category gaps

Current diagnostics categories are broad.

Future categories should distinguish:

- no active profile;
- unsupported profile scheme;
- profile parse failure;
- config build failure;
- VPN permission denied;
- VPN interface failure;
- engine unavailable;
- engine start failure;
- routing apply failure;
- DNS failure;
- reconnect failure;
- user cancellation.

### Permission handling gaps

Permission cancellation is tested and currently returns to `Idle`.

A future orchestrator should keep this as an explicit transition:

```text
Preparing -> Idle
```

The transition must not start the engine.

The transition must not create connected evidence.

### Routing and probe ambiguity

Per-app routing means validation source matters.

Future probes should record:

- traffic source;
- routing mode;
- whether the source is included or excluded;
- whether the probe is optional or required.

### Test gaps

Future implementation needs tests for evidence aggregation, transition
ordering, no fake `Connected`, null establish failure, engine start
without VPN interface, reconnect cleanup, and redaction of new diagnostic
categories.

## Proposed minimal ConnectionPlan skeleton

`ConnectionPlan` is a docs-only conceptual skeleton at this stage.

It should include:

- `profileRef`;
- `engine = Xray`;
- `routingMode`;
- `transportMode = Direct`;
- `dnsPolicy`;
- `diagnosticsPolicy`;
- `redactionPolicy`.

`profileRef` should point at a persisted profile entry.

`profileRef` should not duplicate or mutate raw profile values.

`engine = Xray` preserves the current product direction.

`transportMode = Direct` preserves the current runtime path.

Transport Override should come later.

## Proposed minimal ConnectionState skeleton

`ConnectionState` is a docs-only conceptual skeleton at this stage.

It should include:

- `Idle`;
- `Preparing`;
- `StartingVpnService`;
- `StartingEngine`;
- `Connecting`;
- `Connected`;
- `Degraded`;
- `Failed`;
- `Disconnecting`.

Suggested interpretation:

- `Idle`: no active lifecycle in progress.
- `Preparing`: profile and permission preparation.
- `StartingVpnService`: Android service start requested.
- `StartingEngine`: VPN interface exists and engine startup is in
  progress.
- `Connecting`: engine started and optional validation may be pending.
- `Connected`: required evidence is satisfied.
- `Degraded`: VPN path exists, but optional validation or route/DNS
  evidence is limited or failed.
- `Failed`: terminal failure with typed redacted reason.
- `Disconnecting`: explicit teardown or cleanup in progress.

## Proposed minimal ConnectionEvidence skeleton

`ConnectionEvidence` is a docs-only conceptual skeleton at this stage.

It should include:

- `vpnPermissionPrepared`;
- `vpnInterfaceEstablished`;
- `engineStarted`;
- `androidVpnNetworkVisible`;
- `trafficProbeResult optional`;
- `immediateFatalError absent`.

Initial evidence rules should stay conservative:

- no VPN interface means not `Connected`;
- engine started without VPN interface means not `Connected`;
- permission denied means no engine start;
- immediate fatal error means `Failed`;
- traffic probe should be optional until routing source semantics are
  clear.

## Suggested implementation stages

### Stage A

Add domain model types only.

Do not change behavior.

Do not change UI.

### Stage B

Map the current state machine to the new model internally.

Do not change UI behavior.

Do not change release metadata.

### Stage C

Add no-fake-Connected tests.

Tests should fail if `Connected` can appear without VPN interface and
engine-start evidence.

### Stage D

Add `Degraded` only after tests define its meaning.

Do not use `Degraded` as a vague catch-all.

### Stage E

Use `ConnectionPlan` as the boundary before Transport Override.

Do not start TURN, SSH, sing-box, Hysteria2, or Provider Mode before the
state foundation is stable.

## Test plan for future implementation

Future tests should cover:

- no profile;
- invalid profile;
- unsupported URI;
- permission denied;
- permission cancelled;
- establish null;
- engine unavailable;
- engine start failure;
- engine started without VPN interface;
- disconnect while `Preparing`;
- disconnect while `StartingEngine`;
- reconnect clears stale state;
- reconnect failure cleans up;
- network callback ignores the VPN network itself;
- per-app allow and disallow modes are not mixed;
- empty include-only routing keeps a safe fallback;
- redaction for new error categories;
- diagnostics never include raw profile data;
- Android smoke ties connected-looking state to active VPN network
  evidence and not shell ping alone.

## Non-goals

- No runtime code changes.
- No Android UI behavior changes.
- No TURN.
- No SSH.
- No sing-box.
- No Hysteria2.
- No Provider Mode.
- No Google Play work.
- No release/tag work.
- No GitHub Release asset work.
- No APK/AAB work.
