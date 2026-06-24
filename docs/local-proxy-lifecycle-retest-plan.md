# Local Proxy Lifecycle Retest Plan

## Purpose

- Defines a safe physical scanner retest for the local proxy listener
  lifecycle hardening in PR #27.
- This document is a plan only. It does not record scanner results.
- Raw scanner output, screenshots, diagnostics, profiles, endpoints, and
  device dumps must stay local in ignored storage.
- Committed follow-up evidence must use redacted pass/fail summaries only.

## Scope

- Target build: a debug or signed test build produced from the exact PR
  #27 head under test.
- Scanner: YOURVPNDEAD or an equivalent local-app scanner approved by the
  maintainer.
- Goal: verify listener cleanup around disconnect, service destruction,
  reconnect, app restart, per-app routing interpretation, and diagnostics
  redaction.

## Non-goals

- No Stage 4 UI adoption.
- No Transport Override.
- No new protocol work.
- No release, tag, GitHub Release asset, version metadata, or Google
  Play work.
- No raw scanner evidence in git.

## Retest Matrix

| Step | State | Action | Expected result | Release impact |
| --- | --- | --- | --- | --- |
| Baseline before connect | GMvpn disconnected | Run full scanner pass | No GMvpn-owned local proxy listener is observed | Required before interpreting connected evidence |
| Connected scan | GMvpn connected | Run full scanner pass | VPN/TUN/app-path evidence may be observed; any local proxy exposure remains connected-state risk | Does not prove exposure is fixed |
| Disconnect cleanup | Connected, then user disconnects | Run scanner immediately after disconnect | Local proxy listeners are closed | Failure is a release blocker |
| Service destroy cleanup | GMvpn connected | Destroy service/app in a safe local way, then scan | No stale listener remains after destruction | Failure is a release blocker |
| Reconnect lifecycle | Connect, disconnect, connect | Run scanner after reconnect | No stale or duplicate listener state | Failure is a release blocker |
| App restart while connected | GMvpn connected | Restart the app while preserving system VPN state, then scan | State is consistent and no duplicate listener appears | Required before release interpretation |
| App restart while disconnected | GMvpn disconnected | Restart the app, then scan | No fake connected state and no stale listener | Failure is a release blocker |
| Per-app allow-list included | Scanner is included in VPN app list | Run scanner while connected | Scanner evidence applies only to scanner app path | Do not treat as global proof |
| Per-app allow-list excluded | Scanner is excluded from VPN app list | Run scanner while connected | Scanner bypass behavior matches routing policy | Do not treat as global proof |
| Per-app disallow-list excluded | Scanner is excluded from VPN by disallow-list | Run scanner while connected | Scanner bypass behavior matches routing policy | Do not treat as global proof |
| Diagnostics redaction | After scanner pass | Generate shareable diagnostics | No raw profile, endpoint, proxy address, port, URI, token, password, or raw scanner data appears | Failure is a privacy blocker |

## Evidence Handling

- Store raw scanner output only in ignored `.local/` paths.
- Do not commit screenshots, raw logs, diagnostics dumps, profile data, or
  scanner exports.
- Do not paste raw scanner output into issues, PRs, docs, or chat.
- Record only:
  - build source SHA;
  - build type;
  - device model and Android version if user-approved;
  - step name;
  - pass/fail/blocked;
  - redacted notes.

## Safe Listener Evidence

- In-app evidence may record only safe local proxy lifecycle facts:
  - listener state enum;
  - bound-address class, such as loopback/non-loopback/unknown;
  - port class, such as runtime ephemeral/default fallback/unknown;
  - whether the engine is running;
  - whether the TUN is established.
- In-app evidence must not record or export raw port numbers, endpoints,
  tokens, profile values, subscription URLs, scanner output, or raw
  diagnostics.
- This safe evidence improves lifecycle interpretation, but it is not a
  standalone proof that connected-state local proxy exposure is impossible.

## Acceptance Rules

- Passing disconnect, service-destroy, reconnect, and restart cleanup
  checks can support lifecycle cleanup confidence.
- Connected-state loopback exposure is not automatically fixed by PR #27.
- If scanner still observes local proxy exposure while connected, classify
  it as remaining connected-state risk rather than a cleanup regression.
- If scanner observes a listener after disconnect, service destruction, or
  disconnected app restart, block release interpretation and open a focused
  implementation issue.

## Follow-up Decision

- If cleanup checks pass but connected-state exposure remains unacceptable,
  consider a separate PR for local auth/token guard or another compatible
  architecture-level mitigation.
- If cleanup checks fail, fix lifecycle ownership before Stage 4 UI
  adoption or any release claim.
- Stage 4 UI adoption and Transport Override remain blocked until this
  lifecycle retest is reviewed by the maintainer.
