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
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Driven by env vars set in CI from GitHub secrets:
            //   RELEASE_KEYSTORE_PATH      (decoded from RELEASE_KEYSTORE_BASE64)
            //   RELEASE_KEYSTORE_PASSWORD
            //   RELEASE_KEY_ALIAS
            //   RELEASE_KEY_PASSWORD
            val ks = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!ks.isNullOrBlank()) {
                storeFile = file(ks)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
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
            val rsc = signingConfigs.getByName("release")
            if (rsc.storeFile != null) signingConfig = rsc
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
