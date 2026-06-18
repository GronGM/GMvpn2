package com.gmvpn.client.profile

import java.util.Locale

private val supportedProfileSchemes = setOf("vless", "vmess", "trojan", "ss")

fun hasSupportedProfileScheme(uri: String): Boolean =
    uri.substringBefore("://", missingDelimiterValue = "")
        .lowercase(Locale.ROOT) in supportedProfileSchemes
