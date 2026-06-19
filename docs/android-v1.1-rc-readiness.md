# Android v1.1 RC readiness gate

This document prepares the approval package for a future
`android-v1.1.0-rc.1` signed workflow. It does not approve or create a
release, tag, GitHub Release, APK/AAB upload, or Google Play publication.

## v1.1.0-rc.1 readiness

Current source branch:
`codex/p1-play-compliance-and-device-validation`

Pre-metadata readiness SHA:
`66e28ae5aed4b2753cc5d12f33f162be3e20a707`

Release-prep metadata:

- versionCode: `1010001`;
- versionName: `1.1.0-rc.1`;
- package: `com.gmvpn.client`;
- minSdk: `26`;
- targetSdk: `35`.

The exact artifact source SHA is the release-prep commit created after
this metadata update and must be recorded from git before the signed
workflow is run.

Included changes:

- premium v5 live UI;
- Home / Profiles / Import / Settings live mapping;
- icon fidelity pass;
- launcher icon safe-zone fix;
- Kover/CI fix;
- privacy-safe profile display preserved;
- import privacy hardening;
- diagnostics redaction flow preserved.

Validation status:

- unit tests: pass;
- lint: pass;
- assembleDebug: pass;
- real VPN smoke: pass;
- internet through VPN: pass;
- connect / disconnect / reconnect: pass, 2 cycles;
- UI privacy scan: pass;
- diagnostics redaction: pass_limited, clipboard readback unavailable;
- TalkBack/accessibility: pass_limited;
- UDP: pass_limited, from previous validation;
- IPv6: not_tested, no clean external IPv6 baseline;
- Google Play: not published.

Known limitations:

- diagnostics clipboard/export readback not fully confirmed;
- full TalkBack audio QA not fully completed;
- UDP remains pass_limited;
- IPv6 remains not_tested;
- unrestricted production remains blocked.

## Release invariants

- `android-v1.1.0-rc.1` tag: absent at readiness preparation time.
- `android-v1.1.0-rc.1` GitHub Release: not found at readiness
  preparation time.
- `android-v1.0.0` tag remains present and unchanged by this package.
- `android-v1.0.0` GitHub Release remains a pre-release.
- versionCode/versionName are changed only by the release-prep commit
  for `android-v1.1.0-rc.1`.
- No release workflow is run by this package.
- No APK/AAB, raw diagnostics, raw logs, screenshots, private profiles,
  subscription URLs, endpoint/IP/host/password/token values are committed.

## Approval phrase

Do not run a signed RC workflow without the exact approval phrase:

`APPROVE PREPARE SIGNED RC android-v1.1.0-rc.1 WITH DIAGNOSTICS_TALKBACK_UDP_IPV6_LIMITATIONS_ACCEPTED`

Even after that phrase, the next step is only signed workflow execution
and artifact verification. Creating the `android-v1.1.0-rc.1` tag or a
GitHub Release requires a separate approval after artifacts are verified.
