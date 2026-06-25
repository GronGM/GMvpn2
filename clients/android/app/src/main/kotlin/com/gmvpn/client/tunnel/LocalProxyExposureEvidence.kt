package com.gmvpn.client.tunnel

import java.net.InetAddress

internal enum class LocalProxyListenerState {
    Idle,
    Allocated,
    Starting,
    Running,
    Stopping,
    Stopped,
    Failed,
}

internal enum class LocalProxyBindClass {
    Loopback,
    NonLoopback,
    Unknown,
}

internal enum class LocalProxyPortClass {
    EphemeralRuntime,
    DefaultFallback,
    Unknown,
}

internal data class LocalProxyExposureSnapshot(
    val state: LocalProxyListenerState,
    val bindClass: LocalProxyBindClass,
    val portClass: LocalProxyPortClass,
    val generation: Long,
    val engineRunning: Boolean,
    val tunEstablished: Boolean,
) {
    fun redactedSummary(): String =
        "state=$state bindClass=$bindClass portClass=$portClass " +
            "generation=$generation engineRunning=$engineRunning tunEstablished=$tunEstablished"
}

/**
 * Tracks only safe local-proxy lifecycle evidence. It deliberately does
 * not store raw ports, endpoints, tokens, profiles, or subscription data.
 */
internal class LocalProxyExposureTracker {
    private var generation: Long = 0
    private var current = LocalProxyExposureSnapshot(
        state = LocalProxyListenerState.Idle,
        bindClass = LocalProxyBindClass.Unknown,
        portClass = LocalProxyPortClass.Unknown,
        generation = generation,
        engineRunning = false,
        tunEstablished = false,
    )

    fun markAllocated(bindClass: LocalProxyBindClass, portClass: LocalProxyPortClass) {
        generation += 1
        current = LocalProxyExposureSnapshot(
            state = LocalProxyListenerState.Allocated,
            bindClass = bindClass,
            portClass = portClass,
            generation = generation,
            engineRunning = false,
            tunEstablished = false,
        )
    }

    fun markStarting(tunEstablished: Boolean) {
        current = current.copy(
            state = LocalProxyListenerState.Starting,
            engineRunning = false,
            tunEstablished = tunEstablished,
        )
    }

    fun markRunning(tunEstablished: Boolean) {
        current = current.copy(
            state = LocalProxyListenerState.Running,
            engineRunning = true,
            tunEstablished = tunEstablished,
        )
    }

    fun markStopping() {
        current = current.copy(
            state = LocalProxyListenerState.Stopping,
            engineRunning = false,
        )
    }

    fun markFailed() {
        current = current.copy(
            state = LocalProxyListenerState.Failed,
            engineRunning = false,
        )
    }

    fun markStopped() {
        generation += 1
        current = LocalProxyExposureSnapshot(
            state = LocalProxyListenerState.Stopped,
            bindClass = LocalProxyBindClass.Unknown,
            portClass = LocalProxyPortClass.Unknown,
            generation = generation,
            engineRunning = false,
            tunEstablished = false,
        )
    }

    fun snapshot(): LocalProxyExposureSnapshot = current

    companion object {
        fun classifyBindAddress(bindAddress: String?): LocalProxyBindClass {
            if (bindAddress.isNullOrBlank()) return LocalProxyBindClass.Unknown
            return runCatching {
                if (InetAddress.getByName(bindAddress).isLoopbackAddress) {
                    LocalProxyBindClass.Loopback
                } else {
                    LocalProxyBindClass.NonLoopback
                }
            }.getOrDefault(LocalProxyBindClass.Unknown)
        }
    }
}
