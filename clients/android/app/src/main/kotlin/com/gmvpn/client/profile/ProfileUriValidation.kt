package com.gmvpn.client.profile

import java.util.Locale

private val supportedProfileSchemes = setOf("vless", "vmess", "trojan", "ss")

/**
 * Schemes we can parse and display, but that the bundled Xray engine cannot
 * actually carry traffic for yet (Hysteria2: pinned Xray has no obfs/Salamander
 * field and upstream bug XTLS/Xray-core#5921 drops the return path). Keeping
 * them out of [supportedProfileSchemes] avoids starting a dead "connected"
 * tunnel; [isEngineUnsupportedScheme] lets the UI show an honest message.
 */
private val engineUnsupportedSchemes = setOf("hysteria2", "hy2")

private fun schemeOf(uri: String): String =
    uri.substringBefore("://", missingDelimiterValue = "")
        .lowercase(Locale.ROOT)

fun hasSupportedProfileScheme(uri: String): Boolean =
    schemeOf(uri) in supportedProfileSchemes

/**
 * True for a recognised protocol that the current engine cannot tunnel. The UI
 * uses this to explain *why* a valid-looking profile will not connect, instead
 * of a misleading "invalid profile" error or a fake-green dead tunnel.
 */
fun isEngineUnsupportedScheme(uri: String): Boolean =
    schemeOf(uri) in engineUnsupportedSchemes
