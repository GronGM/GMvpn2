# ConnectionState physical smoke plan

## Purpose

This document defines the manual readiness plan required before any
future controlled UI adoption of `ConnectionState`.

It is docs-only. It does not change runtime behavior, UI state sources,
diagnostics export, release metadata, tags, GitHub Releases, assets, or
Google Play status.

## Preconditions

- PR #21 shadow runtime is merged.
- PR #22 typed diagnostics preview is merged.
- PR #23 debug comparison mode is merged.
- A debug or signed test build is available.
- A real test profile is available locally on the physical test device.
- The test device can show the Android VPN permission flow.
- No release, tag, GitHub Release asset, or Google Play action is part
  of this plan.

## Required devices and environments

- At least one current target Android test device, preferably Android 15
  or Android 16 when available.
- Wi-Fi network.
- Mobile data network, if available.
- Scenario with per-app routing disabled.
- Scenario with per-app routing enabled.
- Scenario with selected-apps-only routing, if supported.
- Scenario with an invalid profile.
- Scenario with VPN permission denied by the user.

## Manual smoke checklist

Record only redacted pass/fail/evidence summaries. Do not store or
commit raw profiles, subscription URLs, endpoints, screenshots with
private data, raw diagnostics, or raw logs.

- Install the test build.
- Launch the app for the first time.
- Grant Android VPN permission.
- Connect a valid local test profile.
- Verify the Android VPN indicator or VPN network visibility where
  available.
- Verify internet access through the expected app path.
- Disconnect.
- Reconnect.
- Change network during connect.
- Change network while connected.
- Restart the app while disconnected.
- Restart the app while connected, if the scenario is safe and
  applicable.
- Trigger invalid profile failure.
- Trigger VPN permission denied flow.
- Simulate engine start failure, if safe and practical.
- Validate per-app routing allow-list behavior.
- Validate per-app routing disallow-list behavior.
- Generate diagnostics.
- Review diagnostics redaction.
- Run UI privacy scan.
- Check crash and ANR markers.

## ConnectionState acceptance criteria

- Engine startup alone must not be enough for user-visible `Connected`.
- Minimum connected evidence must include VPN permission, TUN interface,
  engine started, and no immediate failure.
- Traffic probe alone must not be treated as `Connected` proof.
- Android VPN visibility and traffic probe evidence must be documented
  separately.
- Per-app routing must be interpreted carefully because shell traffic can
  differ from app traffic.
- `TunnelStatus` remains the runtime and UI source of truth until a
  separate controlled UI adoption PR is explicitly approved.

## Privacy acceptance criteria

No manual evidence, diagnostics summary, UI dump, screenshot, or
shareable output may include:

- raw URI;
- UUID;
- endpoint IP;
- host or domain;
- port;
- subscription URL;
- token, password, or private key;
- raw diagnostics;
- screenshots with private data;
- lower-layer raw exception text.

## Failure categories to verify

- `NoProfile`
- `InvalidProfile`
- `VpnPermissionDenied`
- `VpnInterfaceNotEstablished`
- `EngineUnavailable`
- `EngineStartFailed`
- `DnsFailure`
- `ServerUnreachable`
- `HandshakeFailure`
- `PerAppRoutingInvalid`
- `NetworkChangedReconnectFailed`
- `Unknown`

## Explicit blockers before UI adoption

- Any fake `Connected` case.
- Any stale `Preparing` case.
- Any raw data leak.
- Any crash or ANR.
- Any unreviewed diagnostics export change.
- Any untested per-app routing interpretation.
- Any hidden, bidi, control, or Unicode separator finding in changed
  files.
- Any version, release, tag, GitHub Release, asset, or Google Play
  change.

## Next PR after this plan

Only after manual physical smoke is completed and documented, create a
separate explicit controlled UI adoption PR.

That PR must be small. It must not include Transport Override, TURN,
Hysteria2, sing-box, SSH, Provider Mode, release work, version bumps, or
tag work.
