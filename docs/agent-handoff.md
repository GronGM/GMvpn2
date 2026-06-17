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

- DNS: `pass-limited`;
- UDP/iperf: not tested;
- IPv6: not tested.

Do not claim production readiness until these are closed or limitations
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
- authorized physical device: not present in the latest run; earlier
  runs had a physical device with serial masked in console output;
- approved iperf endpoint variables: present locally during the latest
  validation attempt, values not printed or committed;
- local `iperf3`: available through trusted WinGet user portable install
  or PATH lookup;
- controlled VPS endpoint: configured with `iperf3-gmvpn.service`,
  TCP/UDP 5201 firewall rules, SSH key access, and rotated root
  password;
- Windows endpoint TCP/UDP connectivity: pass, endpoint redacted;
- Android GMvpn VPN-path UDP: blocked by missing authorized ADB device;
- DNS: still `pass-limited`;
- IPv6: not tested;
- RC5 stability smoke: blocked in the latest run because no authorized
  ADB device was present; earlier evidence remains pass-limited only;
- no raw logs, profiles, endpoints, APK/AAB files, or `.local/`
  artifacts were committed.

## Next product sprint

Recommended order:

1. Collect RC5 tester feedback.
2. Triage new issues with privacy-sensitive rules.
3. Run controlled UDP/iperf validation using only approved endpoints.
4. Run full DNS leak audit with at least two independent methods.
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

Start by restoring an authorized physical Android device in ADB, then
set the already prepared controlled endpoint env vars locally without
printing their values and rerun the Windows preflight/runner. After that,
run Android VPN-path UDP, manual full DNS leak audit, and IPv6
pass/fail-closed checks before any `v1.0.0` decision.
