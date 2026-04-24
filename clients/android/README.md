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

## UniFFI bindings (shared Rust core)

The Kotlin-side `uniffi.gmvpn_ffi` package is generated, not committed.
To refresh it locally:

```sh
cd ../../shared
make kotlin                      # → shared/bindings/kotlin/…/gmvpn_ffi.kt
```

Android packaging (producing an `.aar` with the native `.so` for all
four ABIs) is handled separately. Outline:

1. Install NDK + `cargo install cargo-ndk`.
2. Cross-compile `gmvpn-ffi` for `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`.
3. Drop each `libgmvpn_ffi.so` into `app/src/main/jniLibs/<abi>/`.
4. Copy the generated `gmvpn_ffi.kt` into `app/src/main/kotlin/uniffi/gmvpn_ffi/`.
5. Add the `net.java.dev.jna:jna:@aar` runtime dependency in
   `app/build.gradle.kts`.

This is tracked as future work; the Android client currently builds
without the bindings so the scaffolding is verifiable independently.

## Engine integration (next step)

1. Build `gmvpn.aar`: `cd ../../core && make gomobile-install && make android`.
2. Copy `core/build/gmvpn.aar` to `clients/android/app/libs/gmvpn.aar`.
3. Uncomment the `implementation(files("libs/gmvpn.aar"))` line in
   `app/build.gradle.kts` (the `TODO(engine)` marker).
4. In `GmvpnVpnService.handleStart()`, replace the `TODO(engine)`
   placeholder with:
   - parse the active profile via `uniffi.gmvpn_ffi.parseProfileUri(uri)`;
   - build Xray-core config JSON from the resulting `FfiProfile`;
   - call `Builder#establish()` with the addresses, routes, DNS, and MTU
     the profile demands;
   - `com.gmvpn.core.Gmvpn.new(listener).start(configJson, pfd.getFd())`;
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
