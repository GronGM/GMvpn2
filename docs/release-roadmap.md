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
   validate locally is wrong. _Status: workflow is configured; local
   release-readiness audit on 2026-06-15 rebuilt native artifacts,
   passed debug build/tests, lintDebug, physical connected tests,
   release APK, and release bundle. Android package metadata was bumped
   to `versionName` `1.0.0-rc.1` / `versionCode` `1000001` for RC
   packaging. Observe the next PR/main workflow before treating CI as
   independently green._
2. **Real device validation.** Run the debug APK against a known-good
   VLESS+Reality server: connect, browse over IPv4, browse over IPv6,
   resolve a domain via UDP DNS, watch a 5-minute video to exercise
   the UDP relay under load. Capture a redacted `logcat` bundle if
   anything fails. _Physical evidence recorded on 2026-06-15:
   TECNO LG8n reached stable Connected for 60s, browsed HTTPS, showed
   IPv4 egress, passed app-level disconnect/reconnect, and later passed
   browser-based DNS leak audit and Always-on/block-without-VPN
   lockdown validation, Wi-Fi/cellular handover, and UDP-heavy fallback
   validation. IPv6 was not applicable on that device/network because
   there was no underlying IPv6 default route; no public IPv6
   fall-through was observed. UDP-heavy used browser WebRTC/STUN plus a
   5-minute Chrome YouTube playback window because no controlled
   UDP/iperf target was configured; it does not measure controlled UDP
   throughput/loss. Baseline physical validation is complete for the
   RC3 candidate, but v1.0.0 approval still needs either controlled
   UDP/full DNS/real IPv6 evidence or an explicit release decision
   accepting those limitations. The final release-readiness audit passed
   as a release candidate state on
   2026-06-15 after fixing narrow R8/JNA release-shrinker rules and a
   lint-only foreground-service permission warning on the VPN service;
   this is not a production/public distribution claim. Details live in
   `docs/android-device-validation.md`,
   `docs/android-v1-validation-checklist.md`, and
   `docs/android-network-validation-bench.md`, and
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
8. **Release signing + signed APK/AAB in CI.** Manual packaging
   workflow `android-release.yml` is prepared for RC candidates: it
   runs only from `workflow_dispatch`, does
   not create git tags, does not publish a GitHub Release, builds
   unsigned audit artifacts, and requires all `RELEASE_KEYSTORE_*`
   secrets before producing signed RC artifacts as GitHub Actions
   artifacts. Required setup is documented in
   `docs/android-release-signing.md`. _Status: manual workflow run
   `27632339860` succeeded on 2026-06-16 from
   `claude/relaxed-euler-1Vr2R` at
   `1775829107eac1066af911353fc17f8d11f24a18`, producing signed APK/AAB
   artifacts and checksums. Local `apksigner` verification and SHA-256
   checksum verification passed. No git tag or GitHub Release was
   created; public distribution still requires explicit tag/release
   approval._
   _RC tag approval package: candidate `android-v1.0.0-rc.1`; artifact
   source SHA `1775829107eac1066af911353fc17f8d11f24a18`; docs/audit
   HEAD after artifact verification
   `a2fe00a5677665a44ab6b1396a50acf2e28f0d42`; workflow run
   `https://github.com/GronGM/GMvpn2/actions/runs/27632339860`
   (`27632339860`); APK/AAB signed: yes; APK signature verified: yes;
   checksums verified: yes; secrets exposed: no. Tag/release still
   require explicit approval._
   _RC2 candidate evidence: run
   `https://github.com/GronGM/GMvpn2/actions/runs/27640095772`
   (`27640095772`) succeeded on 2026-06-16 from
   `codex/p1-play-compliance-and-device-validation` at
   `4d15f3054384cd6a1ee7ae954491ade0e7a98370`, producing signed
   `versionName` `1.0.0-rc.2` / `versionCode` `1000002` APK/AAB
   artifacts. CI and local download verification passed checksums,
   APK signature verification, AAB jar verification with expected
   self-signed/untimestamped certificate warnings, signed APK/AAB
   16 KB ELF checks, signed APK `zipalign -P 16`, and APK metadata
   (`minSdk` 26 / `targetSdk` 35). RC1 tag is unchanged; RC2 tag and
   GitHub Release were not created._
   _RC2 physical validation update: a 2026-06-16 signed APK attempt on
   a physical Android 12/API 31 device installed and launched, showed
   correct About metadata, and did not crash. It did not pass release
   validation: cancelling the Android VPN permission dialog left the UI
   stuck at `Preparing` with `Disconnect` visible, invalid-profile
   failure did not remain persistently user-visible in the captured UI,
   and no approved real VPN profile/server was used. DNS/IPv4 route,
   controlled UDP/iperf, and real IPv6 checks remain pending. RC2 tag
   and GitHub Release were still not created._
   _RC3 blocker-cleanup candidate: workflow run
   `https://github.com/GronGM/GMvpn2/actions/runs/27643689894`
   (`27643689894`) succeeded on 2026-06-16 from
   `codex/p1-play-compliance-and-device-validation` at
   `dd10df9d3683fa41ccc628e5db0c186d029dd6ae`, producing signed
   `versionName` `1.0.0-rc.3` / `versionCode` `1000003` APK/AAB
   artifacts. CI and local download verification passed checksums,
   APK signature verification, AAB jar verification with expected
   self-signed/untimestamped certificate warnings, signed APK/AAB
   16 KB ELF checks, signed APK `zipalign -P 16`, and APK metadata
   (`minSdk` 26 / `targetSdk` 35). After explicit approval, RC3
   annotated tag `android-v1.0.0-rc.3` was created and pushed with tag
   object `65f3f0bd0d99a284291f178e4ac326300dc8d353` targeting
   `dd10df9d3683fa41ccc628e5db0c186d029dd6ae`. GitHub Release was not
   created. Physical validation was rerun on physical TECNO LG8n
   (Android 12/API 31): permission cancel, invalid-profile persistent
   error UX, VPN permission allow, valid profile connect/disconnect,
   reconnect cycles, app restart, basic browsing, IPv4 route, and a
   short network-change check passed with redacted evidence. DNS
   evidence is pass-limited, UDP/iperf was not tested, and real
   external IPv6 was not validated. No emulator was used._
   _RC3 tag approval package: candidate `android-v1.0.0-rc.3`;
   artifact source SHA
   `dd10df9d3683fa41ccc628e5db0c186d029dd6ae`; validation docs HEAD
   `560b82976f80fbef4b46d669e097968471bcbb3d`; workflow run
   `https://github.com/GronGM/GMvpn2/actions/runs/27643689894`
   (`27643689894`); APK/AAB signed: yes; SDK35: yes; 16 KB: yes;
   physical Android install/connect/disconnect/reconnect: yes; DNS:
   pass-limited; IPv4: pass; UDP/iperf: not tested; IPv6: not tested;
   log privacy: pass; GitHub Release: not authorized; RC3 tag created.
   Existing signed RC3 artifacts are tied to tag
   `android-v1.0.0-rc.3`, which points to
   `dd10df9d3683fa41ccc628e5db0c186d029dd6ae`, not the docs commit.
   Approval phrase used:
   `APPROVE RC TAG android-v1.0.0-rc.3 ON dd10df9d3683fa41ccc628e5db0c186d029dd6ae WITH UDP_IPV6_LIMITATIONS_ACCEPTED`._
   _RC3 tester pre-release: GitHub Pre-release
   `https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3`
   was created for manual APK testing. It is marked prerelease, uses
   the existing RC3 tag, and includes only
   `GMvpn-android-v1.0.0-rc.3.apk` plus
   `GMvpn-android-v1.0.0-rc.3.apk.sha256`. No AAB, Google Play
   publication, production/latest release, or `android-v1.0.0` tag was
   created. Known limits remain DNS `pass-limited`, UDP/iperf not
   tested, and IPv6 not tested._
   _RC4 privacy pre-release: source metadata is `versionName`
   `1.0.0-rc.4` / `versionCode` `1000004`. Privacy fix commit
   `c6f635211a698c75df904152cbe0e3cb39f2e730` removes server
   IP/host/domain/port, UUID/password/raw URI/base64/query-like secret
   data from normal saved profile labels. Workflow run `27672658765`
   succeeded from artifact source SHA
   `1b99d5abc1a693584519eb201c49c466ca13a782`; APK/AAB signature,
   checksum, 16 KB alignment, zipalign, and metadata checks passed.
   After explicit approval, annotated tag `android-v1.0.0-rc.4` was
   created and pushed with tag object
   `86c4a5158ae9c784d5ad97bbee251e5e4b1444a5` targeting
   `1b99d5abc1a693584519eb201c49c466ca13a782`. GitHub Pre-release
   `https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.4`
   is published for manual APK testing and includes only
   `GMvpn-android-v1.0.0-rc.4.apk` plus
   `GMvpn-android-v1.0.0-rc.4.apk.sha256`; AAB is not uploaded for
   testers. RC3 tag/release assets are unchanged; final
   `android-v1.0.0` is not authorized. Known limits remain DNS
   `pass-limited`, UDP/iperf not tested, and IPv6 not tested._
   _RC5 profile/import/diagnostics UX pre-release: source metadata is
   `versionName` `1.0.0-rc.5` / `versionCode` `1000005`. RC5 keeps the
   RC4 saved-profile privacy fix and publishes a tester APK for safe
   profile management: safe list/details labels, active selection,
   rename, delete confirmation, active-profile reset after deleting the
   active entry, safe import preview, redacted diagnostics, and the
   network validation bench docs. Workflow run `27679203026` succeeded
   from artifact source SHA
   `15d0a7f5fd691f9bf517a05ac867fc661be8c233`; APK/AAB signature,
   checksum, `bundletool validate`, 16 KB alignment, zipalign, and
   metadata checks passed. After explicit approval, annotated tag
   `android-v1.0.0-rc.5` was created and pushed with tag object
   `16503777e38328d890ee78e47b27f46778f72e13` targeting the artifact
   source SHA. GitHub Pre-release
   `https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.5`
   is published for manual APK testing and includes only
   `GMvpn-android-v1.0.0-rc.5.apk` plus
   `GMvpn-android-v1.0.0-rc.5.apk.sha256`; AAB is not uploaded for
   testers. Final `android-v1.0.0` is not authorized. Known limits are
   DNS `pass-limited`, Android-side UDP/iperf `pass-limited`, and IPv6
   not tested. On 2026-06-17 the network validation runbook was refined
   with redacted evidence templates, an approved-endpoint gate for
   controlled UDP/iperf, a two-method DNS audit template, and a real
   IPv6/fail-closed template. This prepares the next validation sprint
   but does not change any pass/fail status. Post-RC5 source hardening
   also tightened diagnostics redaction so profile URIs collapse without
   endpoint data and free-form text masks HTTP URLs, IPv4 addresses,
   and host/destination context. Public RC5 APK assets were not
   changed; delivering this code to testers requires a later version
   bump, signed workflow, artifact checks, and explicit RC approval._
   _Post-RC5 validation tooling was added on 2026-06-17:
   `scripts/validation/preflight-windows.ps1` and
   `scripts/validation/run-network-validation-windows.ps1`. Preflight
   found `adb` through the standard Android SDK platform-tools path and
   an authorized physical device, with the serial masked in console
   output. Later follow-ups installed trusted `iperf3` tooling through
   WinGet, hardened the scripts to find a user portable install, and
   configured a controlled VPS endpoint with `iperf3-gmvpn.service`,
   TCP/UDP firewall rules, and a rotated SSH password. Windows to VPS
   TCP/UDP endpoint connectivity passes with endpoint details redacted,
   including a 30-second 5M UDP run with 0% packet loss and 4.249 ms
   jitter. A later Android-side RC5 run installed official Termux
   `v0.119.0-beta.3`, verified its APK SHA-256, installed `iperf3` 3.21,
   imported an approved subscription, and ran a controlled UDP matrix
   through active GMvpn on TECNO LG8n Android 12/API 31. With payload
   1200 bytes and 30-second runs, 1M x3 had 0% loss, 3M x3 had max
   0.011% loss, and 5M x3 had max 0.096% loss / max 2.477 ms jitter;
   2M x3 had one high-loss outlier up to 43%. GMvpn stayed connected
   before and after every run, and endpoint/profile/subscription details
   were redacted. A post-matrix logcat tail scan found no case-sensitive
   GMvpn crash/ANR markers. UDP is now Android-side `pass-limited`, not
   unrestricted `pass`. DNS remains pass-limited after two Android-side
   resolver-discovery methods without private/router DNS but without
   provider/country attribution or browser DNS leak page evidence. IPv6
   remains not tested because no real external IPv6 baseline was
   collected. No release assets or tags were changed._
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
    2026-06-15 physical TECNO LG8n validation passed Wi-Fi to cellular
    and cellular back to Wi-Fi with the tunnel remaining Connected,
    HTTPS/IPv4 browser checks passing after each transition, and
    post-handover disconnect/reconnect working. UDP-heavy fallback
    validation is also recorded in the Android v1 checklist, with no
    controlled iperf throughput/loss measurement. Final
    release-readiness audit passed as a release candidate state on
    2026-06-15; signed RC artifacts were produced on 2026-06-16, and
    public distribution still requires an explicit tag/release decision.
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

## Android Play compliance and post-RC validation

Post-RC Play compliance and device-validation work is tracked in
`docs/android-play-compliance-and-validation.md`: targetSdk 35+
migration, Play Console VpnService declaration, signed release APK
physical validation, controlled UDP/iperf validation, real IPv6
validation, 16 KB native page-size readiness, and source-to-artifact
traceability.
_Status: compileSdk/targetSdk 35 migration passed local unit/lint/
debug/release build checks on 2026-06-16. Android 15 FGS/VpnService
audit found no `dataSync`/`mediaProcessing` service type and no boot
auto-start path. 16 KB native page-size source pipeline fix passed
local release APK/AAB verification on 2026-06-16: all 23 packaged
native libraries in both artifacts had minimum LOAD align `0x4000`,
and `zipalign -c -P 16` passed for the release APK. This is post-RC/P1
work; existing RC1 signed artifacts are unchanged. Post-RC/P1 source
now has signed RC3 candidate evidence from workflow run `27643689894`
with `versionCode` `1000003`, `versionName` `1.0.0-rc.3`, SDK 35, and
16 KB native readiness verified on signed APK/AAB. The earlier
2026-06-16 signed RC2 physical-device attempt installed and launched
but failed release validation because the VPN permission cancel path
left the UI stuck at `Preparing`, invalid-profile error UX was not
persistently visible in the captured UI, and no approved real VPN
profile/server was used. RC3 tag is now created; GitHub Release and
final `android-v1.0.0` tag are not created. Signed RC3 physical
validation is pass-limited on physical TECNO LG8n with the
release-blocking permission cancel and invalid-profile UX paths fixed.
Controlled UDP/iperf is blocked by missing approved endpoint evidence,
full DNS remains pass-limited for signed RC3, and real external IPv6
is not tested. An unrestricted v1.0.0 approval should block on those
items; an MVP release needs explicit acceptance of the remaining
network-validation limitations. GitHub Actions Node 24 maintenance was
prepared in commit `9786fe3fa23080b8c9aff80f8e26e88bd38f87fc` by
updating official `actions/*` refs and `android-actions/setup-android`
to Node 24-compatible major versions. Proof workflow run
`27648312721` succeeded from
`5a7aca93e34dac3aa606711806669af75a99d067` with no Node 20
deprecation annotation or log match. Unrestricted v1.0.0 remains
blocked by UDP/full-DNS/IPv6 evidence gaps and the absence of a final
signed `1.0.0` workflow from a release source SHA. MVP v1.0.0 requires
explicit acceptance phrase
`APPROVE MVP V1.0.0 WITH UDP_DNS_IPV6_LIMITATIONS_ACCEPTED`; strict
release requires
`APPROVE UNRESTRICTED V1.0.0 AFTER UDP_DNS_IPV6_PASS`. A 2026-06-17
strict-path attempt now has Android-side UDP evidence through Termux
`iperf3` over active GMvpn RC5. The best stable matrix row was 5M with
payload 1200 bytes, three 30-second runs, max packet loss 0.096%, and
max jitter 2.477 ms; however UDP remains `pass-limited` because no formal
release loss threshold has been approved and the 2M row had one high-loss
outlier. DNS remains `pass-limited`: two Android-side
resolver-discovery methods ran while GMvpn stayed connected and did not
show private/router DNS, but provider/country attribution and browser DNS
leak page evidence were not completed. No real external IPv6 validation
was run.
MVP/internal path is document-ready for approval review, but not
approved. RC4 uses `versionCode` `1000004` /
`versionName` `1.0.0-rc.4` for the saved-profile privacy fix. RC5 is
published as a GitHub Pre-release tester APK with `versionCode`
`1000005` / `versionName` `1.0.0-rc.5` for profile management, safe
import preview, and redacted diagnostics UX validation. Final `1.0.0`
preparation remains
plan-only and must use a later Android `versionCode` than RC5, then run
`android-release.yml` with
`rc_tag=android-v1.0.0` from the exact final release source SHA, verify
signed artifacts, and only then create a final tag or GitHub Release
after explicit approval. The 2026-06-17 network evidence-plan update
added templates only. Later 2026-06-17 scripts restored repeatable
Windows preflight, VPS setup made a redacted controlled endpoint
available, and Android-side Termux `iperf3` produced RC5 UDP matrix
evidence. UDP/full DNS/IPv6 remain open for unrestricted production:
UDP is `pass-limited`, DNS is `pass-limited`, and IPv6 is `not_tested`._

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
