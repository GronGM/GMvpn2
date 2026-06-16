# Android v1 validation checklist

The YAML block below is the machine-readable checklist for Android v1
device validation. Keep `status` as one of `pending`, `pass`, `fail`,
`pass_limited`, `blocked`, or `not_applicable`. Do not change
device-only items to `pass` without real device evidence.

```yaml
schema_version: 1
product: GMvpn2
platform: android
package_debug: com.gmvpn.client.debug
package_release: com.gmvpn.client
version_code: 1000003
version_name: 1.0.0-rc.3
rc_tag_candidate: android-v1.0.0-rc.3
overall_status: rc3_candidate_local_fixes_pending_signed_artifacts_not_tagged_not_released
rc_tag_approval_package:
  rc_candidate: android-v1.0.0-rc.1
  artifact_source_sha: "1775829107eac1066af911353fc17f8d11f24a18"
  docs_audit_head_after_artifact_verification: "a2fe00a5677665a44ab6b1396a50acf2e28f0d42"
  workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27632339860"
  workflow_run_id: 27632339860
  apk_aab_signed: true
  apk_signature_verified: true
  checksums_verified: true
  secrets_exposed: false
  tag_release_requires_explicit_approval: true
rc3_candidate:
  rc_candidate: android-v1.0.0-rc.3
  status: local_fixes_pending_signed_artifacts_not_tagged_not_released
  based_on_branch: codex/p1-play-compliance-and-device-validation
  version_code: 1000003
  version_name: 1.0.0-rc.3
  release_blocker_cleanup:
    vpn_permission_cancel_state_fix: pending_verification
    invalid_profile_persistent_error_ux: pending_verification
  apk_aab_signed: false
  physical_validation_status: pending
  rc3_tag_created: false
  github_release_created: false
rc2_candidate:
  rc_candidate: android-v1.0.0-rc.2
  status: physical_validation_failed_not_tagged_not_released
  based_on_branch: codex/p1-play-compliance-and-device-validation
  artifact_source_sha: "4d15f3054384cd6a1ee7ae954491ade0e7a98370"
  version_code: 1000002
  version_name: 1.0.0-rc.2
  workflow_run_url: "https://github.com/GronGM/GMvpn2/actions/runs/27640095772"
  workflow_run_id: 27640095772
  apk_aab_signed: true
  apk_signature_verified: true
  aab_signature_verified: true
  checksums_verified: true
  native_16kb_verified: true
  apk_zipalign_16kb_verified: true
  physical_validation_status: failed
  physical_validation_date: "2026-06-16"
  physical_validation_device: "physical Android 12 / API 31"
  physical_validation_blockers:
    - "VPN permission cancel path leaves UI stuck in Preparing with Disconnect visible"
    - "Invalid-profile start fails safely in service logs, but the user-visible error was not persistently visible in the captured UI"
    - "No approved real VPN profile/server was used, so signed RC2 tunnel, DNS, IPv4 route, UDP, and IPv6 validation remain pending"
  rc1_tag_unchanged: true
  rc2_tag_created: false
  github_release_created: false
items:
  - id: apk-debug-build
    priority: P0
    status: pass
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:assembleDebug"
    evidence: "2026-06-15: Gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest passed; debug APK at clients/android/app/build/outputs/apk/debug/app-debug.apk. 2026-06-15 RC packaging step bumped Android metadata to versionName 1.0.0-rc.1 / versionCode 1000001; the post-packaging Gradle command :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease passed. Physical-device evidence remains tied to the earlier audited debug build and no production/public release is implied."

  - id: native-artifacts-build
    priority: P0
    status: pass
    requires_physical_device: false
    command: "./scripts/build-android-libs.sh"
    evidence: "2026-06-15: Rust libgmvpn_ffi.so rebuilt for arm64-v8a/armeabi-v7a/x86/x86_64 via cargo-ndk; Kotlin UniFFI bindings regenerated; Go gmvpn.aar rebuilt via gomobile bind. Local Windows Git Bash did not have GNU make, so the Makefile-equivalent cargo/uniffi/gomobile commands were run directly after updating gomobile and moving Go temp/cache to D:. CI runs scripts/build-android-libs.sh on Ubuntu, where make is available. Artifacts remain ignored under core/build, shared/target, clients/android/app/libs, and clients/android/app/src/main/jniLibs."

  - id: apk-release-build
    priority: P0
    status: pass_limited
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:assembleRelease :app:bundleRelease --stacktrace"
    evidence: "2026-06-15: initial release build failed in R8 because JNA optional desktop/AWT helpers referenced java.awt classes unavailable on Android. Added narrow dontwarn rules for java.awt.Component, GraphicsEnvironment, HeadlessException, and Window in app/proguard-rules.pro. :app:assembleRelease and :app:bundleRelease then passed, producing app-release-unsigned.apk and app-release.aab. 2026-06-15 RC packaging step set versionName 1.0.0-rc.1 / versionCode 1000001 and prepared manual android-release.yml packaging/signing workflow; the post-packaging Gradle command passed and aapt2 confirmed release package com.gmvpn.client versionCode 1000001 versionName 1.0.0-rc.1. Local apksigner verification of app-release-unsigned.apk returned DOES NOT VERIFY as expected without RELEASE_KEYSTORE_* env vars. 2026-06-16 manual workflow run 27632339860 produced signed APK/AAB artifacts; public distribution still requires explicit tag/release approval."

  - id: release-signing-workflow
    priority: P0
    status: pass
    requires_physical_device: false
    command: "gh workflow run android-release.yml --repo GronGM/GMvpn2 -f rc_tag=android-v1.0.0-rc.1 -f version_name=1.0.0-rc.1"
    evidence: "2026-06-16: manual-only android-release.yml run 27632339860 succeeded from branch claude/relaxed-euler-1Vr2R at 1775829107eac1066af911353fc17f8d11f24a18. It did not create git tags or GitHub Releases. It uploaded unsigned audit artifact gmvpn-android-android-v1.0.0-rc.1-unsigned-audit and signed artifact gmvpn-android-android-v1.0.0-rc.1-signed as GitHub Actions artifacts. Downloaded signed APK verified locally with apksigner using APK Signature Scheme v2 and one signer; signed-rc.sha256 matched the signed APK/AAB, and unsigned-audit.sha256 matched all five unsigned audit files. Local copy: .local/release-artifacts/android-v1.0.0-rc.1/."

  - id: target-sdk-35-play-migration
    priority: P1
    status: pass_limited
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace"
    evidence: "2026-06-16: installed local Android SDK platform android-35 and build-tools 35.0.0; bumped compileSdk and targetSdk from 34 to 35; kept minSdk 26; Gradle command :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace passed. Limitation: connected instrumentation tests and signed SDK-35 workflow artifacts still need separate validation before any Play-bound artifact is approved."

  - id: play-vpnservice-declaration
    priority: P1
    status: pass_limited
    requires_physical_device: false
    manual_step: "Prepare Play Console VpnService declaration from docs/android-play-compliance-and-validation.md"
    evidence: "2026-06-16: prepared Play Console VpnService declaration draft in docs/android-play-compliance-and-validation.md. Current audit: VPN is core functionality; manifest service is private, protected by BIND_VPN_SERVICE, and declares android.net.VpnService; repo scan found no ad, analytics, crash-reporting, hidden telemetry, or traffic monetization SDK in the Android dependency/config surface. Limitation: Play listing copy, screenshots, Data safety answers, and final product/privacy review are still pending."

  - id: android-15-fgs-vpnservice-audit
    priority: P1
    status: pass_limited
    requires_physical_device: false
    command: "rg -n \"foregroundServiceType|FOREGROUND_SERVICE|dataSync|mediaProcessing|BOOT_COMPLETED|BroadcastReceiver|onTimeout|startForeground|VpnService\" clients/android/app/src/main"
    evidence: "2026-06-16: audited AndroidManifest, GmvpnVpnService, and TunnelController. GmvpnVpnService is the only foreground service, uses foregroundServiceType=systemExempted, declares BIND_VPN_SERVICE and android.net.VpnService intent filter, is exported=false, and is started through user/VpnService flows. No dataSync/mediaProcessing foreground service type, BOOT_COMPLETED receiver, or background boot auto-start path was found. Limitation: long-running signed-release physical tunnel validation still required."

  - id: native-16kb-page-size-readiness
    priority: P1
    status: pass
    requires_physical_device: false
    command: "scripts/check-android-16kb-elf-alignment.sh <release-apk-or-aab>"
    evidence: "2026-06-16: post-RC/P1 source pipeline updated for Android NDK r28c, gomobile CGO_LDFLAGS, cargo-ndk RUSTFLAGS, and JNA 5.17.0. Local Gradle command :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease --stacktrace passed. scripts/check-android-16kb-elf-alignment.sh passed against local unsigned APK/AAB and against downloaded signed RC2 candidate APK/AAB from workflow run 27640095772: all 23 packaged .so entries in each artifact had minimum LOAD align 0x4000. zipalign -c -P 16 passed for unsigned and signed APKs. Limitation: existing RC1 signed artifacts are unchanged; RC2 tag/release is not approved."

  - id: release-signing-workflow-rc2-candidate
    priority: P1
    status: pass
    requires_physical_device: false
    command: "gh workflow run android-release.yml --repo GronGM/GMvpn2 --ref codex/p1-play-compliance-and-device-validation -f rc_tag=android-v1.0.0-rc.2 -f version_name=1.0.0-rc.2"
    evidence: "2026-06-16: manual-only android-release.yml run 27640095772 succeeded from branch codex/p1-play-compliance-and-device-validation at 4d15f3054384cd6a1ee7ae954491ade0e7a98370. It did not create git tags or GitHub Releases. It uploaded unsigned audit artifact gmvpn-android-android-v1.0.0-rc.2-unsigned-audit and signed artifact gmvpn-android-android-v1.0.0-rc.2-signed as GitHub Actions artifacts. CI verified unsigned native ELF 16 KB alignment, unsigned APK zipalign -P 16, signed native ELF 16 KB alignment, signed APK apksigner verification, and signed APK zipalign -P 16. Local download under .local/release-artifacts/android-v1.0.0-rc.2/ verified signed-rc.sha256 and unsigned-audit.sha256, APK v2 signature, AAB jarsigner verification with expected self-signed/untimestamped RC certificate warnings, signed APK/AAB 16 KB ELF alignment, signed APK zipalign -P 16, and aapt metadata versionCode 1000002 / versionName 1.0.0-rc.2 / minSdk 26 / targetSdk 35. Signed APK SHA-256: 4f8901d00af6f09792b39584168d758466b1e16174d86a35e83e6a27709334c5. Signed AAB SHA-256: 92da35514e603e1474edd42c665a9192c702bc49c9c2f941f939abb5282fc7e2. RC1 tag remains unchanged; RC2 tag and GitHub Release were not created."

  - id: signed-release-apk-physical-validation
    priority: P1
    status: fail
    requires_physical_device: true
    command: "adb install -r .local/release-artifacts/android-v1.0.0-rc.2/gmvpn-android-android-v1.0.0-rc.2-signed/outputs/apk/release/app-release.apk"
    evidence: "2026-06-16: signed RC2 APK installed on a physical Android 12/API 31 device with adb install -r and launched com.gmvpn.client/.ui.MainActivity. Package metadata remained versionCode 1000002 / versionName 1.0.0-rc.2. Fresh launch and About passed: UI opened without crash/ANR, no-profile state was understandable, Connect was disabled without an active profile, About showed app 1.0.0-rc.2, core 0.0.1, and Xray-core 26.3.27. A non-secret dummy invalid profile enabled Connect and Android's com.android.vpndialogs VPN permission dialog appeared for GMvpn. Cancel path did not crash, but remained stuck at Preparing with Disconnect visible after waiting; this is a release-blocking state bug. Later attempts with the dummy invalid profile reached GmvpnVpnService and failed safely with profile URI unsupported protocol, cleanupAfterFailure, stopForeground/remove, and no Connected state, but the user-visible invalid-profile error was not persistently visible in the captured UI. Raw logcat and UI dumps stayed under ignored .local/device-validation/ and were not committed; post-test adb shell pm clear com.gmvpn.client removed the dummy profile. Crash scan found no FATAL EXCEPTION, AndroidRuntime crash, or ANR for com.gmvpn.client. Privacy scan found no private key blocks or vless/vmess/trojan/ss URI tokens in raw logcat, and no UUID/password/Auth/Cookie/X-Api-Key patterns in GMvpn-related lines; one Android BackupManager restore-at-install token field was a system line, not a GMvpn VPN credential. Signed RC2 physical validation is fail/blocked until the VPN permission cancel state and invalid-profile visible-error behavior are fixed and a real approved VPN profile/server validation is rerun."

  - id: controlled-udp-iperf-validation
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "Run controlled iperf3 UDP validation through an approved test endpoint"
    evidence: "Current UDP status is pass_limited from browser WebRTC/STUN plus YouTube/QUIC-style playback. Do not mark pass until an approved controlled UDP endpoint is used and redacted throughput/loss/stability evidence is recorded."

  - id: real-ipv6-network-validation
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "Run IPv6 leak validation on a network with a real public IPv6 baseline"
    evidence: "Previous TECNO/network had no underlying IPv6 default route, so IPv6 was not_applicable. Do not mark pass until a real IPv6 baseline is proven and active-VPN behavior either tunnels IPv6 or fails closed without raw IPv6 fallback."

  - id: android-lint
    priority: P0
    status: pass_limited
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:lintDebug --stacktrace"
    evidence: "2026-06-15: initial lintDebug failed on ForegroundServicePermission for android:foregroundServiceType=\"systemExempted\" even though GmvpnVpnService is a VpnService and already declares FOREGROUND_SERVICE_SYSTEM_EXEMPTED. Added a narrow tools:ignore=\"ForegroundServicePermission\" on the VPN service instead of adding unrelated exact-alarm permissions. :app:lintDebug then passed. Limitation: this is an explicit lint suppression for the VPN service exemption, not a new runtime permission."

  - id: emulator-smoke-tests
    priority: P1
    status: pass
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:connectedDebugAndroidTest"
    evidence: "2026-06-15: VpnTunnelSmokeTest passed on emulator gmvpn_api34 and physical TECNO LG8n Android 12/API 31; reports under clients/android/app/build/reports/androidTests/connected/debug/"

  - id: install-debug-apk
    priority: P0
    status: pass
    requires_physical_device: true
    command: "adb install -r clients/android/app/build/outputs/apk/debug/app-debug.apk"
    evidence: "2026-06-15: adb install succeeded on TECNO LG8n, Android 12/API 31; package com.gmvpn.client.debug versionName 0.0.1 targetSdk 34"

  - id: import-vless-reality-profile
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Import a redacted throwaway VLESS+Reality test profile"
    evidence: "2026-06-15: TECNO LG8n imported HTTPS subscription from .local/test-profile.txt through the normal UI confirmation flow; 4 VLESS+Reality profiles saved; redacted evidence in artifacts/android-diagnostics/20260615-194415/"

  - id: xray-version-visible
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Open About and verify Engine: Xray-core is not unbundled"
    evidence: "2026-06-15: About screen on TECNO LG8n showed Xray-core 26.3.27; redacted UI dump artifacts/android-diagnostics/20260615-194415/16-about-engine-version.xml"

  - id: vpn-permission-flow
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Tap Connect before permission is granted; approve Android VPN prompt"
    evidence: "2026-06-15: Android VPN permission prompt appeared from com.android.vpndialogs, user approved it physically, and later Connect attempts on TECNO LG8n reused the granted permission"

  - id: basic-connect-browse-disconnect
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Connect, browse two HTTPS sites, disconnect"
    evidence: "2026-06-15: TECNO LG8n reached Connected and remained stable for 60s after EngineBridge class lookup fix; browser loaded https://example.com and https://api.ipify.org through active VPN; disconnect returned UI to Disconnected and closed tun fd only through handleStop; redacted evidence in artifacts/android-diagnostics/tun-lifecycle-fixed-20260615-201047/"

  - id: ipv4-connectivity
    priority: P0
    status: pass
    requires_physical_device: true
    command: "adb shell ping -4 -c 4 1.1.1.1"
    evidence: "2026-06-15: TECNO LG8n browser loaded https://api.ipify.org while VPN was Connected and displayed a public IPv4 address; exact IP redacted. adb shell curl/wget were not available on the device, so browser evidence was used instead."

  - id: ipv6-behavior
    priority: P0
    status: not_applicable
    requires_physical_device: true
    command: "adb shell ping -6 -c 4 2606:4700:4700::1111"
    evidence: "2026-06-15: TECNO LG8n browser test-ipv6.com showed no public IPv6 while VPN was Connected; baseline had no underlying IPv6 default route, so full IPv6 egress is not applicable for this device/network. Android VPN LinkProperties included ::/0 -> tun0 and no public IPv6 fall-through was observed. Redacted evidence: artifacts/android-diagnostics/dns-ipv6-audit-20260615-202413/ipv6-behavior-summary-redacted.txt"

  - id: dns-leak-audit
    priority: P0
    status: pass
    requires_physical_device: true
    command: "adb shell nslookup example.com"
    evidence: "2026-06-15: TECNO LG8n browser-based DNS leak audit ran dnsleaktest.com standard test and browserleaks.com/dns while VPN was Connected; BrowserLeaks reported public/VPN-path resolver providers (Cloudflare, Google LLC, Kraken Network ISP LTD) and no local mobile/Wi-Fi ISP resolver in the result set. Raw IPs redacted. Evidence: artifacts/android-diagnostics/dns-ipv6-audit-20260615-202413/dns-leak-summary-redacted.txt"

  - id: kill-switch-always-on
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Enable Always-on VPN and Block connections without VPN, then interrupt network"
    evidence: "2026-06-15: TECNO LG8n Android 12/API 31 exposed Always-on VPN and Block connections without VPN for GMvpn. After GmvpnVpnService handled the Android system android.net.VpnService start action, enabling Always-on started the service via the system path and UI reached Connected. With lockdown=1, HTTPS loaded while VPN was active; after force-stop of com.gmvpn.client.debug, Chrome could not load a unique example.com URL and logcat returned BLOCKED NetworkInfo instead of direct network access. Restore set always_on_vpn_app=null and always_on_vpn_lockdown=0; onRevoke -> handleStop -> closeTun was observed. Evidence: artifacts/android-diagnostics/always-on-killswitch-20260615-204557/"

  - id: reconnect-network-change
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Switch Wi-Fi to cellular and back while connected"
    evidence: "2026-06-15: TECNO LG8n Android 12/API 31 had active cellular data (mobile_data=1). With GMvpn Connected, adb svc wifi disable moved the active path to cellular+VPN; HTTPS example.com and IPv4 browser egress still worked. adb svc wifi enable moved back to Wi-Fi+VPN; HTTPS and IPv4 still worked. A second short Wi-Fi off/on cycle also ended in Wi-Fi+VPN with browser success. The tunnel state remained Connected across the observed handovers; no app crash or traffic leak was observed. Post-handover UI disconnect removed the VPN, reconnect reached VPN Connected for 60s, HTTPS worked, and final disconnect removed the VPN. DNS sanity after handover used VPN LinkProperties DNS 1.1.1.1/8.8.8.8 and browser domain resolution. Evidence: artifacts/android-diagnostics/network-handover-20260615-212318/"

  - id: udp-heavy-traffic
    priority: P0
    status: pass_limited
    requires_physical_device: true
    manual_step: "Run DNS workload and 5-minute video/QUIC-heavy browsing"
    evidence: "2026-06-15: TECNO LG8n Android 12/API 31 had no controlled UDP/iperf target in ignored local config/env, so the documented browser/WebRTC/QUIC fallback was used. Chrome loaded a WebRTC/STUN leak-check page, then played a 10-minute YouTube browser video for a 5-minute observed window; playback progressed from 9:34 to 3:25 while VPN LinkProperties stayed Connected at each minute. After the UDP-heavy window, browser HTTPS example.com, browser IPv4 egress, DNS sanity via VPN LinkProperties plus browser domain resolution, UI disconnect, 60s reconnect, and final disconnect all passed. Logcat showed no GMvpn crash/panic, reconnect loop, TUN loss, or app onDestroy during the UDP-heavy window. Limitation: this validates browser/WebRTC/QUIC behavior only; it does not measure controlled iperf UDP throughput/loss. Evidence: artifacts/android-diagnostics/udp-heavy-20260615-215101/; adb diagnostics bundle: artifacts/android-diagnostics/20260615-191157Z/"

  - id: diagnostics-export-in-app
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "About -> Export diagnostics"
    evidence: "redacted diagnostics text; no profile URI, UUID, token, pbk, sid, spx"

  - id: diagnostics-adb-bundle
    priority: P1
    status: pass
    requires_physical_device: true
    command: "./scripts/collect-android-diagnostics.sh"
    evidence: "2026-06-15: Git Bash syntax check passed and scripts/collect-android-diagnostics.sh collected artifacts/android-diagnostics/20260615-171555Z, artifacts/android-diagnostics/20260615-174021Z, artifacts/android-diagnostics/20260615-181132Z, artifacts/android-diagnostics/20260615-184123Z, and artifacts/android-diagnostics/20260615-191157Z from TECNO LG8n; default Windows bash points to WSL and is blocked by missing distro"

  - id: release-not-ready-until-device-pass
    priority: P0
    status: pass
    requires_physical_device: true
    manual_step: "Confirm every P0 device item is pass"
    evidence: "2026-06-15: P0 physical validation evidence is complete on TECNO LG8n, including stable connect/basic browse, DNS leak audit, this TECNO/network's IPv6 behavior, Always-on/block-without-VPN, Wi-Fi/cellular handover reconnect, and UDP-heavy browser/WebRTC/QUIC fallback validation. Final release-readiness audit found and fixed two local blockers: R8 rules for JNA optional AWT references and a narrow lint suppression for VpnService systemExempted foreground-service type. After the fixes debug build/tests, physical connected tests, lintDebug, release APK build, and release bundle build passed. Classification: release_ready_candidate, not a production/public distribution claim."
```
