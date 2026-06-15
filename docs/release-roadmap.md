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
   anything fails. _Partial physical evidence recorded on 2026-06-15:
   TECNO LG8n reached stable Connected for 60s, browsed HTTPS, showed
   IPv4 egress, passed app-level disconnect/reconnect, and later passed
   browser-based DNS leak audit and Always-on/block-without-VPN
   lockdown validation. IPv6 was not applicable on that device/network
   because there was no underlying IPv6 default route; no public IPv6
   fall-through was observed. Still blocking: Wi-Fi/cellular reconnect
   and UDP-heavy validation. Details live in
   `docs/android-device-validation.md`,
   `docs/android-v1-validation-checklist.md`, and
   `scripts/collect-android-diagnostics.sh`._
3. ~~**Kill-switch / always-on UX.**~~ Done — `HomeScreen` shows an
   explainer card with a button that deep-links to
   `Settings.ACTION_VPN_SETTINGS`; `PRIVACY.md` and About cover the
   policy side. _Status: 2026-06-15 TECNO LG8n system VPN settings
   exposed Always-on and Block connections without VPN for GMvpn.
   After `GmvpnVpnService` handled Android's `android.net.VpnService`
   system start action, Always-on started the tunnel, lockdown allowed
   HTTPS only while the VPN was active, and force-stopping the VPN app
   blocked browser traffic outside the tunnel._
4. **DNS leak audit.** With the tunnel up, run a DNS-leak test
   (e.g. dnsleaktest.com) and a manual `nslookup` in `adb shell`.
   Confirm every query goes through the tunnel; if not, add a
   default DNS rule to the Xray config built by `gmvpn-core::xray`.
   _Status: 2026-06-15 TECNO LG8n browser audit passed with redacted
   evidence; no local mobile/Wi-Fi ISP resolver observed._
5. **IPv6 leak audit.** Same again with `test-ipv6.com`. Make IPv6
   either tunnel cleanly (current default) or be explicitly blocked
   per profile. _Status: 2026-06-15 TECNO LG8n network had no
   underlying IPv6 default route; browser observed no public IPv6 while
   VPN was active. Re-run on an IPv6-capable network before claiming
   IPv6 tunneling support._
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

11. ~~**Subscription import UI.**~~ Done — `SubscriptionFetcher`
    (HTTPS-only, redirects refused, 2 MiB cap) + UniFFI
    `decodeSubscriptionUris` feed `ProfileStore.replaceAll`. Compose
    card on the home screen takes a URL, format dropdown
    (uri-list / base64-uri-list / SIP008), and reports
    "imported N · skipped M".
12. ~~**Multi-profile management.**~~ Done — `ProfileStore` now holds
    an encrypted library with an `activeIndex`. Library card on
    HomeScreen shows each saved profile with a radio button + remove
    + clear-all. Migration from the previous single-URI key is
    automatic on first read. Detail / edit screens are a P1 polish
    item still pending.
13. ~~**Latency probe.**~~ Done — `LatencyProbe` opens a TCP socket to
    `profile.server:profile.port` with a 3-second timeout and reports
    the handshake RTT. Each library card row has a Test button; the
    library header has Test-all. Implemented on the Android side
    rather than in `gmvpn-core` to keep the shared core network-free
    per ADR / project-context. Live caveat: the probe goes through
    whatever the system's current default network is — when the
    tunnel is up that means the probe measures the tunneled RTT.
14. ~~**Reconnect on network changes.**~~ Done — `GmvpnVpnService`
    registers a `ConnectivityManager` default-network callback after
    a successful start. On `onLost` the status flips to
    `Reconnecting`; on the next `onAvailable` (or any default-network
    swap) the service stops the engine + bridge, closes the
    `ParcelFileDescriptor`, and runs `bringTunnelUp()` again. All
    tunnel-touching paths share a `Mutex` so an explicit Start /
    Stop and an in-flight reconnect cannot interleave. Physical
    Wi-Fi/cellular handover validation is still pending in the Android
    v1 checklist.
15. ~~**Diagnostics export.**~~ Done — `DiagnosticsCollector` builds a
    redacted blob with app/core/Xray/device meta, current tunnel
    status + last error, library entries (URIs redacted via
    `Redactor`: UUID → `<uuid>`, trojan password → `<pw>`, ss
    userinfo → `<ss-userinfo>`, Reality `pbk`/`sid`/`spx` → masked),
    last latencies, and the tail of self-process logcat.
    `MainActivity` writes it to `cacheDir/diagnostics/<ts>.txt`,
    serves via `FileProvider`, and fires an `ACTION_SEND` chooser.
    The "Export diagnostics" button lives under About so it doesn't
    clutter the connect path. No READ_LOGS permission needed: post
    Android 4.1 apps see only their own logcat output.
16. ~~**Localization.**~~ Done for Russian — `values-ru/strings.xml`
    covers every key in `values/strings.xml` (47 strings, parity
    verified). Adding more locales is a copy-translate-PR job; the
    string surface is stable.
17. ~~**Per-app routing UI.**~~ Done — `PerAppRouting` model
    (`Off / IncludeOnly / ExcludeListed` + package set),
    `PerAppRoutingStore` (DataStore, plain — package names are not
    secret), `InstalledAppsLoader` (PackageManager,
    user-visible apps + updated system apps, sorted by label, self
    filtered out), and a Compose screen with mode picker, search
    field, and a checkable lazy list. `GmvpnVpnService.establishTun`
    branches on the mode: `Off` only excludes self,
    `IncludeOnly` calls `addAllowedApplication` for each selected
    package (empty list falls back to `Off` so the user never lands
    on a no-op tunnel), `ExcludeListed` excludes self plus each
    selected package. Bilingual strings (en + ru); parity verified.

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

- **Android JVM unit tests** — first wave landed under
  `app/src/test/kotlin`: `RedactorTest` (URI / log redaction surface
  — the worst possible failure mode for diagnostics export),
  `PerAppRoutingStoreTest` (wire-format round-trip + deterministic
  ordering), `TunnelStatusTest` (engine-string → typed mapping
  contract). Wired into `:app:testDebugUnitTest` in the
  `android-aar.yml` apk job; HTML test reports uploaded as artifact.
- **Android instrumented tests** — first emulator/device smoke scaffold
  landed in `app/src/androidTest/kotlin`: `VpnTunnelSmokeTest` checks
  the private `VpnService` manifest contract, `VpnService.prepare`
  boundary, `EngineBridge` availability, non-empty `XrayVersion()`
  when `gmvpn.aar` is bundled, idle disconnect, and the no-active-profile
  start path. Run manually with `:app:connectedDebugAndroidTest`; it is
  not mandatory CI yet and does not fake a successful VPN connection.
- **Dependency vulnerability scanning** — `cargo audit` runs in
  `shared.yml`, `govulncheck ./...` runs in `core.yml`. Both are
  `continue-on-error: true` so a freshly-published advisory cannot
  block a release; findings still appear in the run log and need a
  follow-up commit. Android module's deps tracked by Dependabot.
- **Dependabot** — `.github/dependabot.yml` watches four ecosystems
  weekly: cargo (`/shared`), gomod (`/core`), gradle (`/clients/android`),
  github-actions (`/`). Each PR is auto-labelled by language and
  capped at 5 in flight per ecosystem so the queue stays reviewable.
  Pairs with the advisory scanners above: a new CVE shows up both
  as an in-log advisory and as an open PR with the bump ready.
- **Coverage reports** — landed across all three workflows.
  `cargo llvm-cov` runs in `shared.yml`; `go test -coverprofile`
  + `cover -html` runs in `core.yml`; `kover` runs in the
  `android-aar.yml` apk job (`:app:koverHtmlReportDebug`,
  generated UniFFI / JNA helpers excluded). Each workflow uploads
  an HTML report as `gmvpn-{shared,core,android}-coverage-<sha>`
  with 14-day retention. Local Go coverage today: 36% overall —
  pure-logic paths covered, engine-integration paths intentionally
  not.
- **Security review.** First-pass self-review landed in
  `docs/security-review-001.md` (data path, secret storage, IPC,
  diagnostics export, CI). Six concrete TODOs surfaced; the most
  pressing is randomising the SOCKS inbound port. A real
  third-party review must run before any public-store release.
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
