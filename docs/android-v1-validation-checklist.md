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
overall_status: pending
items:
  - id: apk-debug-build
    priority: P0
    status: pending
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:assembleDebug"
    evidence: "APK path and Gradle result"

  - id: native-artifacts-build
    priority: P0
    status: pending
    requires_physical_device: false
    command: "./scripts/build-android-libs.sh"
    evidence: "core/build/gmvpn.aar and shared/target/android/jniLibs/*"

  - id: emulator-smoke-tests
    priority: P1
    status: pending
    requires_physical_device: false
    command: "cd clients/android && ./gradlew :app:connectedDebugAndroidTest"
    evidence: "instrumentation report showing VpnTunnelSmokeTest results"

  - id: install-debug-apk
    priority: P0
    status: pending
    requires_physical_device: true
    command: "adb install -r clients/android/app/build/outputs/apk/debug/app-debug.apk"
    evidence: "adb install success on device model and Android version"

  - id: import-vless-reality-profile
    priority: P0
    status: pending
    requires_physical_device: true
    manual_step: "Import a redacted throwaway VLESS+Reality test profile"
    evidence: "profile saved; screenshots/logs redact UUID, host, pbk, sid, spx"

  - id: xray-version-visible
    priority: P0
    status: pending
    requires_physical_device: true
    manual_step: "Open About and verify Engine: Xray-core is not unbundled"
    evidence: "redacted screenshot or copied version string"

  - id: vpn-permission-flow
    priority: P0
    status: pending
    requires_physical_device: true
    manual_step: "Tap Connect before permission is granted; approve Android VPN prompt"
    evidence: "permission prompt appears once; later connects do not reprompt"

  - id: basic-connect-browse-disconnect
    priority: P0
    status: pending
    requires_physical_device: true
    manual_step: "Connect, browse two HTTPS sites, disconnect"
    evidence: "status sequence, Android active VPN indicator, no crash"

  - id: ipv4-connectivity
    priority: P0
    status: pending
    requires_physical_device: true
    command: "adb shell ping -4 -c 4 1.1.1.1"
    evidence: "VPN egress observed; exact IP redacted if sensitive"

  - id: ipv6-behavior
    priority: P0
    status: pending
    requires_physical_device: true
    command: "adb shell ping -6 -c 4 2606:4700:4700::1111"
    evidence: "IPv6 tunneled through VPN or explicitly blocked; no raw-network leak"

  - id: dns-leak-audit
    priority: P0
    status: pending
    requires_physical_device: true
    command: "adb shell nslookup example.com"
    evidence: "dnsleaktest.com extended result plus nslookup output; resolvers redacted"

  - id: kill-switch-always-on
    priority: P0
    status: pending
    requires_physical_device: true
    manual_step: "Enable Always-on VPN and Block connections without VPN, then interrupt network"
    evidence: "traffic blocked while tunnel is down or reconnecting"

  - id: reconnect-network-change
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "Switch Wi-Fi to cellular and back while connected"
    evidence: "Reconnecting then Connected; repeat DNS/IPv6 checks after reconnect"

  - id: udp-heavy-traffic
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "Run DNS workload and 5-minute video/QUIC-heavy browsing"
    evidence: "no crash or SOCKS5 UDP ASSOCIATE errors in redacted logcat"

  - id: diagnostics-export-in-app
    priority: P1
    status: pending
    requires_physical_device: true
    manual_step: "About -> Export diagnostics"
    evidence: "redacted diagnostics text; no profile URI, UUID, token, pbk, sid, spx"

  - id: diagnostics-adb-bundle
    priority: P1
    status: pending
    requires_physical_device: true
    command: "./scripts/collect-android-diagnostics.sh"
    evidence: "artifacts/android-diagnostics/<timestamp>/ reviewed and redacted"

  - id: release-not-ready-until-device-pass
    priority: P0
    status: pending
    requires_physical_device: true
    manual_step: "Confirm every P0 device item is pass"
    evidence: "signed-off checklist with device model, Android version, and tester"
```
