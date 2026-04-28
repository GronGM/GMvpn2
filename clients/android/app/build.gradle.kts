plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    implementation(libs.jna) { artifact { type = "aar" } }

    // The Go Xray-core wrapper produced by `scripts/build-android-libs.sh`.
    // The `.aar` is not committed; if it isn't in app/libs/ the gomobile
    // classes (`com.gmvpn.core.*`) won't be on the classpath and engine
    // start will fail with a clear ClassNotFoundException at runtime.
    // Compile-time we tolerate its absence by talking to it through
    // reflection — see `tunnel/EngineBridge.kt`.
    implementation(fileTree("libs") { include("*.aar") })

    debugImplementation(libs.androidx.compose.ui.tooling)
}
