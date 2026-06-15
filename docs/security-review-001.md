# Security review 001 — pre-Android-v1

- Date: 2026-05-06
- Scope: everything currently in the repo on `claude/relaxed-euler-1Vr2R`,
  excluding device-validation work (live tunnel, leak audits) which is
  tracked separately in `release-roadmap.md` P0 #2/#4/#5.
- Reviewer: self-review by the implementing engineer. A real
  third-party review must run before any public-store release.

## Threat model in one paragraph

The product is a VPN client. Adversaries we care about:

1. **Network observer** between the device and the user's chosen VPN
   server (ISP, hotel Wi-Fi, hostile state). Should learn nothing
   beyond the obfuscated outer flow.
2. **Local malware** on the same device, sandboxed in another app.
   Must not be able to read profile credentials, route its own
   traffic through a foreign user's tunnel, or pivot through GMvpn's
   IPC.
3. **A privileged shell user / forensic image** of the device after
   GMvpn has been running. Should find no profile credentials in
   plain on-disk state.
4. **A third-party log recipient** the user shares diagnostics with.
   Must never receive credentials, UUIDs, or precise server identity.

Out of scope: physical attacker with TEE break, supply-chain attack
on Xray-core / gVisor / UniFFI / JNA / Kotlin / Compose / AGP, the
user's chosen VPN server itself.

## Per-component findings

Format: **[ok]** done well, **[note]** acceptable but worth tracking,
**[gap]** real issue with mitigation noted.

### Profile storage (`com.gmvpn.client.profile`)

- **[ok]** Active URI is encrypted with AES-256-GCM
  (`AndroidKeyStore` alias `gmvpn.profile.v1`,
  `setRandomizedEncryptionRequired(true)`). Raw key bytes never
  leave the keystore.
- **[ok]** Wire format is `[12-byte IV || ciphertext+tag]` Base64.
  GCM authenticated encryption protects against ciphertext tampering.
- **[ok]** Legacy plain-text key `active_profile_uri_v2` is migrated
  on first read and then deleted; old plaintext copies are not
  retained.
- **[note]** `KeystoreSecrets.decrypt` swallows every exception as
  `null`. A corrupted entry surfaces to the UI as "no profile saved"
  — desired UX, but a determined attacker who can mutate DataStore
  can force the user to re-enter the URI. Mitigation: when we add
  diagnostic logging, distinguish "decrypt failed" from "no entry".
- **[gap]** Backup / device-transfer rules deny everything for the
  whole app, but only `allowBackup=false` enforces this on
  Android 11+. On 10 and below the encrypted blob may end up in a
  cloud backup; the key won't, so the blob is useless without the
  key — defence-in-depth, not a leak. Acceptable given minSdk 26.

### KeystoreSecrets

- **[ok]** Uses GCM, randomized IV per encrypt, 256-bit key, no
  hardcoded passphrase.
- **[note]** Key isn't `setUserAuthenticationRequired(true)`. We
  could gate decrypt on biometrics, but doing so behind the scenes
  silently breaks long-running services. Defer to a future "lock
  GMvpn behind biometrics" feature.

### Subscription import

- **[ok]** `SubscriptionFetcher` rejects non-HTTPS URLs and refuses
  redirects (3xx → typed error). The user must paste the final
  HTTPS URL.
- **[ok]** Hard 2 MiB body cap; 15 s connect / 30 s read timeouts
  bound resource use against a malicious server.
- **[ok]** Decoder never dereferences invalid lines — they become
  warnings, not exceptions. One bad URI doesn't poison the import.
- **[ok]** Successful subscription contents are now staged in a
  transient `PendingImport` and only replace the library after the
  user confirms a redacted preview dialog. A hostile subscription
  URL can no longer silently rotate the saved library.

### Per-app routing

- **[ok]** Self is always excluded so the engine's loopback SOCKS
  inbound stays reachable; without this, IncludeOnly with self
  selected would deadlock the tunnel.
- **[ok]** Empty IncludeOnly list falls back to Off so the user
  cannot accidentally reach a no-op state where nothing is tunneled
  but the UI says "Connected".
- **[ok]** Android forbids mixing `addAllowedApplication` and
  `addDisallowedApplication`; the branches in `applyPerAppRouting`
  use only one of them per build.
- **[note]** Package names are unencrypted in DataStore. They aren't
  secret, but the *fact that the user routes a banking app through
  the VPN* is mild user fingerprint. Acceptable.

### Tunnel data path (`core/`)

- **[ok]** TUN fd is opened by `VpnService`, owned by the app
  process, never written to disk. `fdbased.New` does not take fd
  ownership; we close `ParcelFileDescriptor` on Stop and on every
  reconnect.
- **[ok]** SOCKS inbound binds to `127.0.0.1` only — never reachable
  off-device. The port is now picked at runtime per Start by
  binding a `ServerSocket` to `127.0.0.1:0`, reading the
  kernel-assigned port, and immediately closing
  (`GmvpnVpnService.pickEphemeralLoopbackPort`). The same value
  flows into both the Xray-core JSON config (`opts.socksPort`) and
  the `EngineBridge.start(socksPort=...)` call, so a hostile
  co-resident VPN can no longer squat the well-known 10808. On
  allocation failure we fall back to the static default — still
  loopback-only.
- **[ok]** Reality / TLS details (sni, fp, public key, short id)
  travel in JSON between Rust and Go but never get logged with the
  default `LogLevel.Warning` — the engine would have to be flipped
  to `debug`/`info` for this to leak.
- **[note]** UDP relay uses `golang.org/x/net/proxy.SOCKS5` for TCP
  and a hand-rolled SOCKS5 UDP ASSOCIATE for UDP. The hand-rolled
  path has unit tests for wire format only. Physical TECNO LG8n
  validation on 2026-06-15 covered browser WebRTC/STUN plus a
  5-minute Chrome YouTube playback window through the VPN, with
  post-load HTTPS/IPv4/DNS and disconnect/reconnect passing. Limitation:
  this was fallback browser/WebRTC/QUIC evidence, not controlled iperf
  UDP throughput/loss measurement.

### Diagnostics export

- **[ok]** Output goes to `cacheDir/diagnostics/`, exposed only via
  FileProvider with `grantUriPermissions=true`.
  `allowBackup=false` keeps it out of cloud backup.
- **[ok]** Redactor removes UUIDs, ss userinfo, trojan password,
  Reality `pbk/sid/spx`, password / token query params,
  Authorization / X-Api-Key / Cookie headers, recursively re-applied
  to URI tokens that show up inside log lines. Covered by 10 JVM
  unit tests in `RedactorTest`.
- **[gap]** `vmess://...` is collapsed to `vmess://<base64-redacted>`
  whole, which is safe but loses *all* metadata — a server admin
  helping debug can't tell which transport was used. Acceptable
  trade-off; revisit if support friction is real.
- **[ok]** Logcat tail uses `-v threadtime` and post-filters every
  line by `Process.myPid()`. Foreign-pid lines are dropped before
  redaction; the count is surfaced in a one-line header so the
  drop is visible to whoever inspects the blob. Defence-in-depth
  against a future Android version relaxing the post-4.1
  "apps see only their own logs" default.

### Engine bridge (`tunnel/EngineBridge.kt`)

- **[ok]** The gomobile `.aar` is reached via reflection so the app
  builds without the artifact. If the artifact is missing we surface
  a typed `EngineUnavailableException` rather than a raw
  `ClassNotFoundException` from the Compose layer.
- **[note]** Reflection matches `Tunnel.Start(String, int, int, int)`
  by parameter list. A future gomobile naming break would still
  match if the parameter shape stayed the same; the runtime symptom
  would be wrong-method dispatch. Mitigation: when we add an
  integration smoke test, assert the engine's `XrayVersion()` is
  non-empty as a sanity check.

### Reconnect on network change

- **[ok]** `tunnelMutex` serialises Start / Stop / handover-driven
  reconnect. No two `bringTunnelUp` calls can race.
- **[ok]** `unregisterNetworkCallback()` runs before `engine.stop()`
  in `handleStop` — no orphaned handover events crash the closing
  service.
- **[note]** The status flips to `Reconnecting` on `onLost`, then
  the actual restart only happens on the next `onAvailable`. While
  in this in-between state we have no underlying network and no
  tunnel; the kill-switch comes from Android's "Block connections
  without VPN", not from us. Documented in the always-on hint, but
  must be verified during the device run.

### VpnService configuration

- **[note]** The service declares
  `android:foregroundServiceType="systemExempted"` as a `VpnService`
  and suppresses AGP's `ForegroundServicePermission` lint warning only
  on that service. We deliberately do not request exact-alarm
  permissions to satisfy a generic lint path that is unrelated to VPN
  foreground-service behavior.
- **[ok]** Dual-stack: routes for `0.0.0.0/0` and `::/0`. IPv6 is
  tunneled rather than left raw — closes the obvious leak.
- **[ok]** DNS servers are explicit (`1.1.1.1`, `8.8.8.8`); the
  builder does not inherit the default resolver. Physical TECNO LG8n
  browser DNS leak audit on 2026-06-15 observed public/VPN-path
  resolver providers and no local mobile/Wi-Fi ISP resolver in the
  result set; raw resolver IPs are kept only in ignored artifacts.
- **[gap]** MTU is fixed at 1500. Some carriers MSS-clamp lower; we
  may emit oversized packets that fragment or drop silently.
  Workaround for v1: keep 1500 for the first device run and, on
  observed regressions, expose the value in the UI later.
- **[gap]** Internal IPv4 `10.10.10.2/28` and IPv6 `fd00:0:0:1::2/112`
  are hard-coded and could collide with a captive-portal subnet in
  exotic networks. Low risk; address space is generic enough; pick
  a more obscure prefix if seen in the field.

### CI / release pipeline

- **[ok]** `RELEASE_KEYSTORE_BASE64` decoded only inside `runner.temp`,
  passed to Gradle via env vars, not via gradle.properties. Workflow
  fails fast if any secret is missing.
- **[note]** Workflow uses `actions/upload-artifact` for the signed
  APK but does not yet sign attestations or publish a SLSA
  provenance. Out of scope for v1 but worth a sub-task before any
  reproducible-build claim.

## Open security TODOs (priority order)

1. ~~Pick the SOCKS inbound port from the ephemeral range at runtime~~
   Done — `GmvpnVpnService.pickEphemeralLoopbackPort` binds
   `127.0.0.1:0`, reads the kernel-assigned port, closes, and
   feeds it into both the Xray config and `EngineBridge.start`.
2. ~~Subscription-import confirmation step before `replaceAll`~~
   Done — `MainActivity.decodeSubscription` now only decodes the
   body and stores the result as a transient `PendingImport`.
   `HomeScreen` shows an `AlertDialog` with redacted preview of
   the first 5 URIs (via `Redactor.redactProfileUri`), warning
   count, and Save/Cancel buttons. `profileStore.replaceAll`
   only runs after explicit user confirmation. Cancel surfaces
   an unobtrusive "library unchanged" message.
3. ~~Logcat-tail safety net: post-read filter to assert each line is~~
   ~~tagged with our process id; warn loudly if not.~~
   Done — `LogcatTail` switched to `-v threadtime` so the PID lives
   in a fixed column. Every line is matched against
   `Process.myPid()`; foreign-pid lines are dropped and a one-line
   header (`[gmvpn] kept N lines; dropped M foreign-pid lines`)
   is prefixed when M > 0 so the discrepancy is visible in the
   shared blob. Pure-function pid filter has its own JUnit suite
   (`LogcatTailTest`) covering known levels, unparseable framing,
   and unknown-level lines.
4. ~~Add a `XrayVersion()` non-empty assertion to the first instrumented~~
   ~~smoke test.~~ Done — `VpnTunnelSmokeTest` asserts
   `EngineBridge.xrayVersionOrNull()` is non-empty when `gmvpn.aar`
   is bundled, and that a missing artifact surfaces as
   `EngineUnavailableException` rather than a fake successful start.
5. Consider `setUserAuthenticationRequired(true)` on the keystore key
   gated on a future biometric-lock feature.
6. ~~Re-run this review after the device validation pass, with each~~
   ~~leak audit (DNS, IPv6, kill-switch) captured here.~~ Done
   2026-06-15: DNS leak audit passed; IPv6 was not applicable on the
   tested TECNO/network because there was no underlying IPv6 default
   route and no public IPv6 fall-through was observed; Always-on /
   block-without-VPN passed; Wi-Fi/cellular handover passed; UDP-heavy
   fallback validation passed with the throughput/loss limitation noted
   above.

## What I did **not** review

- Xray-core, gVisor, UniFFI, JNA, AGP source. Treated as trusted
  upstreams pinned by version.
- Kotlin / Compose runtime. Likewise.
- Hardware attack surface (TEE break, side channels).
- Source-code-level audit of every file. This is a design / data-flow
  review.
