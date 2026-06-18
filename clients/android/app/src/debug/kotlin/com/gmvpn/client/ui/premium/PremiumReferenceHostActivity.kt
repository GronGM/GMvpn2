package com.gmvpn.client.ui.premium

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class PremiumReferenceHostActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#050B12")
        window.navigationBarColor = Color.parseColor("#050B12")

        val screen = intent.getStringExtra(EXTRA_SCREEN).orEmpty()
        setContent {
            when (screen) {
                SCREEN_PROFILES -> PremiumProfilesReferencePreview()
                SCREEN_IMPORT -> PremiumImportReferencePreview()
                SCREEN_PRIVACY -> PremiumPrivacyReferencePreview()
                else -> PremiumHomeReferencePreview()
            }
        }
    }

    companion object {
        const val EXTRA_SCREEN = "screen"
        const val SCREEN_HOME = "home"
        const val SCREEN_PROFILES = "profiles"
        const val SCREEN_IMPORT = "import"
        const val SCREEN_PRIVACY = "privacy"
    }
}
