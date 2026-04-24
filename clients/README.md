# /clients

Native client applications, one directory per platform. Each is a full app
project in its platform's conventional layout, linking in:

- `/shared/gmvpn-ffi` (Rust) — via UniFFI (Kotlin, Swift) or C-ABI
  (Windows, Linux).
- `/core` — Xray-core artifacts for the platform.

The first target slated for implementation is **Android** (see
`docs/memory/pending-decisions.md` §1). Other directories exist as
placeholders with platform-specific notes so the skeleton is consistent
and future setup is obvious.

| Platform | Toolchain                                           | Tunnel primitive                  |
|----------|-----------------------------------------------------|-----------------------------------|
| Android  | Gradle + Kotlin                                     | `android.net.VpnService`          |
| iOS      | Xcode + Swift                                       | `NEPacketTunnelProvider`          |
| macOS    | Xcode + Swift, System Extension                     | `NEPacketTunnelProvider` / SysExt |
| Windows  | MSBuild + WinUI 3 (or Qt), background service      | WinTUN                            |
| Linux    | CMake + Qt/GTK, systemd user+system split           | `/dev/net/tun`                    |

See `docs/memory/platform-notes.md` for constraints that must be respected
before writing code for each platform.
