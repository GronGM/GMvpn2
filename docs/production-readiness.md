# GMvpn Android production readiness

This document tracks the path from GitHub MVP/internal `android-v1.0.0`
testing to broader production readiness. It is a plan only. It does not
approve Google Play publication, production/latest GitHub release,
new tags, or new release assets.

Current public build:

- release: `android-v1.0.0`;
- type: GitHub pre-release, MVP/internal;
- APK: `GMvpn-android-v1.0.0.apk`;
- Google Play: not published;
- unrestricted production: not approved.

## 1. UDP threshold decision

Current status: `pass_limited`.

Known evidence:

- Android-side functional UDP/iperf evidence exists through active GMvpn.
- No formal release threshold or outlier policy is approved.
- At least one 2M UDP outlier was observed during prior validation.

Production readiness requires:

- approved packet loss threshold;
- approved jitter threshold or review rule;
- approved outlier handling policy;
- controlled endpoint outside the phone local network and outside the VPN
  server itself;
- redacted evidence with bitrate, duration, loss, jitter, pass/fail, and
  VPN connected before/after.

Do not use random public iperf servers as release evidence unless that is
explicitly approved for the release decision.

## 2. IPv6 tunneled/fail-closed validation

Current status: `not_tested`.

Production readiness requires a network with real external IPv6 before
GMvpn is enabled. During active VPN, one of these must be true:

- IPv6 routes through the VPN endpoint; or
- IPv6 is blocked/fail-closed with no local IPv6 fallback.

Unacceptable result:

- local ISP IPv6 is visible while VPN is connected.

If no real IPv6 network is available, keep status `not_tested`; do not
mark it pass.

## 3. Full multi-device smoke

Minimum production candidate matrix:

- Android 12/API 31 physical device already covered by TECNO LG8n smoke;
- at least one Android 13 or newer physical device;
- at least one Samsung or Pixel-class device if available;
- fresh install;
- update over previous APK if applicable;
- import subscription;
- profile list privacy;
- active profile select/rename/delete;
- VPN permission allow and cancel;
- connect/disconnect/reconnect;
- app restart while connected;
- network handover if practical;
- diagnostics copy/export;
- crash/ANR scan.

Do not commit raw logs, screenshots, profiles, subscription URLs, IPs,
hostnames, APK/AAB, or `.local/` artifacts.

## 4. Play VpnService declaration

Before any Play submission, prepare a clear declaration for:

- why the app uses `VpnService`;
- user-visible VPN purpose;
- no hidden proxying;
- no undisclosed telemetry;
- no crash analytics unless separately added and disclosed;
- how the user starts/stops the VPN;
- how profile data is stored and redacted.

Play submission is not approved by this document.

## 5. Privacy policy / data handling statement

Production readiness requires a public privacy/data handling statement
that covers:

- no telemetry in the current APK;
- no automatic profile upload;
- local profile storage;
- diagnostics redaction rules;
- what users must not attach to GitHub issues;
- how to report privacy-sensitive bugs safely;
- support contact or issue process.

The statement must not include private server details, subscription URLs,
credentials, or raw endpoints.

## 6. Store listing requirements

Prepare, but do not publish:

- app name and short description;
- full description in Russian first;
- screenshots with no private profile data;
- support URL;
- privacy policy URL;
- category and content rating inputs;
- clear MVP/internal limitation wording if using internal testing first.

Screenshots must be redacted and must not show profile secrets, IPs,
hostnames, subscription URLs, or personal data.

## 7. Support/feedback process

Current public feedback path:

- GitHub issues;
- Android APK bug report form;
- Android tester feedback form;
- labels: `needs-triage`, `privacy-sensitive`, `profile-label`,
  `connectivity`, `vpn-permission`, `profile-import`, `dns`, `udp`,
  `ipv6`, `v1.0.0`.

Triage rules:

- every new issue gets `needs-triage`;
- profile/IP/name/privacy reports get `privacy-sensitive` and
  `profile-label`;
- connectivity reports get `connectivity`;
- import reports get `profile-import`;
- VPN permission reports get `vpn-permission`;
- DNS/UDP/IPv6 reports get their specific labels;
- do not quote secrets in comments;
- ask reporters to edit or delete private data.

## 8. Security review

Production readiness requires review of:

- profile import parsing;
- duplicate import handling;
- saved profile label redaction;
- diagnostics redaction;
- tunnel lifecycle state machine;
- VPN permission cancel path;
- invalid profile error path;
- native artifact missing path;
- release signing and artifact verification;
- dependency/license review for Android release artifacts.

High-risk findings block production until fixed and physically smoke
tested.

## 9. Release signing/key rotation policy

Document before broader rollout:

- where release signing secrets live;
- who can trigger signed workflows;
- how workflow run ID maps to tag target SHA;
- how APK/AAB checksums are recorded;
- when to rotate keys;
- how to revoke or supersede a compromised artifact;
- rule that tags point to artifact source SHA, not later docs commits.

Do not store signing secrets, keystores, passwords, or generated APK/AAB
files in git.

## 10. Upgrade path from GitHub APK to Play/internal tracks

Before Play/internal testing:

- decide whether GitHub APK users can update to Play build with the same
  package and signing key;
- verify versionCode monotonicity;
- test install/update over GitHub APK;
- document migration from `android-v1.0.0` to the next build;
- decide whether GitHub pre-release remains available;
- prepare rollback guidance.

No Play/internal track upload is authorized until a separate explicit
approval is given.
