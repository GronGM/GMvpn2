package com.gmvpn.client.tunnel

import android.util.Log
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Java reflection wrapper around the Go gomobile-generated bindings
 * (`com.gmvpn.core.gmvpn.*` from `core/build/gmvpn.aar`). The `.aar`
 * is produced by `scripts/build-android-libs.sh` and dropped into
 * `app/libs/`; it is not committed to the repository.
 *
 * Compile-time the app does not depend on the gomobile types, so it
 * builds even before the artifact is in place. At runtime, [start]
 * looks up the classes via reflection and throws
 * [EngineUnavailableException] if they are missing — the user sees a
 * clear "engine not bundled" message instead of a confusing
 * `ClassNotFoundException` from the Compose layer.
 *
 * Stable surface mirrors the Go wrapper's
 * `gmvpn.Tunnel` interface and `gmvpn.StatusListener`:
 *   * `Gmvpn.New(StatusListener) -> Tunnel`
 *   * `Tunnel.Start(configJson String, tunFD int32, mtu int32, socksPort int32) error`
 *   * `Tunnel.Stop() error`
 *   * `Tunnel.Stats() (*TrafficStats, error)`
 *   * `StatusListener.OnStatusChanged(status String, detail String)`
 *
 * gomobile camel-cases method names: in Java these are
 * `Gmvpn.new_(...)`, `tunnel.start(...)`, etc. We use reflection so we
 * are not coupled to the exact bytecode Java method names — they
 * change subtly between gomobile versions.
 */
class EngineBridge {

    private val tunnelMu = Any()
    private var tunnelInstance: Any? = null

    fun start(
        configJson: String,
        tunFd: Int,
        mtu: Int,
        socksPort: Int,
        listener: (status: String, detail: String) -> Unit,
    ) {
        synchronized(tunnelMu) {
            if (tunnelInstance != null) {
                throw IllegalStateException("engine already running")
            }

            val gmvpnCls = lookupClass("com.gmvpn.core.gmvpn.Gmvpn")
            val statusListenerCls =
                lookupClass("com.gmvpn.core.gmvpn.Gmvpn\$StatusListener")
            val tunnelCls = lookupClass("com.gmvpn.core.gmvpn.Gmvpn\$Tunnel")

            // gomobile renames Go's `New` to a Java method on the package
            // class. Find a public static method that takes one argument
            // and returns a Tunnel.
            val newMethod = gmvpnCls.declaredMethods.firstOrNull { m ->
                java.lang.reflect.Modifier.isStatic(m.modifiers) &&
                    m.parameterCount == 1 &&
                    m.parameterTypes[0] == statusListenerCls &&
                    tunnelCls.isAssignableFrom(m.returnType)
            } ?: throw EngineUnavailableException(
                "gomobile signature changed: no New(StatusListener) on Gmvpn",
            )

            val proxy = Proxy.newProxyInstance(
                statusListenerCls.classLoader,
                arrayOf(statusListenerCls),
            ) { _, method, args ->
                if (method.name == "onStatusChanged" && args != null && args.size == 2) {
                    val status = args[0] as? String ?: ""
                    val detail = args[1] as? String ?: ""
                    runCatching { listener(status, detail) }
                        .onFailure { Log.w(TAG, "listener threw", it) }
                }
                null
            }

            val tunnel = newMethod.invoke(null, proxy)
                ?: throw EngineUnavailableException("Gmvpn.New returned null")

            val startMethod = findStartMethod(tunnelCls)
            try {
                startMethod.invoke(tunnel, configJson, tunFd, mtu, socksPort)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw EngineStartException("engine start failed", e.cause ?: e)
            }
            tunnelInstance = tunnel
        }
    }

    fun stop() {
        synchronized(tunnelMu) {
            val t = tunnelInstance ?: return
            tunnelInstance = null
            val stopMethod = t.javaClass.methods.firstOrNull { it.name == "stop" }
            if (stopMethod != null) {
                runCatching { stopMethod.invoke(t) }
                    .onFailure { Log.w(TAG, "engine stop threw", it) }
            }
        }
    }

    fun isRunning(): Boolean = synchronized(tunnelMu) { tunnelInstance != null }

    /**
     * Returns the Xray-core version exposed by the bundled gomobile
     * artifact, or null when `gmvpn.aar` is not on the classpath. This
     * is deliberately a soft probe: callers use it for UI/about text
     * and smoke tests, never as proof that a tunnel can connect.
     */
    fun xrayVersionOrNull(): String? {
        return try {
            val gmvpnCls = lookupClass("com.gmvpn.core.gmvpn.Gmvpn")
            val method = gmvpnCls.methods.firstOrNull {
                it.name == "xrayVersion" && it.parameterCount == 0
            } ?: return null
            (method.invoke(null) as? String)?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Cumulative byte counters since the current Start. Returns null
     * if the engine isn't running, the gomobile classes are missing,
     * or the call failed for any reason — callers must treat null as
     * "stats unavailable" rather than zero.
     */
    fun stats(): TrafficStats? {
        val t = synchronized(tunnelMu) { tunnelInstance } ?: return null
        val statsMethod = t.javaClass.methods.firstOrNull { it.name == "stats" }
            ?: return null
        return try {
            val raw = statsMethod.invoke(t) ?: return null
            TrafficStats(
                uplinkBytes = readLong(raw, "getUplinkBytes", "uplinkBytes"),
                downlinkBytes = readLong(raw, "getDownlinkBytes", "downlinkBytes"),
            )
        } catch (e: Throwable) {
            Log.w(TAG, "engine stats threw", e)
            null
        }
    }

    private fun readLong(target: Any, vararg candidates: String): Long {
        val cls = target.javaClass
        for (name in candidates) {
            val getter = cls.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
            if (getter != null) {
                return (getter.invoke(target) as? Long) ?: 0L
            }
        }
        // Fall back to a public field if gomobile exposes one.
        cls.fields.firstOrNull { c -> candidates.any { it == c.name } }?.let {
            return (it.get(target) as? Long) ?: 0L
        }
        return 0L
    }

    private fun lookupClass(name: String): Class<*> = try {
        Class.forName(name)
    } catch (_: ClassNotFoundException) {
        throw EngineUnavailableException(
            "engine class $name is missing — drop core/build/gmvpn.aar into app/libs/",
        )
    }

    private fun findStartMethod(tunnelCls: Class<*>): Method {
        // Tunnel.Start(configJSON String, tunFD int32, mtu int32, socksPort int32)
        // gomobile maps int32 → int. Match by parameter list rather than
        // exact name to survive gomobile renames.
        return tunnelCls.methods.firstOrNull { m ->
            m.parameterCount == 4 &&
                m.parameterTypes[0] == String::class.java &&
                m.parameterTypes[1] == Integer.TYPE &&
                m.parameterTypes[2] == Integer.TYPE &&
                m.parameterTypes[3] == Integer.TYPE
        } ?: throw EngineUnavailableException(
            "gomobile signature changed: no Tunnel.Start(String, int, int, int)",
        )
    }

    companion object {
        private const val TAG = "EngineBridge"
    }
}

/** Thrown when the gomobile `.aar` is not on the classpath. */
class EngineUnavailableException(message: String) : RuntimeException(message)

/** Thrown when the engine is on the classpath but Start failed. */
class EngineStartException(message: String, cause: Throwable) :
    RuntimeException(message, cause)
