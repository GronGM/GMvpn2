# /clients/android — GMvpn Android client

Kotlin + Jetpack Compose app with `android.net.VpnService` as the tunnel
primitive. Consumes:

- `core/build/gmvpn.aar` — Xray-core wrapper (see `core/README.md`).
- `shared/gmvpn-ffi` — Rust domain layer via UniFFI (wired later).

Status: **Android v1 candidate wiring**. The service, notifications,
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
- Android SDK with platform 34 and build-tools.
  Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`, or point
  `local.properties` at it (file is gitignored).
- Go 1.22+ and `gomobile` if you are regenerating `gmvpn.aar`
  (see `core/README.md`).

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
`platform-tools`, `platforms;android-34`, and `build-tools;34.0.0`.

## Build

```sh
cd clients/android

# Debug APK
./gradlew :app:assembleDebug

# CI-safe JVM tests
./gradlew :app:testDebugUnitTest

# Lint + release APK
./gradlew :app:lint :app:assembleRelease

# Requires an emulator or connected device; not mandatory CI yet.
./gradlew :app:connectedDebugAndroidTest
```

First sync will download the Android Gradle Plugin and Compose BOM —
this needs network and ~1.5 GB of cache.

## UniFFI bindings + Xray-core engine artifacts

Both native pieces — the Rust UniFFI library (`libgmvpn_ffi.so` per ABI
+ Kotlin bindings) and the Go Xray-core wrapper (`gmvpn.aar`) — are
built by a single script:

```sh
# One-time prerequisites:
#   - Android NDK r26+ (set ANDROID_NDK_HOME)
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
  can be shrunk / obfuscated.
- IPv4, IPv6, and DNS are explicitly configured in `GmvpnVpnService`,
  but DNS/IPv6 leak audits remain device-blocked until the validation
  checklist is run on physical hardware.

## Still requires physical-device validation

- End-to-end connect / browse / disconnect with a known-good
  VLESS+Reality profile.
- DNS leak audit.
- IPv6 behavior audit.
- Always-on / block-without-VPN kill-switch audit.
- Reconnect across Wi-Fi/cellular network changes.
- UDP-heavy traffic validation.

Runbook: `docs/android-device-validation.md`.
Machine-readable checklist: `docs/android-v1-validation-checklist.md`.
