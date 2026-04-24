# Platform notes

Snapshot of platform-specific constraints. Update as decisions are confirmed.

## Android

- Tunnel: `android.net.VpnService`. Establishes a TUN fd that is handed to
  Xray-core. No root required.
- Xray-core: built as an Android `.aar` (gomobile bind) or shipped as a
  native `.so` loaded via JNI. Current leaning: gomobile bind for the first
  iteration, switch to `.so` + C-ABI if binary size or startup becomes an
  issue.
- Background: foreground service with a persistent notification is
  mandatory while the tunnel is up.
- Always-on VPN + kill switch: use the system "Always-on VPN" + "Block
  connections without VPN" settings. The app must handle
  `ACTION_VPN_PREPARE` and survive Doze.
- DNS: set via `VpnService.Builder#addDnsServer`. IPv6 handled per profile
  — disable in tunnel config if profile has no IPv6 outbound.
- Storage: encrypted shared preferences (Jetpack Security) for profiles;
  Android Keystore for any persisted secrets.

## iOS

- Tunnel: `NEPacketTunnelProvider` in a separate Network Extension target.
  Requires the `com.apple.developer.networking.networkextension`
  entitlement with the `packet-tunnel-provider` value, which requires a
  paid developer account and an approved request from Apple.
- Xray-core: must be compiled for iOS (arm64, arm64 simulator, x86_64
  simulator) and linked into the extension. Size budget in the extension
  is tight (~50 MB before throttling).
- Communication main app ↔ extension: `NETunnelProviderManager` +
  app group (`group.<bundle-id>`) for shared config.
- Kill switch: set `includeAllNetworks = true` and `excludeLocalNetworks`
  according to user preference. On iOS 14+ this is the correct primitive.
- Background: extension is managed by the system, not the app. Main app
  can be killed; tunnel persists.
- Storage: Keychain with `kSecAttrAccessibleAfterFirstUnlock` for secrets,
  shared via app group.

## macOS

- Tunnel: `NEPacketTunnelProvider` inside a System Extension (preferred on
  macOS 11+) or legacy Network Extension. System Extension requires user
  approval in System Settings on first run.
- Distribution: outside the Mac App Store if we need flexible entitlements
  and bundled Xray-core; Developer ID + notarization is mandatory.
- Otherwise very similar to iOS NEPacketTunnelProvider.

## Windows

- Tunnel: WinTUN driver (user-mode adapter, no kernel driver signing pain
  for us) plus a background Windows Service running as LocalSystem for
  route and DNS management.
- Elevation: the GUI runs as the user; the service handles privileged
  networking operations over a local IPC channel (named pipe, ACL-locked
  to the user's SID).
- DNS: set per-interface via `netsh` or Win32 APIs; NRPT rules for
  split-DNS scenarios.
- Auto-start: service set to automatic; GUI launched from the user
  session.
- Installer: MSIX (modern, sandboxed) preferred; fall back to MSI via
  WiX if MSIX limitations bite.

## Linux

- Tunnel: TUN device via `/dev/net/tun`. Process needs `CAP_NET_ADMIN`.
  Options: run a systemd service as root, or a setuid helper, or
  Polkit-elevated helper. Leaning: systemd user-system split — a
  system-level service for network config, a user-level tray app that
  talks to it over a Unix domain socket.
- DNS: `systemd-resolved` where available (`resolvectl`), fall back to
  editing `/etc/resolv.conf` with a restore-on-exit guard.
- Packaging: `.deb`, `.rpm`, and Flatpak. AppImage as a convenience
  build.
- Distro variance is real — test on at least Ubuntu LTS, Fedora, and
  Arch before calling Linux "supported".

## Cross-cutting

- **IPv6 leak:** every platform must either route IPv6 through the tunnel
  or block it outside the tunnel. There is no acceptable "leave IPv6
  alone" default.
- **DNS leak:** never rely on the OS's default resolver while the tunnel
  is up. Route DNS via the tunnel's DNS servers or via DoH/DoT configured
  in Xray-core.
- **LAN access:** user-configurable. Default: allow LAN, block WAN outside
  the tunnel (classic "block connections without VPN").
- **Captive portal:** each platform needs a documented strategy for
  letting the user authenticate to a captive portal without dropping
  kill-switch guarantees unexpectedly.
