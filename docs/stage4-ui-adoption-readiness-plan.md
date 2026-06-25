# Stage 4 UI Adoption Readiness Plan

## Purpose

This document records the post-PR #29 forward path before any Stage 4
ConnectionState UI adoption work can begin.

It is a planning gate only. It does not authorize implementation,
release work, Transport Override, or any new runtime behavior.

## Current Accepted Baseline

- PR #27 lifecycle cleanup hardening is merged.
- PR #28 import fix and redacted diagnostics work is merged.
- PR #29 redacted local proxy lifecycle evidence hardening is merged.
- Current P1 HEAD is
  `2487e1d7920f9e89b2e6ef0de524d437aa5b86bb`.
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

## Still Blocked

- Stage 4 UI adoption implementation remains blocked until separate
  explicit maintainer approval.
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

Use this branch only after explicit approval for Stage 4 implementation:

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

- No Stage 4 implementation in this docs PR.
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
