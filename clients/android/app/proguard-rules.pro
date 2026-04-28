# Keep gomobile-generated bridge classes — reached via reflection from
# EngineBridge, so R8 can't see the references.
-keep class com.gmvpn.core.** { *; }

# Keep the UniFFI-generated bindings. Their internal types are addressed
# from the native side via JNI/JNA names, R8 must not rename them.
-keep class uniffi.gmvpn_ffi.** { *; }

# JNA's structure / pointer machinery uses reflection over field names
# and method names. The library ships its own consumer rules, but these
# are explicit fallbacks.
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    <fields>;
    <methods>;
}

# Keep our tunnel service entry so the system can start it by name.
-keep class com.gmvpn.client.tunnel.GmvpnVpnService { *; }
