plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
}

kover {
    // Generated UniFFI bindings + JNA helpers are vendored code; we
    // don't author them and they would skew the headline number.
    reports {
        filters {
            excludes {
                packages("uniffi.gmvpn_ffi", "uniffi.gmvpn_ffi.*")
            }
        }
    }
}

android {
    namespace = "com.gmvpn.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gmvpn.client"
        minSdk = 26
        targetSdk = 34
        versionCode = 1000001
        versionName = "1.0.0-rc.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    val releaseKeystoreFile = releaseKeystorePath
        ?.takeIf { it.isNotBlank() }
        ?.let { file(it) }
    val hasReleaseSigningConfig =
        releaseKeystoreFile?.isFile == true &&
            !releaseKeystorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            // Driven by env vars set in CI from GitHub secrets:
            //   RELEASE_KEYSTORE_BASE64    (decoded by CI only)
            //   RELEASE_KEYSTORE_PATH      (path to decoded/local keystore)
            //   RELEASE_KEYSTORE_PASSWORD
            //   RELEASE_KEY_ALIAS
            //   RELEASE_KEY_PASSWORD
            if (hasReleaseSigningConfig) {
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Only attach the signing config when the keystore env var
            // is set; without it `assembleRelease` falls back to an
            // unsigned APK so contributors who don't own the keystore
            // can still produce a release-shaped build for inspection.
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // JNA is required at runtime by the UniFFI Kotlin bindings (the
    // `gmvpn_ffi.kt` we ship under app/src/main/kotlin/uniffi/).
    // The `@aar` classifier picks the AAR variant that bundles the
    // native libs Android needs.
    implementation("net.java.dev.jna:jna:${libs.versions.jna.get()}@aar")

    // The Go Xray-core wrapper produced by `scripts/build-android-libs.sh`.
    // The `.aar` is not committed; if it isn't in app/libs/ the gomobile
    // classes (`com.gmvpn.core.*`) won't be on the classpath and engine
    // start will fail with a clear ClassNotFoundException at runtime.
    // Compile-time we tolerate its absence by talking to it through
    // reflection — see `tunnel/EngineBridge.kt`.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
