# Release roadmap to Android v1

The repo currently has a working pipeline end-to-end on paper:
URI → `parseProfileUri` → `buildXrayConfig` → `VpnService.establish` →
gVisor netstack → SOCKS5 (TCP+UDP) → Xray-core. What's left between
"compiles" and "ship a v1 APK" is mostly hardening, UX, and release
engineering. This file tracks that gap, in priority order.

## P0 — must ship

These block calling anything "v1".

1. **End-to-end CI APK build green.** `.github/workflows/android-aar.yml`
   has the `libs + apk` chain. Once a real device run is captured the
   first push will tell us if anything in the Kotlin code I cannot
   validate locally is wrong. _Status: workflow lands; awaiting first
   green run._
2. **Real device validation.** Run the debug APK against a known-good
   VLESS+Reality server: connect, browse over IPv4, browse over IPv6,
   resolve a domain via UDP DNS, watch a 5-minute video to exercise
   the UDP relay under load. Capture a redacted `logcat` bundle if
   anything fails. _Blocking: device required._
3. ~~**Kill-switch / always-on UX.**~~ Done — `HomeScreen` shows an
   explainer card with a button that deep-links to
   `Settings.ACTION_VPN_SETTINGS`; `PRIVACY.md` and About cover the
   policy side. Airplane-mode airtime test still pending.
4. **DNS leak audit.** With the tunnel up, run a DNS-leak test
   (e.g. dnsleaktest.com) and a manual `nslookup` in `adb shell`.
   Confirm every query goes through the tunnel; if not, add a
   default DNS rule to the Xray config built by `gmvpn-core::xray`.
   _Blocking: device required._
5. **IPv6 leak audit.** Same again with `test-ipv6.com`. Make IPv6
   either tunnel cleanly (current default) or be explicitly blocked
   per profile. _Blocking: device required._
6. ~~**Secure storage for profile credentials.**~~ Done — `ProfileStore`
   wraps the URI in AES-256-GCM with the key kept in
   `AndroidKeyStore` (`KeystoreSecrets`, `gmvpn.profile.v1` alias).
7. ~~**Foreground notification UX.**~~ Done — title "GMvpn — `<profile>`",
   body "↑ rate · ↓ rate · totals" updated every 2s from
   `EngineBridge.stats()`.
8. ~~**Release signing + signed APK in CI.**~~ Done — workflow
   `android-release.yml` triggers on `v*.*.*` tags, decodes a
   keystore from `RELEASE_KEYSTORE_BASE64` secret, runs
   `assembleRelease`, attaches the APK + `.sha256` to a GitHub
   release. Required secrets documented at the top of the workflow.
9. ~~**App icon.**~~ Done — adaptive icon with shield + padlock
   foreground, monochrome variant for Android 13+ themed icons.
10. ~~**Privacy policy + about screen.**~~ Done — `PRIVACY.md` at
    repo root, in-app `AboutScreen` lists app/core/Xray-core
    versions, "no telemetry" statement, and the bundled
    open-source licenses.

## P1 — should ship

Strong UX bumps. Not strictly blocking, but the product feels rough
without them.

11. **Subscription import UI.** Decoder is in `gmvpn-core::subscription`;
    add a Compose screen to paste a subscription URL, fetch it (with
    HTTPS-only enforcement), present the parsed profile list with
    per-line warnings, and let the user pick which to save.
12. **Multi-profile management.** `ProfileStore` to a list (active
    profile + library), Compose list/detail screens, edit / delete /
    duplicate.
13. **Latency probe.** A `gmvpn-core` function that does a TCP-handshake
    ping to `profile.server:profile.port` and reports RTT; wire to a
    "Test" button on each profile card.
14. **Reconnect on network changes.** Listen for
    `ConnectivityManager.NetworkCallback`; on transition Wi-Fi ↔ mobile,
    call `tunnel.Stop()` and re-establish. Surface as `Reconnecting` in
    the UI.
15. **Diagnostics export.** A "Copy logs" button that grabs structured
    log lines from `gmvpn-core` plus the last N entries from logcat,
    redacts UUIDs/passwords, and writes a sharable `.zip`.
16. **Localization.** At minimum English + Russian. String catalog in
    `strings.xml` is already isolated.
17. **Per-app routing UI.** `gmvpn-core::routing` has the model;
    Android picker UI for "include" / "exclude" package lists.

## P2 — nice to have

18. **iOS / macOS clients.** Mirror the Android wiring against
    `NEPacketTunnelProvider`. Big track; new ADR.
19. **Windows client.** WinTUN + a small service. Big track; new ADR.
20. **Linux client.** Qt or GTK tray + systemd service. Big track.
21. **Auto-update.** Decide between Play Store, F-Droid, and
    self-update (currently in pending-decisions §7).
22. **Telemetry decision.** Currently no network calls home; confirm
    that stays the default before any "metrics" library is added.
23. **Routing rule presets.** GeoIP-based "China direct", "Russia
    direct" etc. Use Xray's built-in geosite/geoip data.

## Engineering quality (cross-cutting)

- **Android instrumented tests.** At least one connect/disconnect
  smoke test against a local mock SOCKS5 server. Runs on emulator in
  CI.
- **Coverage report.** `kover` for the Android module + `cargo
  llvm-cov` for the Rust crates; publish HTML report as a CI
  artifact.
- **Security review.** Once P0 is closed, run a dedicated review of
  the data path, secret storage, and IPC. Track findings in
  `docs/security-review-NN.md`.
- **Performance baseline.** Capture throughput numbers (TCP, UDP)
  on a real device + RTT vs. raw connection, before any optimisation
  work.

## Non-Android (lower priority but planned)

- iOS: ADR + scaffold. Reuse `gmvpn-core` and `core/gmvpn` (compile
  with `gomobile bind -target=ios`); write the
  `NEPacketTunnelProvider` host. Apple developer entitlement is the
  long lead time.
- macOS: similar; system-extension model.
- Windows: WinTUN + a Windows service; WinUI 3 or Qt UI.
- Linux: TUN device + systemd unit; Qt tray.

## Where each item lives

- `docs/memory/pending-decisions.md` keeps open architectural
  questions; closed ones move into ADRs.
- `docs/memory/platform-notes.md` carries platform-specific
  constraints discovered along the way.
- ADRs (`docs/adr/NNNN-*.md`) are write-once: a decision lands,
  rationale is recorded, and the file isn't rewritten.
- This roadmap is the only mutable list of remaining work; check
  items off here as they ship.
