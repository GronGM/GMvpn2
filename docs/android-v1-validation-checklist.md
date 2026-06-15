# Android v1 validation checklist

The YAML block below is the machine-readable checklist for Android v1
device validation. Keep `status` as one of `pending`, `pass`, `fail`,
`blocked`, or `not_applicable`. Do not change device-only items to
`pass` without real device evidence.

```yaml
schema_version: 1
product: GMvpn2
platform: android
package_debug: com.gmvpn.client.debug
package_release: com.gmvpn.client
overall_status: blocked
items:
  - id: apk-debug-build
    priority: P0
    status: pass
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:assembleDebug"
    evidence: "2026-06-15: Gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest passed; debug APK at clients/android/app/build/outputs/apk/debug/app-debug.apk"

  - id: native-artifacts-build
    priority: P0
    status: pass
    requires_physical_device: false
    command: "./scripts/build-android-libs.sh"
    evidence: "2026-06-15: built gmvpn.aar via gomobile and libgmvpn_ffi.so for arm64-v8a/armeabi-v7a/x86/x86_64 via cargo-ndk; artifacts copied into clients/android/app/libs and clients/android/app/src/main/jniLibs, then :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest passed"

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
    evidence: "2026-06-15: Android VPN permission prompt appeared from com.android.vpndialogs, user approved it physically, and later Connect attempts did not reprompt; real tunnel start remains blocked separately"

  - id: basic-connect-browse-disconnect
    priority: P0
    status: blocked
    requires_physical_device: true
    manual_step: "Connect, browse two HTTPS sites, disconnect"
    evidence: "2026-06-15: Connect starts VpnService and Android logs Established by com.gmvpn.client.debug on tun0, then netd reports tun0 No such device/Remote I/O error and UI returns to Disconnected; no stable Connected state, browse, or disconnect evidence"

  - id: ipv4-connectivity
    priority: P0
    status: blocked
    requires_physical_device: true
    command: "adb shell ping -4 -c 4 1.1.1.1"
    evidence: "Blocked by basic-connect-browse-disconnect: tunnel does not remain connected long enough to validate IPv4 egress"

  - id: ipv6-behavior
    priority: P0
    status: blocked
    requires_physical_device: true
    command: "adb shell ping -6 -c 4 2606:4700:4700::1111"
    evidence: "Blocked by basic-connect-browse-disconnect: tunnel does not remain connected long enough to validate IPv6 tunneling or blocking"

  - id: dns-leak-audit
    priority: P0
    status: blocked
    requires_physical_device: true
    command: "adb shell nslookup example.com"
    evidence: "Blocked by basic-connect-browse-disconnect: tunnel does not remain connected long enough to run DNS leak checks"

  - id: kill-switch-always-on
    priority: P0
    status: blocked
    requires_physical_device: true
    manual_step: "Enable Always-on VPN and Block connections without VPN, then interrupt network"
    evidence: "Blocked by basic-connect-browse-disconnect: no stable connected baseline for Always-on / block-without-VPN audit"

  - id: reconnect-network-change
    priority: P1
    status: blocked
    requires_physical_device: true
    manual_step: "Switch Wi-Fi to cellular and back while connected"
    evidence: "Blocked by basic-connect-browse-disconnect: no stable connected baseline for Wi-Fi/cellular reconnect"

  - id: udp-heavy-traffic
    priority: P1
    status: blocked
    requires_physical_device: true
    manual_step: "Run DNS workload and 5-minute video/QUIC-heavy browsing"
    evidence: "Blocked by basic-connect-browse-disconnect: no stable connected baseline for UDP-heavy traffic"

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
    evidence: "2026-06-15: artifacts/android-diagnostics/20260615-165047Z collected from TECNO LG8n and scanned for URI/UUID/token/header patterns; only README reminder text matched profile URI strings"

  - id: release-not-ready-until-device-pass
    priority: P0
    status: blocked
    requires_physical_device: true
    manual_step: "Confirm every P0 device item is pass"
    evidence: "Blocked: real tunnel connect is not stable on TECNO LG8n; Android v1 must not be marked ready"
```
