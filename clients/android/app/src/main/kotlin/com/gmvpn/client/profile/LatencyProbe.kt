package com.gmvpn.client.profile

import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TCP-handshake ping: open a TCP connection to `host:port`, time it,
 * close. Returns round-trip time in milliseconds, or null on timeout
 * / refused / unreachable.
 *
 * This is a *reachability + latency* probe, not a quality test:
 * - It measures the TCP-handshake RTT (one round-trip), not TLS or
 *   protocol negotiation. Real per-profile throughput differs.
 * - It runs through the system's current default network. If the
 *   tunnel is up the probe goes through the tunnel and reports the
 *   tunneled RTT — useful as a "is my session still healthy?" check
 *   but not an apples-to-apples server comparison.
 * - ICMP ping is intentionally avoided: it needs root on Android and
 *   many servers drop it.
 */
class LatencyProbe(private val timeoutMs: Int = DEFAULT_TIMEOUT_MS) {

    suspend fun probe(host: String, port: Int): Long? = withContext(Dispatchers.IO) {
        if (host.isBlank() || port <= 0 || port > 65_535) return@withContext null
        var socket: Socket? = null
        try {
            socket = Socket()
            val started = System.nanoTime()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000L
            elapsedMs
        } catch (_: Throwable) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000
    }
}
