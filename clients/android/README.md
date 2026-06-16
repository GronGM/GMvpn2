# /clients/android — GMvpn Android client

Kotlin + Jetpack Compose app with `android.net.VpnService` as the tunnel
primitive. Consumes:

- `core/build/gmvpn.aar` — Xray-core wrapper (see `core/README.md`).
- `shared/gmvpn-ffi` — Rust domain layer via UniFFI (wired later).

Status: **Android v1 RC3 GitHub Pre-release is available for APK
testing**. It is not a final production release, not a Google Play
publication, and not `android-v1.0.0`. The service, notifications,
permission dance, encrypted profile library, subscription import,
per-app routing, reconnect handling, diagnostics export, and engine
bridge are real. Without `gmvpn.aar` / `libgmvpn_ffi.so`, the app
surfaces a typed engine-unavailable error instead of crashing.

## Layout

```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/
  libs.versions.toml        version catalog
  wrapper/…                 committed gradle-wrapper jar + props
gradlew / gradlew.bat
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    kotlin/com/gmvpn/client/
      GmvpnApp.kt           Application
      tunnel/
        TunnelStatus.kt     UI-facing state
        TunnelController.kt process-wide facade (StateFlow)
        GmvpnVpnService.kt  VpnService + foreground notification
      ui/
        MainActivity.kt     Compose host + VPN permission launcher
        HomeScreen.kt       Connect / Disconnect + status card
        theme/Theme.kt
    res/
      values/{strings,themes,colors}.xml
      xml/{backup_rules,data_extraction_rules}.xml
      mipmap-anydpi-v26/ic_launcher.xml
      drawable/ic_launcher_foreground.xml
```

## Prerequisites

- JDK 17+ (Android Gradle Plugin 8.6 requires 17).
- Android SDK with platform 35 and build-tools 35.0.0.
  Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`, or point
  `local.properties` at it (file is gitignored).
- Go 1.26+ and `gomobile` if you are regenerating `gmvpn.aar`
  (see `core/README.md`).
- Rust, `cargo-ndk`, the four Android Rust targets, Android NDK r28+,
  and GNU Make if you are running `scripts/build-android-libs.sh`
  directly.

## Windows development environment

On Windows, Codex/developers should first look for Java in:

- `C:\Program Files\Android\Android Studio\jbr`
- `C:\Program Files\Eclipse Adoptium`
- `C:\Program Files\Microsoft\jdk*`
- `C:\Program Files\Java`

The Android SDK is usually under:

- `%LOCALAPPDATA%\Android\Sdk`
- `C:\Android\Sdk`

Set the Java and Android SDK paths before running Gradle. Typical
PowerShell values:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:PATH"
```

If Gradle cannot discover the Android SDK, create
`clients/android/local.properties` with an escaped Windows path:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

Verify the local toolchain before building:

```sh
java -version
adb version
sdkmanager --version
./gradlew tasks
```

If `sdkmanager` or Gradle reports that Android SDK licenses are not
accepted, finish that step through Android Studio SDK Manager or an
interactive `sdkmanager --licenses` session before installing
`platform-tools`, `platforms;android-35`, and `build-tools;35.0.0`.

## Build

```sh
cd clients/android

# Debug APK
./gradlew :app:assembleDebug

# CI-safe JVM tests
./gradlew :app:testDebugUnitTest

# Lint + release-shaped APK/AAB
./gradlew :app:lint :app:assembleRelease :app:bundleRelease

# Requires an emulator or connected device; not mandatory CI yet.
./gradlew :app:connectedDebugAndroidTest
```

First sync will download the Android Gradle Plugin and Compose BOM —
this needs network and ~1.5 GB of cache.

## Release candidate packaging

Current Android package metadata:

- `applicationId`: `com.gmvpn.client`
- debug package: `com.gmvpn.client.debug`
- release package: `com.gmvpn.client`
- `versionCode`: `1000003`
- `versionName`: `1.0.0-rc.3`
- RC3 tag name: `android-v1.0.0-rc.3`

The RC tag is not created by Gradle or CI. The manual workflow
`.github/workflows/android-release.yml` accepts `rc_tag` and
`version_name`, builds unsigned audit artifacts, and then requires all
release signing secrets before producing signed RC artifacts as GitHub
Actions artifacts. It does not publish a GitHub Release.

Run `27632339860` on 2026-06-16 produced signed RC APK/AAB artifacts
for `android-v1.0.0-rc.1`; local verification of the downloaded
artifact confirmed APK signature verification and SHA-256 checksums.
The annotated RC tag `android-v1.0.0-rc.1` now exists and remains tied
to its original source SHA. No GitHub Release exists.

Post-RC/P1 source now has signed RC3 candidate artifacts
(`versionCode` `1000003`, `versionName` `1.0.0-rc.3`) for SDK 35,
16 KB native readiness, VPN permission cancel, and invalid-profile UX
validation. Workflow run `27643689894` produced signed APK/AAB
artifacts from commit `dd10df9d3683fa41ccc628e5db0c186d029dd6ae`.
The RC3 tag exists and a GitHub Pre-release for tester APK download is
published at:

```text
https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3
```

The Pre-release contains only:

- `GMvpn-android-v1.0.0-rc.3.apk`
- `GMvpn-android-v1.0.0-rc.3.apk.sha256`

No AAB is attached for general testers. This is not the final
production `android-v1.0.0` release and is not a Google Play
publication. Physical validation is pass-limited: the permission-cancel and
invalid-profile UX blockers passed on a physical TECNO LG8n with a
redacted real-profile connect/lifecycle run, while controlled UDP/iperf,
a full DNS leak audit, and real external IPv6 validation remain
pending. The earlier signed RC2 artifacts are historical evidence only
and failed physical validation on the permission-cancel and
invalid-profile UX paths.

Required signing inputs:

- `RELEASE_KEYSTORE_BASE64` in GitHub Actions, decoded only in
  `runner.temp`
- `RELEASE_KEYSTORE_PATH` for local/decoded keystore path
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

If those variables are absent, local `assembleRelease` and
`bundleRelease` intentionally remain unsigned so the build can still be
audited. Distribution requires a signed workflow run and explicit tag
approval. Full runbook: `docs/android-release-signing.md`.

## UniFFI bindings + Xray-core engine artifacts

Both native pieces — the Rust UniFFI library (`libgmvpn_ffi.so` per ABI
+ Kotlin bindings) and the Go Xray-core wrapper (`gmvpn.aar`) — are
built by a single script:

```sh
# One-time prerequisites:
#   - Android NDK r28+ (set ANDROID_NDK_HOME)
#   - GNU Make (the script calls shared/core Makefiles)
#   - cargo install cargo-ndk
#   - rustup target add aarch64-linux-android armv7-linux-androideabi \
#       x86_64-linux-android i686-linux-android
#   - go install golang.org/x/mobile/cmd/gomobile@latest && gomobile init

../../scripts/build-android-libs.sh
```

CI runs the same script in `.github/workflows/android-aar.yml` on every
push to `shared/` or `core/`; artifacts are uploaded as
`gmvpn-android-libs-<sha>.zip`.

Wiring the artifacts into the Android module:

1. Copy the four `libgmvpn_ffi.so` from
   `shared/target/android/jniLibs/<abi>/` into
   `app/src/main/jniLibs/<abi>/`.
2. Copy `core/build/gmvpn.aar` into `app/libs/`.

The Kotlin UniFFI bindings (`gmvpn_ffi.kt`) and the Gradle wiring
(JNA dependency, `fileTree("libs")` include for the `.aar`) are
already in place. To refresh `gmvpn_ffi.kt` after a UniFFI surface
change run `make -C ../../shared kotlin` and copy
`shared/bindings/kotlin/uniffi/gmvpn_ffi/gmvpn_ffi.kt` into
`app/src/main/kotlin/uniffi/gmvpn_ffi/`.

16 KB native page-size verification:

```sh
../../scripts/check-android-16kb-elf-alignment.sh \
  app/build/outputs/apk/release/app-release-unsigned.apk
../../scripts/check-android-16kb-elf-alignment.sh \
  app/build/outputs/bundle/release/app-release.aab
```

The Android native build path sets linker flags for gomobile and
cargo-ndk so `libgojni.so` and `libgmvpn_ffi.so` are emitted with
`LOAD` alignment at least `0x4000`. JNA is pinned to a 16 KB-ready AAR.

## Instrumented smoke tests

`app/src/androidTest/kotlin/com/gmvpn/client/tunnel/VpnTunnelSmokeTest.kt`
is the emulator/device smoke scaffold. It verifies the `VpnService`
manifest boundary, `VpnService.prepare`, `EngineBridge` availability,
non-empty `XrayVersion()` when `gmvpn.aar` is bundled, idle disconnect,
and the safe no-active-profile start path.

These tests do not fake a successful VPN connection and do not replace
the physical-device validation in `docs/android-device-validation.md`.

## Engine pipeline

`GmvpnVpnService.handleStart()` is wired end-to-end:

1. Pull the active profile URI from `ProfileStore` (DataStore-backed).
2. Parse it via `uniffi.gmvpn_ffi.parseProfileUri(uri)`.
3. Build the Xray-core config via
   `buildXrayConfig(profile, defaultTunnelOptions())`.
4. Establish the TUN through `VpnService.Builder` (10.10.10.2/28,
   `fd00:0:0:1::2/112`, default routes for IPv4 and IPv6, DNS
   `1.1.1.1` + `8.8.8.8`, MTU 1500). Our own package is excluded so
   the SOCKS inbound on 127.0.0.1 stays reachable.
5. Hand the `tunFd`, MTU, and SOCKS port to `EngineBridge.start(...)`.
   The bridge talks to the gomobile-bound classes
   `com.gmvpn.core.gmvpn.Gmvpn` / `Tunnel` / `StatusListener` via
   reflection so the app compiles even when `gmvpn.aar` is not yet
   in `app/libs/`. Without the artifact the Connect button surfaces
   `EngineUnavailableException` in the error card instead of
   crashing the process.
6. Engine status events flow back through `TunnelController` →
   Compose UI.

`Stop` tears the engine down first, then closes the
`ParcelFileDescriptor` and stops the foreground service.

## Security posture

- `allowBackup=false` + deny-all backup rules: no profile or secret
  round-trips via cloud backup.
- Foreground service is mandatory while the tunnel is up (`systemExempted`
  type, matches VpnService lifecycle).
- ProGuard keeps the gomobile bridge classes reachable; everything else
  can be shrunk / obfuscated. JNA's optional desktop/AWT helper
  references are suppressed with narrow `java.awt` `-dontwarn` rules
  because they are unused by the Android UniFFI path.
- `GmvpnVpnService` uses `foregroundServiceType="systemExempted"` as a
  VPN service. AGP lint is suppressed only on that service for the
  foreground-service permission check; no unrelated exact-alarm
  permission is requested.
- IPv4, IPv6, and DNS are explicitly configured in `GmvpnVpnService`.
  Physical TECNO LG8n validation on 2026-06-15 covered connect/browse,
  IPv4 egress, DNS leak audit, this TECNO/network's IPv6 behavior,
  Always-on/block-without-VPN, Wi-Fi/cellular handover, and UDP-heavy
  browser/WebRTC/QUIC fallback.

## Release candidate limits

- UDP-heavy validation is `pass_limited`: no controlled UDP/iperf target
  was configured, so evidence is browser WebRTC/STUN plus a 5-minute
  YouTube/QUIC-style playback window, not measured UDP throughput/loss.
- IPv6 was `not_applicable` on the tested TECNO/network because there
  was no underlying IPv6 default route; re-run on an IPv6-capable
  network before claiming broad IPv6 tunneling support.
- Local `assembleRelease` produces `app-release-unsigned.apk` unless
  all `RELEASE_KEYSTORE_*` env vars point at a release keystore.
  Public distribution must use signed artifacts from
  `.github/workflows/android-release.yml` or an equivalent signing
  process, plus explicit tag/release approval; do not commit keystores
  or signing passwords.
- The 16 KB native page-size fix is post-RC/P1. Existing RC1 signed
  artifacts are unchanged; Play-bound artifacts need a new signed
  workflow run from the post-RC source commit.

Runbook: `docs/android-device-validation.md`.
Release signing: `docs/android-release-signing.md`.
RC notes: `docs/android-v1-rc-notes.md`.
Machine-readable checklist: `docs/android-v1-validation-checklist.md`.
