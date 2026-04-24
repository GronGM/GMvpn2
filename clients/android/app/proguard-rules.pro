# Keep gomobile-generated bridge classes reachable from reflection.
-keep class com.gmvpn.core.** { *; }

# Keep our tunnel service entry so the system can start it by name.
-keep class com.gmvpn.client.tunnel.GmvpnVpnService { *; }
