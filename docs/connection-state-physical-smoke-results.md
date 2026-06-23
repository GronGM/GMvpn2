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
- Network modes tested: current device network plus a short Wi-Fi
  toggle with mobile data still available.
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
| Internet access through expected app path | pass | A browser app-path probe loaded a public documentation page while GMvpn showed the connected state. No IP/leak-test page was used. |
| Disconnect | pass | Main CTA returned the UI to disconnected state and Android VPN network became inactive. |
| Reconnect | pass | Main CTA restored connected state and Android VPN network became active again. |
| Network change during connect | pass-limited | A short Wi-Fi toggle was performed immediately after tapping Connect; the app settled into a connected state without visible stale Preparing. This is not a full handoff matrix. |
| Network change while connected | pass-limited | A short Wi-Fi toggle while connected kept the visible UI connected and did not show a fake disconnected/preparing loop. This is one network-change path only. |
| App restart while disconnected | pass | Force-stop and relaunch while disconnected returned to disconnected UI and did not show fake Connected. |
| App restart while connected | pass-limited | Foreground relaunch preserved connected UI; force-stop while connected relaunched safely disconnected without fake Connected. Full process-recovery behavior remains broader QA. |
| Invalid profile failure | pass | A safe synthetic invalid subscription value stayed masked, showed a user-visible failure, and did not create Connected or stuck Preparing. |
| VPN permission denied flow | blocked | VPN permission was already granted. Resetting permission was not performed because it could disturb the local real profile/device state. |
| Engine start failure simulation | blocked | No safe failure injection path was available in the installed signed release package. |
| Per-app routing allow-list | blocked | Not exercised because changing routing policy on the personal device could alter real app routing and shell traffic is not valid standalone evidence. |
| Per-app routing disallow-list | blocked | Not exercised because changing routing policy on the personal device could alter real app routing and shell traffic is not valid standalone evidence. |
| Diagnostics generation | pass-limited | The redacted diagnostics dialog opened. Copy was attempted, but Android/ADB clipboard readback was unavailable; raw diagnostics were not committed. |
| Diagnostics redaction review | pass-limited | The dialog explicitly states that profiles, keys, IPs, hosts, raw URI and full logs are excluded. Full copied text could not be read back through ADB. |
| UI privacy scan | pass | Local screenshots showed only safe profile label/protocol-level information and no raw profile material. |
| Crash/ANR marker scan | pass | No GMvpn crash/ANR markers were found in the checked log tail. |

## Supplemental smoke results

| Scenario | Result | Redacted evidence |
| --- | --- | --- |
| Network change during connect | pass-limited | Connect was started and Wi-Fi was toggled briefly. The app ended in connected UI with no visible stale Preparing. Evidence is limited to one safe network-toggle path. |
| Network change while connected | pass-limited | Wi-Fi was toggled while connected. The app remained visibly connected and no fake/stuck state was observed. |
| App restart while disconnected | pass | Force-stop and relaunch showed disconnected UI and Connect CTA. |
| App restart while connected | pass-limited | Foreground relaunch preserved connected UI. Force-stop while connected relaunched into disconnected UI without fake Connected. |
| Invalid profile failure | pass | Safe synthetic invalid input stayed masked, produced a user-visible import failure, and did not affect connection state. |
| VPN permission denied flow | blocked | Permission reset/cancel was not attempted on the personal device because VPN permission was already granted and resetting it could disturb local setup. |
| Engine start failure simulation | blocked | No safe failure-injection path exists in the installed signed release package. |
| Per-app routing allow-list | blocked | Not tested; changing route policy on the personal device was not safe enough for autonomous smoke. |
| Per-app routing disallow-list | blocked | Not tested; changing route policy on the personal device was not safe enough for autonomous smoke. |
| Diagnostics generation | pass-limited | Redacted diagnostics dialog opened. Copy/export raw evidence was not committed. |
| Full diagnostics redaction review | pass-limited | Dialog redaction boundaries were visible, but full report text could not be read back through ADB clipboard. |
| App-path-specific internet probe | pass | A browser app-path HTTPS page loaded while GMvpn remained visibly connected. No IP/leak-test page was used. |
| Accessibility/TalkBack quick pass | blocked | TalkBack was not toggled to avoid changing global accessibility state on the personal device; UIAutomator text extraction was not reliable for Compose. |
| Crash/ANR marker scan after supplemental scenarios | pass | Local logcat tail scan found zero GMvpn fatal/ANR/crash-like markers. Raw logs stayed under ignored `.local/`. |

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
remain blocked or only partially covered:

- VPN permission denied flow;
- engine start failure simulation;
- per-app routing allow-list;
- per-app routing disallow-list;
- diagnostics generation and full diagnostics redaction review;
- accessibility/TalkBack quick pass;
- broader multi-network handoff beyond a short Wi-Fi toggle.

## Decision

Manual physical smoke pass-limited. Controlled UI adoption may be
proposed only as a separate explicit small PR, and the untested
scenarios above must remain visible in that PR's risk notes.

## Updated decision

Manual physical smoke remains pass-limited. Stage 4 controlled UI
adoption may be proposed only if the maintainer accepts the listed
limitations and preserves them in the PR risk notes.

## Final blocker closure attempt

| Scenario | Result | Redacted evidence | UI adoption impact |
| --- | --- | --- | --- |
| VPN permission denied flow | pass-limited | A debug-only app-op denial path was attempted so the signed local profile was not disturbed. The app stayed disconnected and did not show fake Connected. This was not the real user-cancel dialog. | Permission-denied behavior still needs real dialog coverage before this can count as full-pass. |
| Engine failure simulation | blocked | Source inspection found only test-layer failure categories and no safe injection path in the installed signed package. Runtime code and artifacts were not modified. | Stage 4 cannot claim full failure-path coverage. |
| Per-app routing allow-list | blocked | Not exercised because changing route policy on the personal device could alter real app routing, and shell traffic is not valid standalone evidence. | Stage 4 must keep per-app routing risk notes visible. |
| Per-app routing disallow-list | blocked | Not exercised because changing route policy on the personal device could alter real app routing, and allow/disallow evidence must not be mixed. | Stage 4 must keep per-app routing risk notes visible. |
| Full diagnostics text readback/redaction | pass-limited | Redacted diagnostics dialog opened and copy was attempted. ADB clipboard readback remained unavailable; local scans of accessible clipboard outputs found no secret-pattern hits. Raw diagnostics were not committed. | Diagnostics copy/export still needs manual full-text review before full-pass. |
| Full TalkBack QA | blocked | TalkBack is installed, but it was not enabled because changing global accessibility state on the personal device is invasive and ADB cannot validate spoken output. | Stage 4 remains blocked for full accessibility QA. |
| Broader multi-network handoff | pass-limited | Signed release connected with Wi-Fi disabled, an app-path browser probe loaded, then Wi-Fi was restored and the app stayed in connected UI. Airplane mode and full multi-network matrix were not exercised. | Multi-network behavior is improved but not full-pass. |

## YOURVPNDEAD external scanner evidence

Maintainer-provided screenshots from the local YOURVPNDEAD app were
reviewed only as redacted evidence. The screenshots and raw scanner
output were kept local and were not committed because they contain
sensitive network and device details.

- Third-party scanner detected Android VPN active state.
- Third-party scanner detected VPN transport / VPN network indicators.
- Third-party scanner detected TUN interface.
- Third-party scanner detected app-path/browser-style VPN exit evidence.
- Third-party scanner detected local proxy exposure from the installed
  VPN session.
- Local unauthenticated SOCKS-style proxy exposure was observed by the
  scanner.
- UDP proxy/associate behavior was reported by the scanner.
- DNS and dumpsys findings were observed only as redacted status.
- Screenshots and raw scanner output were kept local only and not
  committed.

| Check | Redacted result | Interpretation |
| --- | --- | --- |
| Android VPN active detection | pass | Supports VPN-path evidence, not sufficient alone for UI adoption |
| TUN interface detection | pass | Supports VPN interface evidence |
| App-path exit evidence | pass-limited | Third-party scanner observed VPN exit, raw IP not committed |
| Local SOCKS proxy exposure | risk-found | External app observed local unauthenticated proxy behavior |
| UDP proxy behavior | risk-found | External app reported UDP proxy/associate behavior |
| DNS/dumpsys checks | pass-limited | Redacted scanner status only |
| Screenshots/raw output | local-only | Not committed because they contain sensitive network/device details |

## YOURVPNDEAD follow-up checks still needed

- Baseline scan before GMvpn connect.
- Scan after GMvpn disconnect to ensure local proxy ports are closed.
- Scan after reconnect to ensure stale local listeners are not left
  behind.
- Per-app allow-list with YOURVPNDEAD included.
- Per-app allow-list with YOURVPNDEAD excluded.
- Per-app disallow-list with YOURVPNDEAD excluded from VPN.
- App restart while connected followed by scanner check.
- App restart while disconnected followed by scanner check.

## Security interpretation

- The external scanner evidence improves app-path validation coverage.
- However, local unauthenticated proxy exposure is a release/privacy risk
  and should be tracked separately before public release.
- This does not authorize Stage 4 UI adoption by itself.
- Stage 4 remains blocked until maintainer either accepts the remaining
  risks explicitly or the remaining blockers are retested.
- Transport Override remains blocked.

## Final UI adoption gate decision

Manual physical smoke final result: blocked. Stage 4 controlled UI
adoption remains blocked. Transport Override and release work remain
blocked.

## Next safe step

Create a separate small Stage 4 controlled UI adoption PR only if the
maintainer accepts the pass-limited smoke scope. Otherwise, repeat the
smoke with the missing scenarios covered.
