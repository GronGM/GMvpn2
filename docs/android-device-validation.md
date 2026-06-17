# Android device validation

This runbook validates the Android v1 candidate on a real Android
phone. It is intentionally manual: emulator smoke tests can catch
crashes and wiring mistakes, but they do not prove that a VPN tunnel
is safe to ship.

Do not claim production or public-distribution readiness solely from
this runbook; the checklist in `docs/android-v1-validation-checklist.md`
must carry the matching release-readiness audit state.

## Latest physical-device snapshot

2026-06-15, TECNO LG8n, Android 12/API 31, debug package
`com.gmvpn.client.debug`:

- Debug APK install, app launch, no-profile path, subscription import,
  About/Xray version, and physical `connectedDebugAndroidTest` passed.
- Real VPN connect reached `Connected` and stayed stable for at least
  60 seconds after fixing the Android `EngineBridge` gomobile class
  lookup for top-level `StatusListener` / `Tunnel` bindings.
- Browser HTTPS worked through the active VPN for `https://example.com`.
- Browser IPv4 egress worked through `https://api.ipify.org`; the
  public IP is redacted in artifacts.
- App-level disconnect, reconnect, and final disconnect passed.
- DNS leak audit passed on the same TECNO/browser path: public/VPN-path
  resolver providers were observed, with no local mobile/Wi-Fi ISP
  resolver in the result set.
- IPv6 was not applicable on this TECNO/network because the baseline had
  no underlying IPv6 default route; while VPN was active, Android
  LinkProperties included `::/0 -> tun0` and browser testing observed no
  public IPv6 fall-through.
- Always-on/block-without-VPN passed on this TECNO build after
  `GmvpnVpnService` handled Android's system `android.net.VpnService`
  start action. With lockdown enabled, HTTPS worked while the VPN was
  active; after force-stopping the VPN app, Chrome could not load a
  unique `example.com` URL and Android returned blocked network state
  instead of allowing direct traffic. Cleanup restored
  `always_on_vpn_app=null` and `always_on_vpn_lockdown=0`.
- Wi-Fi/cellular handover passed on the same TECNO device with active
  cellular data. The tunnel remained Connected while `adb svc wifi
  disable` moved traffic to cellular+VPN and `adb svc wifi enable`
  moved traffic back to Wi-Fi+VPN. HTTPS and IPv4 browser egress worked
  after each transition, DNS sanity still used the VPN DNS addresses,
  and post-handover disconnect/reconnect worked with a final clean
  disconnect.
- UDP-heavy fallback validation passed with a limitation: no controlled
  UDP/iperf target was present in ignored local config or environment,
  so Chrome WebRTC/STUN plus a 5-minute YouTube browser playback window
  was used. VPN stayed Connected each minute, post-load HTTPS/IPv4/DNS
  checks passed, and post-load disconnect/reconnect/final disconnect
  passed. This does not measure controlled UDP throughput or loss.
- P0 physical validation evidence is complete on this TECNO run. The
  follow-up release-readiness audit passed as a release candidate state
  after fixing narrow R8/JNA release-shrinker rules and a lint-only
  foreground-service permission warning on the VPN service. This is not
  a production/public distribution claim; signed distribution still goes
  through the release workflow and repository signing secrets.

Redacted local evidence is under
`artifacts/android-diagnostics/tun-lifecycle-fixed-20260615-201047/`.
The adb diagnostics bundle for the same device run is under
`artifacts/android-diagnostics/20260615-171555Z/`.
DNS/IPv6 audit evidence is under
`artifacts/android-diagnostics/dns-ipv6-audit-20260615-202413/`.
Always-on/block-without-VPN evidence is under
`artifacts/android-diagnostics/always-on-killswitch-20260615-204557/`.
Wi-Fi/cellular handover evidence is under
`artifacts/android-diagnostics/network-handover-20260615-212318/`.
UDP-heavy fallback evidence is under
`artifacts/android-diagnostics/udp-heavy-20260615-215101/`.
The latest adb diagnostics bundle is under
`artifacts/android-diagnostics/20260615-191157Z/`.
The release-readiness audit was completed on 2026-06-15; local debug,
physical connected, lintDebug, release APK, and release bundle builds
passed after the R8 rule fix in `clients/android/app/proguard-rules.pro`
and the manifest lint suppression for the VPN service foreground type.

## Prerequisites

- A physical Android 10+ device with Developer options enabled.
- USB debugging enabled and authorized for the workstation.
- Android platform-tools available in `PATH` (`adb version` works).
- JDK 17+ and Android SDK platform 35/build-tools 35.0.0.
- Go, gomobile, Rust, cargo-ndk, Android NDK r28+, and the Rust
  Android targets listed in `clients/android/README.md`.
- One known-good VLESS+Reality test profile controlled by the tester.
  Use a throwaway account and redact all credentials before sharing
  logs.

## Build native artifacts

From the repository root:

```sh
./scripts/build-android-libs.sh
```

Wire the outputs into the Android app:

```sh
mkdir -p clients/android/app/libs
mkdir -p clients/android/app/src/main/jniLibs
cp core/build/gmvpn.aar clients/android/app/libs/
cp -R shared/target/android/jniLibs/* clients/android/app/src/main/jniLibs/
cp shared/target/android/kotlin/uniffi/gmvpn_ffi/gmvpn_ffi.kt \
  clients/android/app/src/main/kotlin/uniffi/gmvpn_ffi/
```

## Build and install debug APK

```sh
cd clients/android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug package name is `com.gmvpn.client.debug`. Release-shaped
local builds use `com.gmvpn.client`.

## Current RC5 validation target

RC5 source metadata is prepared as `versionCode` `1000005` and
`versionName` `1.0.0-rc.5` after the profile/import/diagnostics UX
sprint. RC5 keeps the RC4 saved-profile privacy fix: saved profile
labels must not expose server IPs, hostnames/domains, ports, UUIDs,
passwords, raw URIs, query-like secrets, or base64 payloads. Safe
human-readable fragments and safe `vmess.ps` names may be shown;
otherwise the UI falls back to generic labels such as `VLESS профиль`,
`VMess профиль`, `Trojan профиль`, `Shadowsocks профиль`, or
`Профиль N`. The secondary profile row may show only the protocol type
plus latency.

RC5 also validates profile management and diagnostics UX:

- profile list/details use safe display names only;
- rename persists a safe custom display name;
- delete requires confirmation;
- deleting the active profile resets the active profile to another
  saved profile, or to no active profile if the library becomes empty;
- import preview avoids endpoint, raw URI, UUID, password, token, and
  query-like labels;
- diagnostics remain redacted and do not include raw profiles, raw
  logcat, endpoints, credentials, subscription URLs, or private keys.

RC5 is published as a GitHub Pre-release APK for testers:

- tag: `android-v1.0.0-rc.5`;
- APK: `GMvpn-android-v1.0.0-rc.5.apk`;
- AAB is not uploaded for normal testers;
- `android-v1.0.0` is not created;
- production/latest GitHub Release is not created;
- Google Play is not published.

Post-RC5 source hardening tightened diagnostics redaction, but public
RC5 APK assets were not replaced. Shipping that source change to testers
requires a later version bump, signed workflow, artifact checks,
physical smoke, release notes, and explicit RC approval.

Required signed RC5 physical validation:

- Install the signed RC5 APK on a physical Android device.
- Confirm the app launches without crash/ANR.
- Confirm saved profile list/details do not show endpoint data: no
  IP/host/domain/port, UUID, password, raw URI, or base64.
- Confirm rename and delete-confirmation paths work without leaking
  private profile data.
- Confirm deleting the active profile leaves a truthful active/idle
  state and does not fake a VPN connection.
- Confirm diagnostics export/copy contains only redacted summary data.
- Confirm approved real-profile connect, disconnect, and reconnect still
  work if an approved profile is available.
- Run a log privacy scan without committing raw logs or private
  profiles.

Debug APK install/launch and unit tests cover the formatter and profile
store logic. Manual synthetic UI validation is limited when the physical
device contains encrypted real profiles: do not clear or modify real app
data, and do not dump screenshots/UI if private profiles may be visible
unless the tester explicitly approves that reset or uses a clean test
install.

Known release limitations for RC5 are now: full DNS is `pass` for the
tested device/network, controlled Android-side UDP/iperf is
`pass-limited`, and real external IPv6 is not tested. RC5 is a test
candidate, not production. The practical bench for closing remaining
gaps is defined in `docs/android-network-validation-bench.md`; do not
mark UDP or IPv6 as unrestricted `pass` until that evidence exists.

## Post-RC5 network and stability preflight

2026-06-17 follow-up checks added repeatable Windows preflight tooling
and produced limited local evidence:

- GitHub issues: none open, so no triage labels were applied.
- Preflight script: `scripts/validation/preflight-windows.ps1`.
- Network runner: `scripts/validation/run-network-validation-windows.ps1`.
- ADB: found through the standard Android SDK platform-tools path.
- Authorized physical device: present in the latest run; console output
  masks the serial.
- Controlled endpoint: configured on a VPS with `iperf3-gmvpn.service`,
  TCP/UDP 5201 firewall rules, SSH key access, and a rotated root
  password. Endpoint values were kept only in ignored local env/files.
- Endpoint connectivity: Windows to VPS TCP and UDP checks passed with
  endpoint details redacted. The latest runner recorded a 30-second UDP
  run at 5M with 0% packet loss and 4.249 ms jitter. This is endpoint
  readiness evidence only, not Android GMvpn VPN-path UDP evidence.
- Controlled UDP/iperf over Android GMvpn path: `pass-limited`. Termux
  was installed from the official `termux/termux-app` GitHub pre-release
  `v0.119.0-beta.3`, APK SHA-256 was verified locally, and Termux
  `iperf3` 3.21 ran over active GMvpn RC5 on the physical TECNO LG8n.
  Payload was 1200 bytes, duration was 30 seconds per run, and endpoint
  details were redacted. Matrix: 1M x3 had 0% loss; 2M x3 had one
  high-loss outlier with 0% / 14.333% / 43% min/avg/max loss; 3M x3 had
  0% / 0.004% / 0.011%; 5M x3 had 0% / 0.041% / 0.096%. Best stable
  result was 5M with max 0.096% loss and max 2.477 ms jitter. GMvpn
  stayed connected before and after each run. A post-matrix logcat tail
  scan with case-sensitive GMvpn crash/ANR markers found no GMvpn crash
  or ANR. A 5-run 2M rerun reproduced one high-loss outlier with
  0 / 6.803 / 34% min/avg/max loss and 0.940 / 4.766 / 11.132 ms
  min/avg/max jitter. Keep status `pass-limited` because no formal
  release loss threshold is approved and the 2M anomaly reproduced.
- Full DNS leak audit: `pass` for this device/network. A 2026-06-17
  Android-side follow-up used BrowserLeaks DNS in Android Chrome plus a
  Termux `dig` controlled resolver query while GMvpn stayed connected.
  The recorded summary is provider/country-level only, found no
  private/router DNS, and committed no raw IPs or screenshots.
- IPv6: not tested. A disconnect/baseline/reconnect smoke found no real
  external IPv6 baseline on the current device/network. During VPN there
  was also no global IPv6/default-route/ping evidence, but without a
  baseline this cannot be marked `pass` or `fail-closed`.
- RC5 stability smoke: pass-limited. The latest runner confirmed
  disconnect, reconnect, restored `tun0`, and no case-sensitive GMvpn
  crash/ANR markers. A local adb diagnostics bundle was generated and
  ignored, but it still contained IP/host-like local data, so it must be
  reviewed before sharing and is not public-safe raw evidence.
- Evidence handling: no raw logs, diagnostics, screenshots, VPN
  profiles, subscription URLs, endpoints, APK/AAB files, `.local/`, or
  private artifacts were committed.

Historical RC5 approval phrase already used for the published
Pre-release:

```text
APPROVE RC TAG android-v1.0.0-rc.5 ON 15d0a7f5fd691f9bf517a05ac867fc661be8c233
```

Do not reuse that approval for RC6 or final `android-v1.0.0`.

## Published RC4 reference build

RC4 source metadata is `versionCode` `1000004` and `versionName`
`1.0.0-rc.4` for the saved-profile privacy fix. Annotated tag
`android-v1.0.0-rc.4` points to artifact source commit
`1b99d5abc1a693584519eb201c49c466ca13a782`, and GitHub Pre-release
`https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.4`
is published for manual APK testing with only the APK and SHA-256
checksum assets. RC4 is not production, is not Google Play publication,
and does not create `android-v1.0.0`.

## Signed RC3 validation target

RC3 source metadata is prepared as `versionCode` `1000003` and
`versionName` `1.0.0-rc.3` for the VPN permission cancel and
invalid-profile UX fixes. Signed RC3 artifacts were produced by
workflow run `27643689894` from commit
`dd10df9d3683fa41ccc628e5db0c186d029dd6ae`. Annotated tag
`android-v1.0.0-rc.3` was created and pushed after explicit approval;
GitHub Release and `android-v1.0.0` were not created.

Use only the downloaded signed RC3 APK from the ignored local artifact
directory for the next physical release validation:

```text
.local/release-artifacts/android-v1.0.0-rc.3/
  gmvpn-android-android-v1.0.0-rc.3-signed/
    outputs/apk/release/app-release.apk
```

Local verification passed signed checksums, APK v2 signature, AAB
`jarsigner` verification with expected RC certificate warnings,
signed APK/AAB 16 KB ELF alignment, signed APK `zipalign -P 16`, and
APK metadata `versionCode` `1000003`, `versionName` `1.0.0-rc.3`,
`minSdk` 26, `targetSdk` 35. Do not reuse the failed RC2 APK for
release approval.

## Signed RC3 physical-device validation

2026-06-16, physical TECNO LG8n, Android 12/API 31, release package
`com.gmvpn.client`:

- `adb devices -l` showed the phone as `device`.
- `adb install -r` of the signed RC3 APK succeeded.
- Package metadata showed `versionCode` `1000003`, `versionName`
  `1.0.0-rc.3`, `minSdk` 26, and `targetSdk` 35.
- No emulator was started or used.
- `adb shell svc power stayon usb` was enabled for the run.
- A previous dummy validation profile was wiped when the app was
  uninstalled/reinstalled to reset Android VPN consent. No real
  profile had been entered at that point.
- Android Settings -> VPN -> GMvpn -> Forget VPN was used to reset VPN
  consent before the permission cancel and allow checks.
- VPN permission cancel passed: the Android VPN permission dialog
  appeared, tapping Cancel returned the UI to Disconnected/Connect, no
  `GmvpnVpnService` start was logged, no fake Connected state appeared,
  and the UI did not remain stuck in Preparing.
- Invalid-profile UX passed: a non-secret dummy `https` profile failed
  safely with `profile URI: unsupported protocol: https`, no fake
  Connected state appeared, the error card remained visible after the
  service returned to Idle, and tapping Dismiss removed it.
- A redacted approved subscription URL from ignored
  `.local/test-profile.txt` decoded successfully to 4 profiles with 0
  skipped entries. The raw URL and profile contents were not printed or
  committed.
- VPN permission allow passed with the real profile: the Android VPN
  permission dialog appeared, tapping OK started the service, and the
  UI reached Connected/Disconnect without error.
- Tunnel lifecycle passed: connect, disconnect, and reconnect were run
  three times from the UI with repeated Connected/Idle transitions and
  no Error transitions.
- App restart checks passed: Home -> relaunch while connected preserved
  Connected/Disconnect, and Home -> relaunch while disconnected
  preserved Disconnected/Connect.
- Basic browsing passed: `https://example.com` loaded in the device
  browser while the VPN was connected.
- IPv4 route passed: browser-based Cloudflare trace changed from a RU
  baseline to an NL VPN exit during the tunnel. Raw IPs were stored only
  in ignored local evidence.
- DNS evidence is pass-limited: a browser-based DNS leak page loaded
  during the tunnel, the parsed dump showed NL resolver evidence and no
  local ISP/router markers, but this is not a full lab DNS audit.
- Network change passed-limited: Wi-Fi was disabled briefly and then
  restored while the tunnel was connected; the app stayed error-free
  and no GMvpn crash/ANR was found. This does not replace a long-run
  roaming stability test.
- UDP/iperf was not tested because no controlled iperf3 endpoint was
  provided.
- IPv6 was not passed: the browser trace used IPv4 before and during
  VPN, so there was no verified real external IPv6 baseline for leak
  testing.
- Raw logcat, connectivity dumps, and UI dumps stayed under ignored
  `.local/device-validation/rc3/` and were not committed.
- Log privacy scan found no private keys, VPN URIs, UUIDs, password,
  token, Authorization, Cookie, X-Api-Key, `pbk`, `sid`, or `spx`
  patterns in GMvpn-related log lines. Whole-log matches were from
  unrelated system/app lines and remained local only.
- Crash scan found no `FATAL EXCEPTION`, `AndroidRuntime` crash, or ANR
  for `com.gmvpn.client`.

RC3 physical validation is pass-limited: the release-blocking
permission cancel and invalid-profile UX regressions are fixed on the
physical device, and the real profile path connects, browses,
disconnects, and reconnects. Remaining evidence gaps are controlled
UDP/iperf, a full DNS leak audit, and real external IPv6 validation.

## RC3 tag traceability

This is a traceability package only. It records the approved RC3 tag
that was created after review. It does not authorize a GitHub Release
or a final `android-v1.0.0` tag.

- Candidate: `android-v1.0.0-rc.3`.
- Tag object SHA:
  `65f3f0bd0d99a284291f178e4ac326300dc8d353`.
- Artifact source SHA:
  `dd10df9d3683fa41ccc628e5db0c186d029dd6ae`.
- Validation docs HEAD:
  `b129c93ff65564da9543a7350779d0af70daf068`.
- Signed workflow run:
  `https://github.com/GronGM/GMvpn2/actions/runs/27643689894`.
- Signed APK/AAB: yes.
- SDK 35: yes (`compileSdk` 35 / `targetSdk` 35 / `minSdk` 26).
- 16 KB native readiness: yes.
- Physical Android install/connect/disconnect/reconnect: yes.
- DNS: pass-limited.
- IPv4 route: pass.
- UDP/iperf: not tested.
- IPv6: not tested.
- Log privacy: pass.
- GitHub Release: not authorized.
- RC3 tag: created and pushed.
- Final `android-v1.0.0` tag: not authorized.

If the existing signed RC3 artifacts from workflow run `27643689894`
are used, the tag `android-v1.0.0-rc.3` points to
`dd10df9d3683fa41ccc628e5db0c186d029dd6ae`. Do not tag validation or
documentation commits unless a new signed workflow is rerun on that
exact commit.

Approval phrase used before creating the tag:

```text
APPROVE RC TAG android-v1.0.0-rc.3 ON dd10df9d3683fa41ccc628e5db0c186d029dd6ae WITH UDP_IPV6_LIMITATIONS_ACCEPTED
```

## Post-RC3 network validation status for v1.0.0

2026-06-16 follow-up checks did not add stronger network evidence:

- Controlled UDP/iperf: blocked. The current workstation environment
  has no `GMVPN_IPERF_*` / `IPERF3_*` endpoint variables and no approved
  controlled endpoint was provided. Local `iperf3` tooling is now
  available through the Windows preflight scripts.
  Do not mark UDP as pass until a redacted iperf3 result records
  command shape, duration, target bitrate, jitter, packet loss,
  pass/fail, and whether GMvpn stayed connected.
- Full DNS leak audit: still pass-limited for signed RC3. The physical
  RC3 run had browser-level evidence with no local ISP/router markers,
  but this follow-up did not run two fresh independent DNS methods while
  the signed RC3 VPN was known to be connected.
- Real IPv6: not tested. No real external IPv6 baseline was established
  in the signed RC3 follow-up. A v1.0.0 release decision must either
  accept this limitation explicitly or block until an IPv6-capable
  network proves tunneled IPv6 or fail-closed behavior with no local
  IPv6 leak.
- Evidence handling: no raw logcat, screenshots, VPN profiles,
  subscription URLs, server hostnames/IPs, APK/AAB files, `.local/`
  files, or diagnostics artifacts are committed.
- CI proof: Android release workflow run `27648312721` succeeded from
  `5a7aca93e34dac3aa606711806669af75a99d067` after the Node 24 action
  ref updates, with no remaining Node 20 deprecation annotation or log
  match. This does not approve `android-v1.0.0`.
- Release gate: strict v1.0.0 requires
  `APPROVE UNRESTRICTED V1.0.0 AFTER UDP_DNS_IPV6_PASS`; MVP/limited
  v1.0.0 requires
  `APPROVE MVP V1.0.0 WITH UDP_DNS_IPV6_LIMITATIONS_ACCEPTED` and a
  final signed `1.0.0` workflow from the exact release source SHA before
  any GitHub Release.
- 2026-06-17 strict-path attempt: the physical TECNO LG8n was visible
  over ADB and `com.gmvpn.client` was installed as `versionCode`
  `1000003`, `versionName` `1.0.0-rc.3`, `targetSdk` 35. No approved
  controlled UDP endpoint was available. A
  sanitized connectivity check did not observe an active VPN Internet
  network for a fresh DNS audit, and a sanitized route check did not
  observe IPv6. No raw connectivity dumps, raw IPs, logs, profiles,
  subscriptions, `.local/`, APK/AAB, or diagnostics artifacts were
  committed.
- Final v1.0.0 preparation is plan-only until a release path is chosen.
  Because RC5 uses `versionCode` `1000005`, the final Android build must
  use a later `versionCode` and `versionName` `1.0.0`, then run
  `android-release.yml` with `rc_tag=android-v1.0.0` and
  `version_name=1.0.0`, verify checksums, APK signature, AAB, 16 KB ELF
  alignment, `zipalign -P 16`, and APK metadata before any final tag or
  GitHub Release.

## Historical signed RC2 candidate artifact

The signed RC2 candidate evidence artifact is local-only and ignored:

```text
.local/release-artifacts/android-v1.0.0-rc.2/
  gmvpn-android-android-v1.0.0-rc.2-signed/
    outputs/apk/release/app-release.apk
```

Use it for the next physical signed APK validation only after confirming
the file came from workflow run `27640095772`. A physical signed RC2
attempt was performed on 2026-06-16 and did not pass; RC2 tag/release
is not approved.

## Signed RC2 physical-device attempt

2026-06-16, physical Android 12/API 31 device, release package
`com.gmvpn.client`:

- `adb install -r` of the signed RC2 APK succeeded.
- Package metadata stayed at `versionCode` `1000002` and
  `versionName` `1.0.0-rc.2`.
- Fresh launch reached `MainActivity` without crash or ANR.
- The clean no-profile state was understandable: status was
  disconnected, the profile editor explained that no profile was saved,
  and Connect was disabled until a profile was present.
- About opened without crashing and showed app `1.0.0-rc.2`, core
  `0.0.1`, and `Xray-core 26.3.27`.
- Saving a non-secret dummy invalid profile enabled Connect and caused
  Android's system VPN permission dialog to appear for GMvpn.
- Cancelling the VPN permission dialog did not crash, but left the UI
  stuck at `Preparing` with `Disconnect` visible after waiting. This is
  a release-blocking state bug.
- Later invalid-profile start attempts failed safely in
  `GmvpnVpnService`: logs showed unsupported protocol, cleanup after
  failure, foreground-service removal, and no `Connected` state.
- The invalid-profile error was not persistently visible in the final
  captured UI, so invalid-profile UX is not a pass.
- ADB direct service start was denied because the VPN service is not
  exported.
- Post-test cleanup ran `adb shell pm clear com.gmvpn.client` to remove
  the dummy profile from app data.
- Raw logcat and UI dumps stayed under ignored `.local/device-validation/`.
  They were not committed.
- Crash scan found no `FATAL EXCEPTION`, `AndroidRuntime` crash, or
  ANR for `com.gmvpn.client`.
- Privacy scan found no private key blocks or VPN profile URI tokens in
  raw logcat, and no UUID/password/Auth/Cookie/X-Api-Key patterns in
  GMvpn-related lines. One Android BackupManager restore token appeared
  in a system line; it was not a GMvpn VPN credential.

Signed RC2 tunnel lifecycle, HTTPS through tunnel, IPv4 route, DNS
leak, controlled UDP/iperf, and real IPv6 validation remain pending
because no approved real VPN profile/server was used and the app did
not reach a validated Connected state.

## Run emulator smoke tests

These tests are a scaffold for lifecycle and availability checks. They
do not prove a real tunnel works.

```sh
cd clients/android
./gradlew :app:connectedDebugAndroidTest
```

Expected coverage:

- `VpnService` manifest is private and protected by `BIND_VPN_SERVICE`.
- `VpnService.prepare` returns either a system permission intent or
  confirms permission is already granted.
- `EngineBridge.xrayVersionOrNull()` is non-empty when `gmvpn.aar` is
  bundled.
- Missing `gmvpn.aar` surfaces as `EngineUnavailableException`.
- Start without an active profile reaches a user-visible error.
- Stop while idle does not crash.

## Import redacted VLESS+Reality profile

Use a test profile in this shape, replacing placeholders locally only:

```text
vless://<uuid>@<server-host>:443?type=tcp&security=reality&sni=<server-name>&fp=chrome&pbk=<public-key>&sid=<short-id>&flow=xtls-rprx-vision#GMvpn-device-test
```

Before sharing any screenshot or log, redact:

- UUID.
- Server hostname or IP address.
- Reality `pbk`, `sid`, and `spx`.
- Subscription URLs and account names.

## Basic connect / browse / disconnect

1. Open GMvpn.
2. Save the redacted test profile locally with the real credentials.
3. Tap Connect and approve the Android VPN permission prompt.
4. Confirm the status reaches Connected.
5. Open a browser and load at least two HTTPS sites.
6. Return to GMvpn and tap Disconnect.
7. Confirm Android no longer shows GMvpn as the active VPN.

Fail if the app crashes, stays stuck in Starting/Reconnecting, reports
Connected without traffic, or leaves Android showing an active VPN
after Disconnect.

## IPv4 connectivity

With the tunnel connected:

```sh
adb shell ping -4 -c 4 <REDACTED_IP>
adb shell toybox wget -qO- https://api.ipify.org
```

Record whether the observed public IPv4 is the expected VPN egress.
Do not paste the exact IP into public logs unless it is safe to share.

## IPv6 behavior

With the tunnel connected:

```sh
adb shell ping -6 -c 4 2606:4700:4700::1111
```

Then open `https://test-ipv6.com/` in the device browser. Pass only if
IPv6 is either tunneled through the expected VPN egress or explicitly
blocked without falling back to the raw network. Any silent raw IPv6
egress is a P0 fail.

## DNS leak audit

With the tunnel connected:

1. Open `https://www.dnsleaktest.com/` and run the extended test.
2. Run a manual resolver check:

   ```sh
   adb shell nslookup example.com
   ```

Pass only if DNS resolvers belong to the expected VPN path. Any ISP,
carrier, hotel Wi-Fi, or local-network resolver is a P0 fail.

## Kill-switch / always-on audit

1. Open Android VPN settings from GMvpn's always-on card.
2. Enable Always-on VPN for GMvpn.
3. Enable Block connections without VPN.
4. Connect GMvpn and verify browsing works.
5. Force a tunnel interruption by stopping the app/tunnel or by
   toggling Airplane mode or Wi-Fi/cellular briefly.
6. While disconnected or reconnecting, verify the browser cannot reach
   the internet outside the tunnel.

Pass only if traffic is blocked while the VPN is down and the device is
restored to a working network state after the test. This result is
device- and OS-version-specific; capture the Android version.

## Reconnect on network change

1. Connect on Wi-Fi.
2. Start a continuous HTTPS download or video stream.
3. Disable Wi-Fi so the phone moves to cellular.
4. Re-enable Wi-Fi and let the phone move back.

Pass if GMvpn enters Reconnecting and returns to Connected without a
process crash. Re-run the DNS and IPv6 leak checks after reconnection.

## UDP-heavy traffic

With the tunnel connected:

- Run the DNS leak audit.
- Watch a 5-minute video or use an app known to use QUIC/UDP.
- If available, run:

  ```sh
  adb shell ping -c 20 <REDACTED_IP>
  ```

Fail on crashes, stalled browsing, or logcat errors from SOCKS5 UDP
ASSOCIATE handling.

## Export diagnostics

From the app, open About and tap Export diagnostics. Share only after
manual review and redaction.

From the workstation, collect a local adb bundle:

```sh
./scripts/collect-android-diagnostics.sh
```

For release package testing:

```sh
./scripts/collect-android-diagnostics.sh com.gmvpn.client
```

The script writes to `artifacts/android-diagnostics/<timestamp>/`.
Without a device, the script can still be syntax-checked with:

```sh
bash -n scripts/collect-android-diagnostics.sh
```

## Logs to collect

- GMvpn diagnostics export from the app.
- `scripts/collect-android-diagnostics.sh` output directory.
- Android version, device model, network type, and whether Always-on /
  Block-without-VPN were enabled.
- Exact pass/fail notes for IPv4, IPv6, DNS leak, kill-switch, reconnect,
  and UDP-heavy traffic.

## Redaction before sharing

Before sending logs to another person or attaching them to an issue,
search for and remove:

- `vless://<redacted> `vmess://<redacted> `trojan://<redacted> `ss://<redacted>
- UUIDs.
- Server hostnames and IP addresses.
- `password`, `token`, `pbk`, `sid`, `spx`.
- `Authorization`, `Cookie`, `X-Api-Key`.
- Subscription URLs, account names, email addresses, and screenshots
  that reveal the provider.

## Pass/fail checklist

- Debug APK installs and opens.
- Native artifacts are bundled; About shows a non-empty Xray-core
  version.
- VPN permission prompt appears only when permission is not already
  granted.
- Connect reaches Connected with a known-good test profile.
- Disconnect tears down Android's active VPN state.
- IPv4 egress uses the VPN.
- IPv6 is tunneled or explicitly blocked.
- DNS does not leak to the raw network.
- Always-on / block-without-VPN blocks traffic while the tunnel is down.
- Reconnect survives Wi-Fi/cellular changes.
- UDP-heavy traffic works without crashes.
- Diagnostics export is redacted and reviewable.
