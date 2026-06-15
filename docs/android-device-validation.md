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
- JDK 17+ and Android SDK platform/build-tools 34.
- Go, gomobile, Rust, cargo-ndk, Android NDK r26+, and the Rust
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
adb shell ping -4 -c 4 1.1.1.1
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
  adb shell ping -c 20 1.1.1.1
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

- `vless://`, `vmess://`, `trojan://`, `ss://`.
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
