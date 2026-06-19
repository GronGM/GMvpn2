# Android v1.1 RC readiness gate

This document records the readiness package for the prepared
`android-v1.1.0-rc.1` signed artifacts. It does not approve or create a
release, tag, GitHub Release, APK/AAB upload, or Google Play publication.

## v1.1.0-rc.1 readiness

Current source branch:
`codex/p1-play-compliance-and-device-validation`

Pre-metadata readiness SHA:
`66e28ae5aed4b2753cc5d12f33f162be3e20a707`

Artifact source SHA:
`9105255fefe077756b32df82ac898ab9d121c335`

Signed workflow:

- workflow: `android-release`;
- run ID: `27824970999`;
- status: success.

Release-prep metadata:

- versionCode: `1010001`;
- versionName: `1.1.0-rc.1`;
- package: `com.gmvpn.client`;
- minSdk: `26`;
- targetSdk: `35`.

Signed artifact verification:

- signed APK: produced;
- signed AAB: produced;
- APK SHA256:
  `f8d64b5ee2e4d6e14c9aa0606124847ab747b1a8a683756ff7690e68a1325848`;
- AAB SHA256:
  `84f05ff15b6827920b09e1aa8024f9210995c3caf13505643ed5d05206475327`;
- APK signature: pass;
- AAB jarsigner and bundletool validate: pass, with expected RC
  self-signed certificate warnings;
- 16 KB ELF alignment: pass for APK and AAB;
- APK `zipalign -P 16`: pass;
- APK metadata: `com.gmvpn.client`, `versionCode` `1010001`,
  `versionName` `1.1.0-rc.1`, `minSdk` `26`, `targetSdk` `35`.

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
- assembleRelease / bundleRelease: pass;
- signed workflow: pass;
- physical signed APK install: pass;
- physical launch: pass;
- package metadata on device: pass;
- UI privacy scan across Home / Profiles / Import / Settings dumps:
  pass, with zero privacy hits for raw URI, UUID, IP, subscription URL,
  token, password, or long base64 payload;
- no-profile connect path: pass_limited; no fake Connected state and no
  stuck Preparing state were observed;
- signed RC1 real-profile smoke: not completed because the installed
  signed APK had no locally imported saved profiles during this run;
- internet through VPN on signed RC1: not_verified;
- connect / disconnect / reconnect with a real profile on signed RC1:
  not_verified;
- crash/ANR markers: pass, zero markers in the checked logcat window;
- diagnostics redaction: pass_limited, clipboard readback unavailable;
- TalkBack/accessibility: pass_limited;
- UDP: pass_limited, from previous validation;
- IPv6: not_tested, no clean external IPv6 baseline;
- Google Play: not published.

Known limitations:

- diagnostics clipboard/export readback not fully confirmed;
- full TalkBack audio QA not fully completed;
- real-profile signed RC1 connect/disconnect/reconnect still required;
- signed RC1 internet-through-VPN still required;
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
- The signed workflow prepared artifacts only; it did not create a tag,
  GitHub Release, release assets, or Google Play publication.
- No APK/AAB, raw diagnostics, raw logs, screenshots, private profiles,
  subscription URLs, endpoint/IP/host/password/token values are committed.

## Approval phrase

Do not run a signed RC workflow without the exact approval phrase:

`APPROVE PREPARE SIGNED RC android-v1.1.0-rc.1 WITH DIAGNOSTICS_TALKBACK_UDP_IPV6_LIMITATIONS_ACCEPTED`

Even after that phrase, the next step is only signed workflow execution
and artifact verification. Creating the `android-v1.1.0-rc.1` tag or a
GitHub Release requires a separate approval after artifacts are verified.

## Remaining physical validation gate

Before approving an RC tag without additional limitation acceptance,
install the signed APK, import an approved profile locally on the
physical device without committing or printing profile data, and verify:

- real-profile connect;
- internet through VPN;
- disconnect;
- reconnect;
- no fake Connected state;
- no stuck Preparing state;
- no visible raw URI, UUID, IP, subscription URL, token, password, or
  base64 payload in ordinary UI dumps;
- zero GMvpn crash/ANR markers.
