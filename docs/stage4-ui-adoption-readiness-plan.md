# Stage 4 UI Adoption Readiness Plan

## Purpose

This document records the post-PR #29 forward path for Stage 4
ConnectionState UI adoption.

It remains a release and runtime-behavior gate. The only currently
authorized implementation is the first small main-screen UI adoption PR
listed below; release work, Transport Override, and new VPN runtime
behavior remain unauthorized.

## Current Accepted Baseline

- PR #27 lifecycle cleanup hardening is merged.
- PR #28 import fix and redacted diagnostics work is merged.
- PR #29 redacted local proxy lifecycle evidence hardening is merged.
- Current P1 HEAD after the readiness gate merge is
  `b54e0253c4487f3e9c56a15dc3501388be7613b6`.
- Profiles import/result baseline is 4 profiles from preserved local
  data.
- Existing local-only physical retest summary is accepted as
  pass-limited evidence for planning only.
- No new physical retest was run for this docs-only readiness plan.

## Accepted Limitation

- Connected-state local proxy non-reachability remains pass-limited.
- PR #29 improved redacted lifecycle evidence and cleanup confidence.
- PR #29 removed raw SOCKS port logging.
- PR #29 does not prove full connected-state local proxy
  non-reachability.
- This limitation must be repeated in any future Stage 4 or release
  handoff.

## Current Stage 4 Allowance

- A first small Stage 4 implementation PR is approved on branch
  `codex/stage4-connection-state-ui-adoption`.
- The scope is limited to wiring the main user-facing connection UI to
  the existing safe `ConnectionState` model.
- The first PR must not change VPN runtime behavior, VpnService
  behavior, Xray, Go, Rust, FFI, gomobile, local proxy listener behavior,
  Transport Override, release metadata, tags, workflows, GitHub Release,
  or Google Play paths.
- The main UI must not show `Connected` from optimistic button clicks,
  legacy `TunnelStatus.Connected` alone, or local proxy lifecycle
  evidence alone.

## Still Blocked

- Any Stage 4 implementation beyond the first main-screen connection
  state UI adoption PR remains blocked until separate explicit
  maintainer approval.
- Transport Override remains blocked.
- Release and RC work remain blocked.
- Google Play publishing remains blocked.
- Version, tag, workflow, and GitHub Release changes remain blocked.

## Gate Before Stage 4 Implementation

Stage 4 implementation may start only after explicit maintainer
approval, and only if all of the following are true:

- P1 branch is clean and current.
- CI is green.
- The PR #29 pass-limited connected-state limitation is explicitly
  acknowledged.
- No new connected-state reachable exposure evidence appears.
- No raw evidence is needed or committed.
- Implementation is limited to connection-state UI adoption only.
- Implementation does not touch Transport Override.
- Implementation does not touch release, version, tag, workflow, GitHub
  Release, or Google Play paths.
- Implementation has rollback-safe behavior and focused test coverage.
- UI must never show Connected unless runtime evidence is sufficient
  according to the existing connection-state model.

## Suggested Future Branches

Use this branch for the approved first small UI-only adoption pass:

```text
codex/stage4-connection-state-ui-adoption
```

Use this branch only if stronger local proxy proof or design is required
before Stage 4:

```text
codex/local-proxy-auth-token-guard-plan
```

The auth/token guard planning branch must include Rust, FFI, Go engine,
gomobile, and Android service path impact analysis before any code
implementation.

## Non-goals

- No Stage 4 implementation beyond the approved first main-screen
  connection state UI adoption pass.
- No Transport Override.
- No release, RC, version, tag, workflow, GitHub Release, or Google Play
  work.
- No raw scanner output, diagnostics, UI dumps, screenshots, profiles,
  subscriptions, endpoint values, credentials, APKs, or AABs in git.

## Decision

Outcome A is accepted for planning:

- PR #29 is merged and verified.
- Local proxy evidence is safer and redacted.
- Cleanup confidence improved after disconnect and service destruction.
- Connected-state non-reachability remains pass-limited.
- No immediate extra mitigation PR is required unless the maintainer
  wants stronger proof.

This outcome does not authorize RC readiness, release claims, Stage 4
implementation, or Transport Override.
