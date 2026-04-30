# Release roadmap to Android v1

The repo currently has a working pipeline end-to-end on paper:
URI → `parseProfileUri` → `buildXrayConfig` → `VpnService.establish` →
gVisor netstack → SOCKS5 (TCP+UDP) → Xray-core. What's left between
"compiles" and "ship a v1 APK" is mostly hardening, UX, and release
engineering. This file tracks that gap, in priority order.

## P0 — must ship

These block calling anything "v1".

1. **End-to-end CI APK build green.** `.github/workflows/android-aar.yml`
   now has an `apk` job; the first push to a real GitHub remote will
   surface any compile-time issues we cannot validate locally without
   the Android SDK. Fix everything that lights up red.
2. **Real device validation.** Run the debug APK against a known-good
   VLESS+Reality server: connect, browse over IPv4, browse over IPv6,
   resolve a domain via UDP DNS, watch a 5-minute video to exercise
   the UDP relay under load. Capture a redacted `logcat` bundle if
   anything fails.
3. **Kill-switch / always-on UX.** Document in-app how the user opts
   in via Android system settings ("Always-on VPN" + "Block
   connections without VPN"); add a Settings entry that deep-links to
   the system page. Verify with airplane-mode toggle that no traffic
   leaks while the tunnel is "Reconnecting".
4. **DNS leak audit.** With the tunnel up, run a DNS-leak test
   (e.g. dnsleaktest.com) and a manual `nslookup` in `adb shell`.
   Confirm every query goes through the tunnel; if not, add a
   default DNS rule to the Xray config built by `gmvpn-core::xray`.
5. **IPv6 leak audit.** Same again with `test-ipv6.com`. Make IPv6
   either tunnel cleanly (current default) or be explicitly blocked
   per profile.
6. **Secure storage for profile credentials.** Move the `ProfileStore`
   off plain DataStore Preferences for the secret bits (UUID,
   password) and into Android Keystore + EncryptedSharedPreferences
   (or DataStore + `androidx.security.crypto`). The URI string itself
   is fine in plain DataStore; secrets aren't.
7. **Foreground notification UX.** Title shows "GMvpn — connected to
   `<profile name>`"; body shows uplink/downlink rates pulled from
   `Tunnel.Stats()`; tap returns to the app.
8. **Release signing + signed APK in CI.** Add a `release-signed`
   job that runs on tags; reads keystore from a GitHub secret;
   produces a signed APK uploaded as a release artifact. Document
   the secret rotation procedure.
9. **App icon.** Replace the placeholder shield drawable with a real
   icon (SVG → adaptive icon, all densities).
10. **Privacy policy + about screen.** One web page (privacy policy)
    plus an in-app About screen listing licenses (Xray-core MPL,
    UniFFI Apache, gVisor Apache, JNA Apache, etc.).

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
