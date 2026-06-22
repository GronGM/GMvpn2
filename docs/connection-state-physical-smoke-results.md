# ConnectionState physical smoke results

## Scope

This document records redacted manual validation evidence before any
future controlled UI adoption of `ConnectionState`.

This is a docs-only report. Runtime behavior, UI behavior, diagnostics
export, release metadata, tags, GitHub Releases, assets, and Google Play
status were not changed.

## Build and device context

- Build type: installed signed release package.
- App version: `1.1.0-rc.1`.
- Version code: `1010001`.
- Android version/API: Android 12 / API 31.
- Network modes tested: current device network only.
- Profile source: local real test profile, redacted and not committed.
- Screenshots: used only for local visual inspection under ignored
  `.local/`; not committed.
- Raw UI dumps: used only locally where available; not committed.

## Results table

| Check | Result | Redacted evidence |
| --- | --- | --- |
| Install | pass-limited | Installed package metadata was verified; reinstall was not rerun. |
| First launch | pass | App opened to Home. |
| VPN permission grant | pass-limited | Permission prompt was not shown during this run, likely already granted. |
| Valid profile connect | pass | Main CTA connected the active local profile. |
| Android VPN indicator/network visibility | pass-limited | Android VPN network became active during connected state and inactive after disconnect; package ownership was not proven by dumpsys output. |
| Internet access through expected app path | pass-limited | Android VPN network had internet/validated capability while connected; no app-path-specific network probe was captured. |
| Disconnect | pass | Main CTA returned the UI to disconnected state and Android VPN network became inactive. |
| Reconnect | pass | Main CTA restored connected state and Android VPN network became active again. |
| Network change during connect | not tested | Radio/network changes were not performed in the autonomous run. |
| Network change while connected | not tested | Radio/network changes were not performed in the autonomous run. |
| App restart while disconnected | not tested | Not exercised in this run. |
| App restart while connected | not tested | Not exercised in this run. |
| Invalid profile failure | not tested | No safe isolated synthetic-profile setup was used in the installed release package. |
| VPN permission denied flow | not tested | Permission state was not reset because doing so could wipe or disturb the local real profile. |
| Engine start failure simulation | not tested | No safe failure injection was available in the installed release package. |
| Per-app routing allow-list | not tested | Not exercised in this run. |
| Per-app routing disallow-list | not tested | Not exercised in this run. |
| Diagnostics generation | not tested | Raw diagnostics were not captured in the autonomous run. |
| Diagnostics redaction review | pass-limited | UI privacy scan passed; raw diagnostics were intentionally not captured. |
| UI privacy scan | pass | Local screenshots showed only safe profile label/protocol-level information and no raw profile material. |
| Crash/ANR marker scan | pass | No GMvpn crash/ANR markers were found in the checked log tail. |

## ConnectionState acceptance review

- Engine startup alone was not accepted as user-visible `Connected`
  evidence in this smoke.
- Minimum connected evidence expectation remains VPN permission, TUN
  interface, engine started, and no immediate failure.
- Traffic probe alone was not used as `Connected` proof.
- Android VPN visibility and traffic probe evidence were interpreted
  separately.
- Per-app routing interpretation did not rely on naive `adb shell ping`
  alone.

## Privacy review

No committed evidence contains:

- raw URI;
- UUID;
- endpoint IP;
- host or domain;
- port;
- subscription URL;
- token, password, or private key;
- raw diagnostics;
- raw logs;
- screenshots with private data;
- device dumps;
- lower-layer raw exception text;
- APK/AAB;
- `.local/`.

## Blockers

No blocker was found for proposing a separate controlled UI adoption PR
for the basic Home connect/disconnect/reconnect path.

The overall smoke remains pass-limited because the following scenarios
were not exercised in this autonomous run:

- network change during connect;
- network change while connected;
- app restart while disconnected;
- app restart while connected;
- invalid profile failure;
- VPN permission denied flow;
- engine start failure simulation;
- per-app routing allow-list;
- per-app routing disallow-list;
- diagnostics generation and full diagnostics redaction review;
- app-path-specific internet probe.

## Decision

Manual physical smoke pass-limited. Controlled UI adoption may be
proposed only as a separate explicit small PR, and the untested
scenarios above must remain visible in that PR's risk notes.

## Next safe step

Create a separate small Stage 4 controlled UI adoption PR only if the
maintainer accepts the pass-limited smoke scope. Otherwise, repeat the
smoke with the missing scenarios covered.
