package com.gmvpn.core.gmvpn

interface StatusListener {
    fun onStatusChanged(status: String, detail: String)
}

interface Tunnel {
    fun start(configJson: String, tunFd: Int, mtu: Int, socksPort: Int)
    fun stop()
}

object Gmvpn {
    @JvmStatic
    fun new_(listener: StatusListener): Tunnel =
        FakeGmvpnRegistry.nextTunnel ?: FakeGmvpnTunnel()

    @JvmStatic
    fun xrayVersion(): String = "test-xray"
}

object FakeGmvpnRegistry {
    var nextTunnel: Tunnel? = null

    fun reset() {
        nextTunnel = null
    }
}

class FakeGmvpnTunnel(
    private val startError: Throwable? = null,
) : Tunnel {
    var startCalls: Int = 0
        private set
    var stopCalls: Int = 0
        private set
    var lastSocksPort: Int? = null
        private set
    var running: Boolean = false
        private set

    override fun start(configJson: String, tunFd: Int, mtu: Int, socksPort: Int) {
        startCalls += 1
        lastSocksPort = socksPort
        startError?.let { throw it }
        running = true
    }

    override fun stop() {
        stopCalls += 1
        running = false
    }
}
