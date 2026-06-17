# Agent handoff

Этот файл - оперативная сводка для следующего Codex/agent прохода.
Постоянные правила проекта находятся в `AGENTS.md`.

## Current public tester build

- Current public tester build: `android-v1.0.0-rc.5`.
- GitHub Pre-release:
  `https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.5`.
- APK asset: `GMvpn-android-v1.0.0-rc.5.apk`.
- APK SHA-256:
  `ae2ed403818039d90d8f926d9bd8baaa1815e21e10676e9147fcdb509f2c01c8`.
- Testers should download the APK asset only, not Source code zip/tar.gz.

## Release history short

Created:

- RC1 tag.
- RC3 tag and GitHub Pre-release.
- RC4 tag and GitHub Pre-release.
- RC5 tag and GitHub Pre-release.

Not created:

- `android-v1.0.0` tag.
- Production/latest GitHub Release.
- Google Play release.

Do not move RC3/RC4/RC5 tags and do not replace APK assets in existing
GitHub Releases.

## Current branch

Main product development branch:

- `codex/p1-play-compliance-and-device-validation`.

Default branch `main` now has README tester instructions pointing to
RC5 after PR #9 was merged.

## What is done

RC5 includes:

- profile management;
- safe profile names;
- active profile selection;
- rename;
- delete confirmation;
- active-profile reset;
- safe import preview;
- duplicate count;
- redacted diagnostics;
- network validation bench docs with redacted evidence templates.

RC5 verification summary:

- signed workflow run: `27679203026`;
- artifact source SHA:
  `15d0a7f5fd691f9bf517a05ac867fc661be8c233`;
- versionCode: `1000005`;
- versionName: `1.0.0-rc.5`;
- package: `com.gmvpn.client`;
- minSdk: `26`;
- targetSdk: `35`;
- checksums: pass;
- APK signature: pass;
- AAB verification: pass;
- bundletool validate: pass;
- 16 KB ELF alignment: pass;
- APK `zipalign -P 16`: pass;
- physical install/launch: pass;
- crash/ANR scan: pass.

Post-RC5 source hardening:

- diagnostics redaction now collapses profile URIs without host/port;
- diagnostics redaction also masks HTTP URLs, IPv4 addresses, and
  host/destination context in free-form text;
- local unit tests, `lintDebug`, and `assembleDebug` passed;
- public RC5 APK/release assets were not changed.

## Known limitations

Unrestricted `v1.0.0` remains blocked by:

- UDP/iperf: `pass-limited`;
- IPv6: not tested.

DNS is `pass` for the tested device/network, but do not generalize that
claim beyond the recorded RC5 Android-side evidence. Do not claim
production readiness until the remaining gaps are closed or limitations
are explicitly accepted for a limited/MVP release.

Network evidence plan:

- runbook: `docs/android-network-validation-bench.md`;
- updated on 2026-06-17;
- controlled UDP/iperf requires an approved redacted endpoint;
- full DNS requires two independent methods while VPN is connected;
- IPv6 requires a real external IPv6 network or fail-closed evidence;
- raw logs, endpoints, screenshots, profiles, and packet captures stay
  out of git.

Latest preflight:

- date: 2026-06-17;
- GitHub issues: 0 open;
- script: `scripts/validation/preflight-windows.ps1`;
- runner: `scripts/validation/run-network-validation-windows.ps1`;
- local `adb`: found through standard Android SDK platform-tools;
- authorized physical device: present in the latest run, serial masked in
  console output;
- approved iperf endpoint variables: present locally during the latest
  validation attempt, values not printed or committed;
- local `iperf3`: available through trusted WinGet user portable install
  or PATH lookup;
- controlled VPS endpoint: configured with `iperf3-gmvpn.service`,
  TCP/UDP 5201 firewall rules, SSH key access, and rotated root
  password;
- Windows endpoint TCP/UDP connectivity: pass, endpoint redacted. Latest
  runner captured a 30-second 5M UDP run with 0% loss and 4.249 ms
  jitter, classified as endpoint-only evidence;
- Android GMvpn VPN-path UDP: `pass-limited`. Termux was installed from
  the official `termux/termux-app` GitHub pre-release `v0.119.0-beta.3`
  with local SHA-256 verification, `iperf3` 3.21 was installed inside
  Termux, an approved subscription was imported into GMvpn RC5, and a
  controlled UDP matrix ran through active GMvpn on TECNO LG8n Android
  12/API 31. Endpoint, profile, subscription, raw IP, and raw command
  details were not committed. Best stable row: 5M, payload 1200 bytes,
  three 30-second runs, max packet loss 0.096%, max jitter 2.477 ms,
  GMvpn connected before/after every run. A post-matrix logcat tail scan
  found no case-sensitive GMvpn crash/ANR markers. Keep `pass-limited`
  because no formal release loss threshold is approved and 2M had a
  high-loss outlier, later reproduced once in a 5-run 2M rerun. Current
  release interpretation is `UDP functional validation: pass-limited` and
  `UDP performance validation: needs threshold/review`; unrestricted
  v1.0.0 stays blocked until a maintainer-approved threshold is accepted
  or extra stable validation is captured;
- DNS: `pass` for the tested device/network. BrowserLeaks DNS in Android
  Chrome and a Termux `dig` controlled resolver query both ran while
  GMvpn stayed connected, recorded provider/country-level evidence only,
  and found no private/router DNS;
- IPv6: `not_tested`. A disconnect/baseline/reconnect smoke found no
  real external IPv6 baseline on the current network, so IPv6 cannot be
  marked `pass` or `fail_closed`. A later force-stop baseline check did
  produce a clean pre-VPN shell baseline with `tun0` absent, but the
  tested network still had no external IPv6 route or IPv6 probe success.
  A follow-up network search tried current network, Wi-Fi-only,
  mobile-data-only, and Wi-Fi+mobile modes via adb. No external IPv6
  baseline appeared; after restoring radios, Android reported a validated
  network and IPv4 worked, but IPv6 probes still failed. Re-test on LTE/5G,
  another Wi-Fi, or another network where the Android device has external
  IPv6 before GMvpn is enabled;
- RC5 stability smoke: pass-limited. Force-stop baseline and restore were
  exercised, `tun0` returned after restore, no case-sensitive GMvpn
  crash/ANR markers were found, and the local diagnostics bundle was not
  committed. The adb diagnostics bundle still requires manual review
  before sharing because dumpsys/logcat can contain IP/host-like local
  data;
- no raw logs, profiles, endpoints, APK/AAB files, or `.local/`
  artifacts were committed.

## Next product sprint

Recommended order:

1. Collect RC5 tester feedback.
2. Triage new issues with privacy-sensitive rules.
3. Define an approved UDP release threshold or rerun controlled UDP/iperf
   on another network window using only approved endpoints.
4. Optionally repeat full DNS leak audit on another network before final
   release, keeping only redacted provider/country summaries.
5. Run real IPv6 validation or prove fail-closed behavior.
6. Improve stability:
   - app restart while connected/disconnected;
   - reconnect edge cases;
   - invalid subscription handling;
   - no-profile state;
   - diagnostics copy/export UX.
7. Harden diagnostics with more sensitive-pattern unit tests.
8. Decide whether RC6 is required.

## Release rules

Do not create `android-v1.0.0` without explicit approval.

Do not create a new RC without:

- version bump;
- signed workflow;
- checksum verification;
- APK signature verification;
- AAB verification if produced;
- 16 KB ELF verification;
- APK `zipalign -P 16`;
- physical smoke;
- release notes;
- explicit tag/release approval.

For tester GitHub Pre-releases, upload only:

- signed APK;
- SHA-256 checksum.

AAB is not uploaded for normal testers unless separately approved.

## Last known safe next step

Start by deciding whether the current Android-side UDP matrix is enough
for MVP/internal scope or whether to rerun with an agreed packet-loss
threshold and another network window. Then run a full DNS leak audit with
browser evidence and provider/country-level redacted summary, and run
real IPv6 pass/fail-closed checks before any unrestricted `v1.0.0`
decision.
