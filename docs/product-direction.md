# GMvpn2 Product Direction

## Current Release Baseline

The current public tester build is `android-v1.1.0-rc.1`.

It is published as a GitHub Pre-release for Android APK testers.

Release facts:

- tag: `android-v1.1.0-rc.1`;
- artifact source SHA: `9105255fefe077756b32df82ac898ab9d121c335`;
- package: `com.gmvpn.client`;
- versionCode: `1010001`;
- versionName: `1.1.0-rc.1`;
- tester asset: `GMvpn2-android-v1.1.0-rc.1-signed.apk`;
- checksum asset: `GMvpn2-android-v1.1.0-rc.1-signed.apk.sha256`;
- AAB asset: not uploaded for ordinary GitHub testers;
- Google Play: not published;
- production/latest release: not created.

Accepted RC limitations remain explicit:

- diagnostics clipboard/export full readback is limited;
- full TalkBack QA is limited;
- UDP is `pass_limited`;
- IPv6 is `not_tested`.

## Product Thesis

GMvpn2 is a privacy-first Android VPN and Xray client.

The product should evolve into a one-button connection orchestrator for
Russia-like usage patterns.

The short product formula is:

> One profile. One button. Multiple transports under the hood.

GMvpn2 should not compete by having the largest protocol matrix.

It should compete by removing user pain:

- safe profile and subscription import;
- safe profile names;
- smart routing;
- one-button connection;
- clear connection state;
- human diagnostics;
- redacted support reports;
- provider-friendly metadata;
- experimental transport chaining under the hood.

The user should not need to understand protocol internals before the app
is useful.

The UI should explain what happened in human language.

The UI must not leak private profile or endpoint data.

## Target Users

Primary users:

- ordinary Android users who import VPN or Xray profiles;
- users in Russia-like environments where some apps need VPN and some
  local services work better directly;
- users who want a GitHub APK without waiting for Google Play;
- users who need safe issue reports without exposing profile secrets.

Secondary users:

- small VPN providers and admins;
- support operators who need redacted diagnostics;
- advanced users who may later test experimental transports;
- maintainers validating routing, DNS, UDP, IPv6, and privacy behavior.

Provider-facing value should stay simple:

- a provider can give one link;
- the user imports it once;
- the app handles connection planning;
- support receives redacted evidence instead of raw profiles.

## Core UX Promise

The default user flow should be:

1. Import a profile or subscription.
2. Select a safe profile name or a provider-recommended profile.
3. Press Connect.
4. Let the app build the connection plan.
5. Show `Connected`, `Degraded`, or `Failed` in human language.
6. Offer redacted diagnostics when support is needed.

Normal users should not need to understand:

- VLESS;
- VMess;
- Trojan;
- Shadowsocks;
- Reality;
- SNI;
- UUID;
- TURN;
- SSH;
- local ports;
- sing-box JSON.

The main screen should keep one primary connection action.

Advanced transport choices should not dominate the normal user flow.

## Strategic Pillars

### Smart Routing

Per-app routing is baseline product functionality.

It is not a premium tier.

Smart Routing should provide:

- selected apps only;
- all apps except selected;
- safe defaults;
- clear app selection;
- clear bypass wording;
- future smart presets;
- validation that routed app traffic uses the Android VPN path.

Examples of useful routing outcomes:

- banking apps can stay direct when the user chooses that policy;
- local services can stay direct when they work better outside VPN;
- selected apps can go through VPN;
- the user can understand which category is active.

Smart Routing must not reveal endpoints, hosts, ports, UUIDs, tokens, or
raw profile data.

### Human Diagnostics

Human Diagnostics is a core product differentiator.

Diagnostics should explain failures without exposing secrets.

Important diagnostic categories:

- no active profile;
- invalid profile;
- VPN permission missing or cancelled;
- VPN interface not established;
- engine unavailable;
- DNS failure;
- server unreachable;
- TLS or Reality handshake failure;
- UDP limited, failed, or not tested;
- IPv6 unavailable or not tested;
- transport setup failed;
- route validation failed.

Diagnostics export must always be redacted.

Diagnostics should prefer categories, timestamps, app version, Android
version, and connection state over raw logs.

Diagnostics must not include:

- raw profile content;
- raw subscription URL;
- raw URI;
- server IP;
- hostname or domain;
- port;
- UUID;
- password;
- token;
- private key;
- cookies;
- authorization headers;
- raw logcat;
- unredacted screenshots.

### Connection Orchestrator

GMvpn2 should introduce an internal connection orchestrator.

The orchestrator should own the connection plan and state transitions.

The UI should not depend on scattered engine callbacks.

The orchestrator should build a `ConnectionPlan` from:

- active profile reference;
- routing mode;
- DNS policy;
- selected transport mode;
- diagnostics policy;
- redaction policy.

The orchestrator should distinguish:

- `Idle`;
- `Preparing`;
- `StartingTransport`;
- `StartingEngine`;
- `Connecting`;
- `Connected`;
- `Degraded`;
- `Failed`;
- `Disconnecting`.

The app must not show normal `Connected` only because an engine process
started.

The app should show `Connected` only when the current connection model
has established the VPN path.

VPN permission cancellation must stop startup work and return to a safe
state.

Invalid profile failures must not create fake connected states.

### Transport Override Layer

Transport Override Layer is the prerequisite for advanced transports.

It should exist before TURN, SSH, local-forward, or similar modes are
implemented.

Saved profiles must remain immutable during connection.

Advanced transports should use ephemeral runtime configuration.

Runtime override may change the dial target for the session.

Runtime override must preserve security-sensitive fields required by the
original profile.

The original endpoint should remain internal-only and redacted.

The UI must not show:

- runtime dial host;
- runtime dial port;
- original endpoint;
- relay endpoint;
- UUID;
- password;
- token;
- raw URI.

Transport override can later support:

- direct profile connection;
- local forward experiments;
- Hysteria2 through Xray if supported by the pinned Xray core;
- user-supplied TURN transport;
- provider-supplied TURN transport;
- SSH transport later;
- optional engine adapters later.

### Engine Strategy

Xray remains the primary stable engine.

Do not start with a protocol-count race.

The preferred order is:

1. Make the current Xray path reliable.
2. Make the current Xray path diagnosable.
3. Centralize connection planning.
4. Add transport override hooks.
5. Validate each fallback mode with controlled evidence.
6. Consider optional engines or protocols later.

Hysteria2 should be evaluated through pinned Xray support first.

sing-box is optional later.

sing-box is not the current rewrite target.

SSH is later work and should go through the transport layer or an
optional engine path.

### TURN Strategy

TURN is experimental.

TURN is an advanced transport option, not a public bypass promise.

TURN work must not start before:

- Connection Orchestrator exists;
- Transport Override Layer exists;
- licensing review is complete;
- security review is complete;
- privacy review is complete;
- controlled validation endpoints are available.

TURN rules:

- no hardcoded third-party TURN infrastructure;
- no committed TURN credentials;
- no committed TURN hostnames;
- no public VK/Yandex bypass wording;
- no claims about bypassing named services;
- only user-supplied or provider-supplied TURN transport;
- redacted docs and diagnostics only.

Acceptable public wording:

- user-supplied TURN transport;
- provider-supplied TURN transport;
- experimental advanced transport.

### Provider Mode

Provider Mode is future work.

It is not a near-term release goal.

Provider Mode should help small providers give users one link and fewer
manual instructions.

Possible subscription metadata:

- provider name;
- support URL;
- provider message or banner;
- expiry timestamp;
- traffic used;
- traffic total;
- recommended routing mode;
- recommended transport;
- fallback order;
- feature flags.

Provider Mode must not send user data to a provider without explicit
user consent.

Provider Mode must not show provider secrets in ordinary UI.

Provider diagnostics must remain redacted.

### GitHub-First Release Quality

GitHub-first release quality is the current priority.

Google Play is not a near-term target.

The project should keep Play-compatible fundamentals where reasonable.

The project should not spend near-term work on:

- Play listing text;
- Play screenshots;
- Play Console production rollout;
- MASA work;
- broad production claims.

GitHub tester releases should keep:

- exact tag-to-artifact-source traceability;
- signed APK for testers;
- SHA-256 checksum for testers;
- no AAB upload unless explicitly needed;
- no retroactive replacement of published assets;
- release notes that disclose limitations;
- physical Android smoke when runtime or UX changed.

## Privacy and Redaction Rules

Never show or commit:

- raw profile URI;
- UUID;
- password;
- token;
- subscription URL;
- server IP;
- hostname;
- domain;
- port;
- private key;
- base64 payload;
- raw diagnostics;
- raw logcat;
- screenshots containing secrets.

Allowed safe data:

- safe user profile name;
- protocol type;
- safe fallback profile label;
- app version;
- Android version;
- package name;
- connection state;
- redacted error category;
- redacted diagnostic summary.

If a label looks like an endpoint or secret, use a fallback name.

## Roadmap

### P0: Maintain GitHub RC Quality

- Keep GitHub APK tester releases traceable.
- Keep signed APK and checksum as primary tester assets.
- Keep AAB out of GitHub tester assets unless explicitly needed.
- Keep release notes honest about limitations.
- Keep release, tag, and asset changes behind explicit approval.

### P1: Stable Xray Android MVP Polish

- Preserve the working Xray path.
- Improve connection state reliability.
- Improve no-profile and invalid-profile flows.
- Keep profile UI privacy-safe.
- Keep diagnostics redacted.

### P2: Smart Routing UX

- Clarify selected-app routing.
- Clarify all-apps-except-selected routing.
- Add safe explanations for bypass behavior.
- Validate that user-app traffic uses the Android VPN path.

### P3: ConnectionPlan and Orchestrator Domain Model

- Define `ConnectionPlan`.
- Define connection state transitions.
- Define permission cancellation behavior.
- Define diagnostics categories.
- Define no-fake-connected rules.

### P4: Transport Override Layer

- Keep saved profiles immutable.
- Generate runtime configs ephemerally.
- Redact original endpoints.
- Preserve TLS, SNI, and Reality fields when required.
- Add tests before enabling advanced transports.

### P5: Hysteria2 Through Xray If Supported

- Evaluate through pinned Xray core first.
- Do not add a separate rewrite just for Hysteria2.
- Require controlled validation evidence.

### P6: TURN Experimental Transport

- Use only user/provider supplied TURN transport.
- Avoid hardcoded third-party endpoints.
- Avoid public bypass claims.
- Require licensing, security, and privacy review.

### P7: Provider Mode Metadata

- Explore provider metadata.
- Keep provider secrets out of UI.
- Keep user consent explicit.
- Keep diagnostics redacted.

### P8: Optional sing-box or SSH Later

- Consider sing-box later as an optional engine.
- Consider SSH later as a transport path.
- Do not make either one the current roadmap driver.

## Non-Goals

- No near-term Google Play work.
- No production/latest release from this document.
- No release tag from this document.
- No GitHub Release asset upload from this document.
- No hardcoded TURN endpoints.
- No hardcoded SSH endpoints.
- No hardcoded provider endpoints.
- No public bypass claims for named services.
- No full sing-box rewrite before the Xray MVP is stable.
- No SSH-first roadmap.
- No broad protocol race.
- No endpoint or secret exposure in UI, logs, docs, or diagnostics.
- No committed APK, AAB, raw diagnostics, screenshots, profiles,
  subscription URLs, IPs, hostnames, passwords, tokens, or keys.
