# Local Proxy Exposure Hardening Plan

## Purpose

- Documents the risk and hardening plan for local proxy exposure
  observed by an external scanner.
- This is a docs-only plan.
- Runtime and UI behavior are unchanged.
- Stage 4 UI adoption is not authorized by this document.

## Background

- YOURVPNDEAD external scanner observed active VPN evidence and local
  proxy exposure.
- Raw screenshots and scanner output stayed local only.
- Committed docs must use only redacted status.

## Risk summary

- Local unauthenticated SOCKS-style proxy exposure was observed by the
  external scanner.
- UDP proxy/associate behavior was reported by the external scanner.
- App-path exit evidence improved validation coverage.
- However, local proxy exposure is a release/privacy risk and must be
  tracked separately.
- This risk does not mean VPN connect failed, but it may mean other
  local apps can observe or use local proxy behavior.

## Required lifecycle checks

| Check | Required result | Release impact |
| --- | --- | --- |
| Baseline before GMvpn connect | No GMvpn-owned local proxy listener observed | Required before release |
| Connected scan | VPN and TUN evidence observed; local proxy exposure documented | Required before UI adoption interpretation |
| Disconnect cleanup | Local proxy listeners closed after disconnect | Release blocker if fail |
| Reconnect lifecycle | No stale or duplicate listeners after reconnect | Release blocker if fail |
| App restart while disconnected | No fake connected and no stale local listener | Release blocker if fail |
| App restart while connected | State remains consistent and no duplicate listeners | Required before release |
| Per-app allow-list with scanner included | Scanner app follows VPN path | Required if per-app routing is supported |
| Per-app allow-list with scanner excluded | Scanner app does not incorrectly prove global VPN path | Required if per-app routing is supported |
| Per-app disallow-list with scanner excluded from VPN | Scanner app bypasses VPN as configured | Required if per-app routing is supported |
| Diagnostics redaction after scanner run | No raw profile/endpoint/proxy data in shareable output | Required before release |

## Hardening candidates for future code PR

These items are planning notes only. They are not implemented by this
docs-only PR.

- Ensure local inbound listeners bind only to loopback, never wildcard.
- Ensure listeners are created only when needed.
- Ensure listeners are closed on disconnect, service stop, engine
  failure, and reconnect.
- Consider random ephemeral local ports per session if compatible with
  tunnel architecture.
- Consider local authentication/token guard if any cross-process local
  proxy use is not required.
- Ensure no local proxy port is exposed in user-visible diagnostics.
- Ensure per-app routing interpretation never treats scanner or shell
  traffic as global proof.
- Add tests for listener lifecycle if architecture supports test seams.

## Acceptance criteria before release

- No local proxy listener remains after disconnect.
- No stale listener remains after reconnect.
- No duplicate listener appears after app restart.
- No raw endpoint/profile/proxy details in diagnostics.
- Per-app routing scanner evidence is interpreted per app, not globally.
- Stage 4 UI adoption remains separate and must not be mixed with
  hardening.

## Non-goals

- No code changes in this PR.
- No UI adoption.
- No release.
- No Transport Override.
- No TURN/Hysteria2/sing-box/SSH.
- No Google Play work.
