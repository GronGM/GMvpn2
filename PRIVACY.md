# Privacy policy — GMvpn

_Last updated: 2026-04-30._

## What GMvpn does with your data

GMvpn is a VPN client. The only third-party network endpoints the app
contacts are the ones **you configure yourself** as profiles or
subscriptions. The app does **not**:

- send analytics, crash reports, or telemetry to anyone;
- contact GMvpn's authors;
- contact any cloud, store, or "phone home" service.

If a future version adds any network request that goes anywhere other
than your configured profile, that change must be announced in the
release notes and the policy below must be updated in the same
release.

## Data stored on your device

- The active profile URI (VLESS / VMess / Trojan / Shadowsocks),
  encrypted with an AES-256-GCM key kept inside Android Keystore. The
  raw URI never touches plain disk.
- Tunnel logs at runtime, kept in memory only. The app does not
  persist a log file.
- Cumulative byte counters for the active session, displayed in the
  ongoing notification. Reset to zero on every connect.

There is no cloud sync, no account, no signup. Uninstalling the app
removes everything.

## Permissions used

- `BIND_VPN_SERVICE` — required to establish the TUN device. Your
  traffic is routed through the engine inside the same process; it is
  not forwarded to anyone else.
- `FOREGROUND_SERVICE` (+ `_SYSTEM_EXEMPTED` type) — required by
  Android while the tunnel is up.
- `POST_NOTIFICATIONS` — for the ongoing tunnel notification.
- `INTERNET` and `ACCESS_NETWORK_STATE` — for the engine to talk to
  your configured server.

## Subscriptions

If you import a subscription URL, the app fetches the URL **only when
you ask it to**. The fetch goes directly to the URL host you typed —
no proxy, no intermediary, no caching service.

## Source code & verification

GMvpn is open source. The source for the build that produced your APK
is at `https://github.com/GronGM/GMvpn2`. Reproducible builds are
tracked in the release engineering roadmap.

## Contact

Open an issue at `https://github.com/GronGM/GMvpn2/issues` for
questions or concerns.
