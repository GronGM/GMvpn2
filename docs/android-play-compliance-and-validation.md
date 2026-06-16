# Android Play compliance and post-RC validation

Status: P1 audit plan, not release approval.

Last checked: 2026-06-16.

This document keeps Google Play readiness and post-RC device validation
separate from the `android-v1.0.0-rc.1` tag gate. It must not be used
to claim production readiness.

## Current Android SDK and toolchain audit

Repository values:

- `compileSdk`: 35
- `targetSdk`: 35
- `minSdk`: 26
- Android Gradle Plugin: 8.6.1
- Kotlin: 2.0.21
- Java toolchain target: 17
- Local Windows SDK platforms observed during this audit:
  `android-34`, `android-35`

Policy baseline:

- Google Play's target API requirements currently require new Android
  apps and app updates to target Android 15/API 35 or higher.
- Current `targetSdk = 35` satisfies the current Android 15/API 35
  target API baseline for new Google Play apps and updates.
- Existing RC artifacts remain tied to their original source SHA; the
  SDK 35 migration needs its own signed workflow run before any
  Play-bound artifact is approved.

Official references:

- Target API level requirements:
  `https://support.google.com/googleplay/android-developer/answer/11926878`
- VpnService policy:
  `https://support.google.com/googleplay/android-developer/answer/12564964`
- Android foreground service type requirements:
  `https://developer.android.com/about/versions/14/changes/fgs-types-required`
- Android 15 foreground service timeout behavior:
  `https://developer.android.com/develop/background-work/services/fgs/timeout`
- 16 KB page size support:
  `https://developer.android.com/guide/practices/page-sizes`

## Target SDK 35+ migration result

Done in a dedicated post-RC branch commit; do not retarget the existing
RC artifacts.

Validation result:

- Installed `platforms;android-35` and `build-tools;35.0.0` locally.
- Changed `compileSdk` and `targetSdk` to 35.
- Kept `minSdk = 26`.
- Passed:

  ```powershell
  cd C:\Users\Gron\Documents\gmvpn2\clients\android
  .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace
  ```

Remaining migration gates:

- Re-run `:app:connectedDebugAndroidTest` on an emulator or physical
  device.
- Re-run a physical signed APK smoke test before publishing anything
  through Play.
- Produce a new signed workflow artifact from the SDK 35 source commit
  before treating any artifact as Play-bound.

Reference procedure for future target API bumps:

1. Install Android SDK platform 35+ and matching build tools in the
   local and CI environments.
2. Change `compileSdk` and `targetSdk` together.
3. Keep `minSdk = 26` unless a dependency requires a higher floor.
4. Run:

   ```powershell
   cd C:\Users\Gron\Documents\gmvpn2\clients\android
   .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease --stacktrace
   ```

5. Re-run `:app:connectedDebugAndroidTest` on an emulator or physical
   device.
6. Re-run a physical signed APK smoke test before publishing anything
   through Play.
7. If AGP or Android lint reports a new foreground-service, permission,
   notification, backup, or manifest rule, document the behavior in an
   ADR before changing the app architecture.

TargetSdk 35 risk areas for this app:

- `GmvpnVpnService` currently uses
  `android:foregroundServiceType="systemExempted"` and declares
  `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`. Keep verifying the VPN service
  start path, Always-on VPN, and reconnect handling after the bump.
- Android 15 foreground-service timeouts currently focus on `dataSync`
  and `mediaProcessing`; GMvpn does not use those service types, but
  the release test should still watch logcat during a long tunnel
  session.
- The app ships native libraries. A Play-targeted Android 15+ build
  must verify 16 KB page-size compatibility for every packaged `.so`,
  including Rust UniFFI, gomobile/Xray, JNA, and AndroidX native
  libraries.

## Android 15 foreground-service and VPN audit result

Inspected files:

- `clients/android/app/src/main/AndroidManifest.xml`
- `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/GmvpnVpnService.kt`
- `clients/android/app/src/main/kotlin/com/gmvpn/client/tunnel/TunnelController.kt`

Result:

- `GmvpnVpnService` is the only foreground service in the manifest.
- Service type is `android:foregroundServiceType="systemExempted"`.
- Permissions declared: `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, and `BIND_VPN_SERVICE`.
- The service is `android:exported="false"` and has the
  `android.net.VpnService` intent filter.
- No `dataSync` or `mediaProcessing` foreground service type is used.
- No `BOOT_COMPLETED` receiver or background boot auto-start path was
  found in the Android manifest/source audit.
- `TunnelController` starts the service only from the app/UI flow after
  `VpnService.prepare`; Android Always-on starts through
  `VpnService.SERVICE_INTERFACE`, which `GmvpnVpnService` handles.

Android 15 foreground-service timeout handling for `dataSync` and
`mediaProcessing` is not applicable to the current manifest because
GMvpn does not use those service types. Keep the long-running physical
tunnel smoke test in the Play-bound validation plan because the app is
still a long-lived VPN foreground service.

## Google Play VpnService declaration checklist

Play Console declaration evidence to prepare:

- VPN is the app's core functionality.
- The app creates a user-requested device-level VPN tunnel to a remote
  endpoint configured by the user.
- The Play listing must clearly document the VPN functionality.
- Manifest uses `android.permission.BIND_VPN_SERVICE`.
- `GmvpnVpnService` is `android:exported="false"`.
- The service declares the `android.net.VpnService` intent filter.
- Traffic is not redirected or manipulated for ads, affiliate routing,
  analytics, monetization, or unrelated third-party services.
- The repo has no ad SDK, analytics SDK, crash-reporting SDK, hidden
  telemetry endpoint, or traffic monetization integration in the
  audited Android dependency/config surface.
- The privacy policy says the app contacts only user-configured
  profiles/subscriptions and sends no analytics, crash reports, or
  telemetry.
- Backup and data extraction rules exclude app state and profile
  storage.
- If any future build collects personal or sensitive data through
  VpnService, add an in-app prominent disclosure and affirmative consent
  before Play submission.

Current repo scan notes:

- `PRIVACY.md` and the About screen state that telemetry is absent.
- `clients/android/gradle/libs.versions.toml` and Android Gradle files
  contain no Google/Firebase/ad/analytics/crash-reporting SDKs.
- `SubscriptionFetcher` only fetches a user-supplied HTTPS subscription
  URL on explicit user action.

## Play Console VpnService declaration draft

Use this as a draft only. Do not submit it until product listing text,
privacy policy, and signed-device validation are final.

- Core functionality: GMvpn is a user-facing VPN client.
- VpnService purpose: GMvpn uses Android `VpnService` to create a
  user-requested device-level tunnel and route selected device traffic
  to a VPN endpoint configured by the user.
- User control: users create or import profiles, approve the Android
  VPN permission dialog, and explicitly start/stop the tunnel. Android
  system Always-on VPN can also start the service when the user enables
  that system setting.
- Encryption: profile protocols are VLESS, VMess, Trojan, and
  Shadowsocks as configured by the user; traffic between the device and
  the configured VPN endpoint is protected by the selected protocol.
- Data handling: GMvpn does not add analytics, crash reporting,
  advertising, affiliate routing, or traffic monetization SDKs.
- Traffic handling: GMvpn does not manipulate traffic for ads,
  monetization, unrelated redirects, or third-party collection.
- Sensitive data: profile URIs are encrypted at rest through Android
  Keystore-backed AES-GCM; diagnostics are redacted before sharing.
- Privacy policy: current policy states that the app contacts only
  user-configured profiles/subscriptions and sends no telemetry.
- Pending before submission: final Play listing copy, screenshots, Data
  safety answers, signed-release physical validation, and confirmation
  that no new SDKs were added.

## 16 KB native page-size audit result

Tooling:

- Built post-RC release artifacts after the SDK 35 migration and native
  linker fixes.
- Used Android NDK `28.2.13676358` (`r28c`) and NDK
  `llvm-readelf -l`.
- Used Go `1.26.4`; local `gomobile version` still reports
  `unknown: binary is out of date, re-install it`, but the rebuilt AAR
  was inspected directly.
- Added `scripts/check-android-16kb-elf-alignment.sh` and ran it
  against the release APK and release AAB.
- Treated a native library as 16 KB ready only when every `LOAD`
  segment had `Align >= 0x4000`.

Summary:

- Packaged release `.so` files checked in
  `clients/android/app/build/outputs/apk/release/app-release-unsigned.apk`:
  23.
- Packaged release `.so` files checked in
  `clients/android/app/build/outputs/bundle/release/app-release.aab`:
  23.
- Ready: 23.
- Not ready: 0.
- APK zip alignment:
  `zipalign -c -P 16 -v 4 app-release-unsigned.apk` passed.
- Current source status: post-RC build pipeline produces 16 KB-ready
  release APK/AAB native libraries locally.

Fix provenance:

| Library | Source/provenance | Before | Fix | After |
| --- | --- | --- | --- | --- |
| `libgojni.so` | `core/Makefile` via `gomobile bind` | Four ABI builds had `0x1000`; NDK r28 alone fixed only 64-bit. | Require NDK r28+ and set Android-only `CGO_LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"`. | `0x4000` for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`. |
| `libgmvpn_ffi.so` | `shared/Makefile` via `cargo ndk` | 32-bit `armeabi-v7a` and `x86` had `0x1000`. | Set Android-only `RUSTFLAGS` linker args for max/common page size. | `0x4000` for all four packaged ABIs. |
| `libjnidispatch.so` | `net.java.dev.jna:jna` Android AAR | Version `5.13.0` had `0x1000` for `armeabi`, `armeabi-v7a`, `x86`, and `x86_64`. | Upgrade JNA AAR to `5.17.0`, which includes the upstream Android 16 KB linker fix. | `0x4000` for every packaged JNA ABI, including legacy AAR entries. |
| AndroidX native libs | AndroidX Graphics/DataStore dependencies | Already `0x4000`. | No change. | `0x4000`. |

Release impact:

- Existing RC1 artifacts are unchanged and remain tied to source SHA
  `1775829107eac1066af911353fc17f8d11f24a18`.
- This 16 KB fix is post-RC/P1 work and does not retarget
  `android-v1.0.0-rc.1`.
- Post-RC/P1 source prepares RC2 candidate metadata:
  `versionCode = 1000002`, `versionName = "1.0.0-rc.2"`.
- RC2 tag/release is not approved, RC2 tag is not created, and signed
  RC2 artifacts are evidence only until separately approved.
- A Play-bound artifact still requires a new signed workflow run from
  the post-RC source commit before submission.

## Signed release APK physical-device validation

Use the signed artifact from the workflow run being evaluated. Do not
mix a newer APK with an older source SHA.

Artifact for RC1:

```text
.local/release-artifacts/android-v1.0.0-rc.1/
  gmvpn-android-android-v1.0.0-rc.1-signed/
    outputs/apk/release/app-release.apk
```

Procedure:

1. Confirm the physical device is in ADB state `device`, not `offline`
   or `unauthorized`.
2. Remove debug and release packages if a clean install is required:

   ```powershell
   adb uninstall com.gmvpn.client.debug
   adb uninstall com.gmvpn.client
   ```

3. Install the signed APK:

   ```powershell
   adb install -r .local\release-artifacts\android-v1.0.0-rc.1\gmvpn-android-android-v1.0.0-rc.1-signed\outputs\apk\release\app-release.apk
   ```

4. Launch the app:

   ```powershell
   adb shell monkey -p com.gmvpn.client 1
   ```

5. Validate first launch and no-profile behavior.
6. Import or create a test profile through the normal UI. Do not put
   profile URIs, UUIDs, passwords, server names, or raw screenshots in
   tracked files.
7. Start the tunnel and approve the Android VPN permission prompt.
8. Confirm the UI reaches Connected and stays stable for at least 60s.
9. Confirm HTTPS browsing through the tunnel.
10. Confirm IPv4 egress uses the expected VPN path; redact public IPs.
11. Run a basic DNS leak check and record only redacted resolver
    ownership/path evidence.
12. Stop the tunnel and confirm Android no longer shows GMvpn as the
    active VPN.
13. Export a diagnostics bundle if needed, then commit only a redacted
    summary.

Pass/fail evidence should go into `docs/android-v1-validation-checklist.md`.
Raw device identifiers, IPs, logs, diagnostics, and profile material
belong only under ignored local artifact directories.

## Controlled UDP validation plan

Current status is `pass_limited` because the previous test used
browser WebRTC/STUN and a YouTube/QUIC-style playback window. Replace
that with a controlled test before claiming measured UDP behavior.

Minimum controlled plan:

- Configure an iperf3 server reachable only through an approved test
  environment. Do not commit hostnames, IPs, or credentials.
- Record the test parameters outside git, for example:
  `GMVPN_IPERF_HOST`, `GMVPN_IPERF_PORT`, duration, bitrate, and
  expected VPN profile.
- Server side:

  ```sh
  iperf3 -s -p "$GMVPN_IPERF_PORT"
  ```

- Android/client side, using an installed iperf3 binary such as a
  Termux package or a temporary test binary outside the app:

  ```sh
  iperf3 -c "$GMVPN_IPERF_HOST" -p "$GMVPN_IPERF_PORT" -u -b 1M -t 60 --json
  iperf3 -c "$GMVPN_IPERF_HOST" -p "$GMVPN_IPERF_PORT" -u -b 5M -t 180 --json
  ```

- Optional ADB wrapper if the test binary is available in device PATH:

  ```powershell
  adb shell iperf3 -c "$env:GMVPN_IPERF_HOST" -p "$env:GMVPN_IPERF_PORT" -u -b 1M -t 60 --json
  ```

- With the VPN disconnected, record whether the endpoint is reachable
  directly. If it is reachable directly, the result is not a leak test.
- With the VPN connected, run low-rate and moderate-rate UDP tests
  long enough to observe reconnect loops, TUN loss, packet loss, and
  engine errors.
- After UDP load, re-run HTTPS, IPv4 egress, DNS sanity, disconnect,
  and reconnect checks.

Pass criteria:

- The app remains connected or reports a clear error.
- No app crash, reconnect storm, TUN fd loss, or raw-network fallback.
- DNS and IPv4 checks still use the VPN path after the UDP window.
- Evidence is redacted before it is summarized in docs.

## IPv6 validation plan

Previous TECNO LG8n validation was `not_applicable` because the tested
network had no underlying IPv6 default route. A real IPv6 test needs a
different network or device/network combination.

Minimum plan:

1. Prove the baseline network has public IPv6 before starting GMvpn.
2. Start GMvpn and confirm Android LinkProperties routes `::/0` to the
   VPN interface or that IPv6 is explicitly blocked.
3. Use a browser IPv6 test and Android route inspection.
4. Fail the test on silent raw IPv6 fallback.
5. If full IPv6 support is not ready, document fail-closed behavior or
   keep the release limitation explicit.

Suggested commands:

```powershell
adb shell ip -6 route
adb shell dumpsys connectivity
```

Browser checks:

- `https://test-ipv6.com`
- `https://browserleaks.com/ip`
- `https://browserleaks.com/dns`

Evidence must redact public IPv6 addresses, resolver IPs, network
names, and profile/server details.

## Source-to-artifact traceability

For every Play-bound artifact, record:

- source commit
- workflow run URL/ID
- artifact names
- APK/AAB SHA-256 checksums
- APK signature verification result
- AAB signature verification result
- native artifact origin and checksums
- physical-device validation summary

Do not create or move a release tag unless it points to the exact
source commit that produced the signed artifacts being approved.
