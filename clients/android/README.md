# /clients/android — GMvpn Android client

Kotlin + Jetpack Compose app with `android.net.VpnService` as the tunnel
primitive. Consumes:

- `core/build/gmvpn.aar` — Xray-core wrapper (see `core/README.md`).
- `shared/gmvpn-ffi` — Rust domain layer via UniFFI (wired later).

Status: **scaffold**. The service, notifications, and permission dance
are real; the engine call itself returns `engine not wired` until
`gmvpn.aar` is produced and dropped into `app/libs/`.

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

## Build

```sh
cd clients/android

# Debug APK
./gradlew :app:assembleDebug

# Lint + release APK
./gradlew :app:lint :app:assembleRelease
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
2. Copy `shared/target/android/kotlin/uniffi/gmvpn_ffi/gmvpn_ffi.kt`
   into `app/src/main/kotlin/uniffi/gmvpn_ffi/`.
3. Copy `core/build/gmvpn.aar` into `app/libs/`.
4. In `app/build.gradle.kts`, add:
   ```kotlin
   implementation(files("libs/gmvpn.aar"))
   implementation("net.java.dev.jna:jna:5.13.0@aar")
   ```

## Engine integration (next step)

After the artifacts above are in place, replace the `TODO(engine)`
placeholder in `GmvpnVpnService.handleStart()` with:

- parse the active profile via `uniffi.gmvpn_ffi.parseProfileUri(uri)`;
- build Xray-core config JSON via
  `uniffi.gmvpn_ffi.buildXrayConfig(profile, defaultTunnelOptions())`;
- call `Builder#establish()` with the addresses, routes, DNS, and MTU
  the profile demands;
- `com.gmvpn.core.Gmvpn.new(listener).start(configJson, pfd.getFd(),
  mtu, socksPort)`;
- close the `ParcelFileDescriptor` we still hold on shutdown.

## Security posture

- `allowBackup=false` + deny-all backup rules: no profile or secret
  round-trips via cloud backup.
- Foreground service is mandatory while the tunnel is up (`systemExempted`
  type, matches VpnService lifecycle).
- ProGuard keeps the gomobile bridge classes reachable; everything else
  can be shrunk / obfuscated.
- DNS and IPv6 handling land with the engine integration — see
  `docs/memory/platform-notes.md` §Android.

## Not yet wired

- Profile storage (Room or DataStore + Jetpack Security) — placeholder.
- Subscription import UI — placeholder.
- Always-on + kill-switch messaging — depends on engine integration.
- Per-app routing UI — depends on `shared/gmvpn-core` routing module.
