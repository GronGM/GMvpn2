# GMvpn2 Product Direction

## Current Release Baseline

The current public tester build is `android-v1.1.0-rc.1`, published as a
GitHub Pre-release for APK testers.

- Tag: `android-v1.1.0-rc.1`
- Artifact source SHA: `9105255fefe077756b32df82ac898ab9d121c335`
- Version: `1010001 / 1.1.0-rc.1`
- Tester asset: signed APK plus SHA-256 checksum only
- AAB asset: not uploaded for ordinary testers
- Google Play: not published and not a near-term target

Known release limitations remain explicit:

- UDP: `pass_limited`
- IPv6: `not_tested`
- diagnostics redaction readback: `pass_limited`
- TalkBack/accessibility audio QA: `pass_limited`

## Product Thesis

GMvpn2 should feel like a privacy-first VPN client that makes difficult
network choices on behalf of the user. The product should not ask a
normal user to understand VPN protocols, transports, fallback chains, or
diagnostic logs before they can connect.

GMvpn2 should not compete by having the largest protocol matrix. It
should compete by removing user pain:

- safe import;
- smart routing;
- one-button connection;
- human diagnostics;
- redacted support reports;
- provider-friendly metadata;
- experimental transport chaining under the hood.

The long-term promise is:

> One profile, one button, multiple safe connection strategies under the
> hood.

## Target Users

Primary users:

- Android users who install a GitHub APK directly and need a reliable
  personal VPN client.
- Users who import subscription or URI profiles but do not want raw
  endpoint details displayed in the app.
- Users in Russia-like environments where some apps should use VPN and
  some local or banking services may work better directly.
- Testers who can provide structured feedback without exposing private
  profile data.

Secondary users:

- Power users who understand routing, diagnostics, and network failure
  causes.
- Operators who may later provide controlled endpoints or transport
  fallback infrastructure.
- Small VPN providers/admins who want to give one link instead of asking
  users to install multiple apps and press multiple buttons.

## Core UX Promise

The default product flow should stay simple:

1. Import or add a profile.
2. Select the active profile.
3. Press one Connect button.
4. See `Connected`, `Degraded`, or `Failed` in human language, with a
   safe next action.
5. Export redacted diagnostics for support when needed.

Advanced behavior should be automatic or placed behind diagnostics and
settings. The main UI must never expose server IPs, hostnames, domains,
ports, UUIDs, passwords, raw URIs, subscription URLs, base64 payloads, or
query-like secret labels.

The user should not need to understand VLESS, Reality, SNI, UUID, TURN,
local ports, SSH, or sing-box JSON to make the app useful.

## Strategic Pillars

### Smart Routing

Per-app routing is baseline product functionality, not a premium tier.
It should be treated as part of the normal privacy and control surface:

- clear app selection;
- clear include/exclude behavior;
- safe defaults;
- no endpoint leakage in routing UI;
- validation that routed app traffic uses the Android VPN path.

### Human Diagnostics

Diagnostics should explain what failed without exposing secrets. The
diagnostic UX should prefer categories and actions over raw logs:

- no active profile;
- VPN permission denied;
- engine artifact missing;
- profile validation failed;
- connection timeout;
- network unavailable;
- DNS/UDP/IPv6 limitation;
- diagnostics copied/exported with redaction applied.

Raw logcat, raw profile data, endpoint values, tokens, and subscription
URLs must remain outside committed evidence and issue-friendly reports.

### Connection Orchestrator

The next product layer should be a connection orchestrator that owns the
connection plan and state machine instead of scattering retry and
transport choices across UI or engine calls.

The orchestrator should:

- take the active safe profile reference;
- build a `ConnectionPlan`;
- choose the first allowed transport mode;
- report a clear `ConnectionState`;
- stop background startup work when permission is cancelled;
- avoid fake Connected states;
- record safe failure categories for diagnostics.

### Transport Override Layer

GMvpn2 should support transport-level fallback through an explicit
override layer before adding any new protocol family. This prevents a
protocol race and keeps Xray as the primary engine.

The transport override layer may later support:

- direct profile transport;
- local forwarding;
- SSH tunnel wrapping;
- controlled TURN relay wrapping;
- future engine-specific adapters.

The UI should still present this as one connection flow, not as a list of
technical protocol choices for normal users.

### Provider Mode

Provider Mode is a future operator-facing concept. It is not a near-term
user-facing release goal. If added later, it must avoid storing or
displaying provider secrets in ordinary UI and must keep all endpoint
metadata redacted in diagnostics.

### GitHub-First Release Quality

Google Play is not a near-term target. The product should mature through
GitHub APK pre-releases, physical Android smoke, tester issue forms, and
explicit release gates.

Every tester release should preserve:

- exact tag-to-artifact-source traceability;
- signed APK plus checksum for testers;
- no AAB upload unless explicitly needed;
- release notes that disclose known limitations;
- no retroactive asset replacement.

## Engine Strategy

Xray remains the primary engine. New engines or protocols should not be
added just to expand the feature list.

The preferred order is:

1. Make the current Xray path reliable and diagnosable.
2. Centralize connection planning.
3. Add transport override hooks.
4. Validate each fallback mode with controlled evidence.
5. Only then consider additional engines or protocols.

Hysteria2 should be evaluated through pinned Xray support first. sing-box
is optional later, not a current rewrite. SSH is later, likely through
sing-box or a separate transport layer, after the orchestrator and
override contracts are stable.

## TURN Strategy

TURN is a possible future transport relay, not a branded bypass promise.
It must not be marketed or documented as a guaranteed bypass for any
specific platform or provider.

Rules for future TURN work:

- no hardcoded TURN endpoints;
- no public claims about bypassing named services;
- no committed credentials or hostnames;
- use controlled endpoints only for validation;
- complete licensing, security, and privacy review before integration;
- document latency, failure, and privacy tradeoffs honestly.

## Privacy and Redaction Rules

The product direction keeps the existing privacy rules:

- no endpoint, URI, UUID, password, token, key, subscription URL, or raw
  profile data in UI, logs, diagnostics, screenshots, docs, or issues;
- safe profile names and protocol labels are allowed;
- unsafe labels fall back to generic names such as `VLESS profile` or
  `Profile N`;
- diagnostics must be redacted before copy/export;
- release evidence must be summarized without private values.

## Roadmap

### P0: Maintain GitHub RC Quality

- Keep `android-v1.1.0-rc.1` as a GitHub tester pre-release.
- Collect tester feedback through GitHub Issues.
- Fix privacy, crash, install, connection, and critical UX bugs before
  considering another RC.
- Close diagnostics and TalkBack limitations if they block tester trust.

### P1: Stable Xray Android MVP Polish

- Keep Xray as the stable primary engine.
- Improve real Android smoke evidence and user-visible failure handling.

### P2: Smart Routing UX

- Make selected-app and all-apps-except-selected behavior clear.
- Validate routed user-app traffic through the Android VPN path.

### P3: ConnectionPlan / Orchestrator Domain Model

- Define the connection orchestrator contract.
- Represent safe state transitions and diagnostic categories.

### P4: Transport Override Layer

- Add transport override planning without enabling unvalidated
  transports.
- Keep saved profiles immutable and generate runtime config
  ephemerally.

### P5: Hysteria2 Through Xray If Supported

- Evaluate only through pinned Xray-core support and controlled evidence.

### P6: TURN Experimental Transport

- Allow only user/provider supplied TURN transport after orchestrator and
  override support exist.

### P7: Provider Mode Metadata

- Explore provider name, support URL, expiry, traffic summary,
  recommended routing, fallback order, and feature flags.

### P8: Optional sing-box / SSH Later

- Consider sing-box or SSH only as optional later engine/transport work,
  not as the current roadmap driver.

### Continuous

- Controlled UDP threshold decision.
- Real IPv6 routed or fail-closed validation.
- Human diagnostics and redacted export UX.

## Non-Goals

- No near-term Google Play work.
- No production/latest release without explicit approval.
- No hardcoded TURN, SSH, or provider endpoints.
- No public bypass claims for named services.
- No new VPN protocol race.
- No full sing-box rewrite before the Xray MVP is stable.
- No SSH-first roadmap.
- No release/tag/asset change from this document.
- No committed APK/AAB, raw diagnostics, screenshots with private data,
  profiles, subscription URLs, IPs, hostnames, passwords, tokens, or keys.
