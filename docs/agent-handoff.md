# Agent handoff

Этот файл - оперативная сводка для следующего Codex/agent прохода.
Постоянные правила проекта находятся в `AGENTS.md`.

## Current public tester build

- Current public tester build: `android-v1.0.0`.
- GitHub Pre-release:
  `https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0`.
- APK asset: `GMvpn-android-v1.0.0.apk`.
- APK SHA-256:
  `a43391d0c6812141f913ae48c1642276239bdc0e42c66370c4d0e73a482da72b`.
- Testers should download the APK asset only, not Source code zip/tar.gz.

## Release history short

Created:

- RC1 tag.
- RC3 tag and GitHub Pre-release.
- RC4 tag and GitHub Pre-release.
- RC5 tag and GitHub Pre-release.
- `android-v1.0.0` annotated tag and GitHub MVP/internal Pre-release.

Not created:

- Production/latest GitHub Release.
- Google Play release.

Do not move RC3/RC4/RC5 tags and do not replace APK assets in existing
GitHub Releases.

MVP/internal `android-v1.0.0` is published as a GitHub Pre-release with
`versionCode` `1000006` and `versionName` `1.0.0`. This does not approve
production/latest release or Google Play publication.

## Current branch

Main product development branch:

- `codex/p1-play-compliance-and-device-validation`.

Current premium UI/design-system branch:

- `codex/p2-premium-ui-system`.

Current reference-first premium UI branch:

- `codex/p2-reference-first-premium-ui`.

Current live Home premium UI mapping branch:

- `codex/p2-live-home-premium-ui`.

Default branch `main` now has README tester instructions and issue forms
pointing to the GitHub MVP/internal `android-v1.0.0` tester build.

## Premium UI sprint status

Branch `codex/p2-premium-ui-system` starts the premium UI/design-system
sprint. It is product work only: no new tag, no new GitHub Release, no
asset replacement, and no Google Play publication are approved.

Started on this branch:

- mature premium dark Material 3 theme tokens;
- semantic colors for connected, disconnected, preparing, warning,
  error, privacy-safe, and neutral states;
- reusable Compose components for quiet cards, compact status marks,
  primary connect button, safe profile list rows, and privacy notice;
- home screen redesign with clear protected/disconnected/preparing/error
  states, one main CTA, section headers, and quieter secondary actions;
- masked manual profile and subscription input fields;
- safer subscription failure messages that do not echo subscription URLs
  or endpoint-like exception text;
- profile list/details polish with safe names only;
- About/diagnostics polish that explains redacted report boundaries;
- `docs/premium-ui-plan.md`;
- `docs/v1.1-roadmap.md`.

Follow-up correction on the same branch:

- removed the decorative large connection orb from Home;
- removed the glow token and reduced card border/elevation intensity;
- shortened Home status copy for ordinary users;
- made connecting/disconnecting states show disabled loading-style CTA
  text instead of competing controls;
- added `Professional UI correction` acceptance criteria to
  `docs/premium-ui-plan.md`.

Latest visual-reference implementation:

- Home is now part of a four-tab shell: `Главная`, `Профили`, `Импорт`,
  `Настройки`;
- app background uses dark navy with subtle blue radial glow;
- bottom navigation is always visible with line-style Compose icons;
- Home follows the reference structure: top title/subtitle, hero status
  card, one main CTA, active profile card, tools, and saved profile
  preview;
- Profiles screen lists safe profile names only, active state, protocol,
  latency, set-active action, details, and a muted destructive clear
  action;
- Import screen separates subscription import and manual profile entry
  with masked inputs;
- Privacy Settings screen contains routing, privacy-first UI, system kill
  switch, and redacted diagnostics sections;
- Compose previews use synthetic profiles only and do not contain real
  endpoints, subscriptions, UUIDs, or IPs.

v5 reference preview decision:

- v5 reference previews are accepted as the visual baseline for further
  work;
- layout, density, cards, buttons, profile rows and bottom navigation are
  accepted for live UI mapping;
- icons are accepted as temporary/reference-quality and can be improved
  in a later dedicated icon fidelity pass;
- preview screenshots remain local in ignored
  `.local/premium-reference-preview/` and are not committed;
- business logic is unchanged;
- live UI is unchanged by the acceptance step;
- no release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Live UI mapping plan:

| Reference component | Live target | Notes |
| --- | --- | --- |
| Reference shell/background | App scaffold/theme | Use tokens, no image backgrounds |
| Home reference | HomeScreen | Preserve real tunnel states |
| ConnectionHeroCard | Home connection state block | Map Idle/Preparing/Connected/Error |
| ActiveProfileCard | Active profile section | Safe labels only |
| ToolsCard | Routing/Diagnostics actions | No endpoint data |
| SavedProfilesPreview | Profile preview area | Safe names only |
| Profiles reference | Profile management section/screen | Active/inactive/delete |
| Import reference | Import flow | Mask inputs, no raw URI echo |
| Privacy reference | Settings/privacy screen | Routing/privacy/kill-switch |
| ReferenceLineIcons | Temporary icon set | Can be improved later |

Recommended live mapping order:

1. Theme/tokens live integration.
2. Home live mapping.
3. Profiles live mapping.
4. Import live mapping.
5. Privacy/settings live mapping.
6. Icon fidelity pass.

Required privacy checks during live mapping:

- saved profiles list does not show endpoint;
- active profile card does not show endpoint;
- details do not show endpoint;
- import errors do not echo raw URL/URI;
- diagnostics copy/export stays redacted;
- synthetic UI dumps are clean;
- real profile UI dumps stay local only and are not committed.

Stage 1 live mapping status:

- Branch: `codex/p2-live-home-premium-ui`.
- Scope: theme/tokens plus live Home only.
- Mapped: premium dark background, glass card/border/radius tokens,
  primary CTA styling, bottom navigation styling, compact Home mark,
  connection hero, active profile card, routing/diagnostics tools, and
  saved profiles preview.
- Privacy: live Home profile rows still use `profileDisplaySummary`, and
  Home error detail is redacted through `Redactor` before display.
- Not mapped deeply: Profiles, Import, Settings and final icon fidelity.
- Business logic unchanged: tunnel controller, profile storage, import
  parsing, diagnostics export, release metadata, `versionCode` and
  `versionName`.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Stage 2 live Profiles mapping status:

- Branch: `codex/p2-live-home-premium-ui`.
- Scope: live Profiles screen plus shared profile-row presentation.
- Home physical QA is limited to debug install/launch and crash marker
  scan because screenshots/UI dumps with possible real profile data were
  not captured.
- Mapped: profile count/header, active/inactive premium rows, safe
  profile name, protocol, latency, active badge, outline select action,
  kebab/details action, and muted red clear action.
- Preserved behavior: active profile selection, details, rename, delete
  confirmation and active-profile reset still use existing callbacks and
  dialogs.
- Not mapped deeply: Import, Settings, unavailable profile state and
  final icon fidelity.
- Privacy: rows use `profileDisplaySummary`; profile internals remain
  hidden from ordinary UI.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Stage 3 Import live mapping status:

- Branch: `codex/p2-live-home-premium-ui`.
- Scope: live Import screen only.
- Mapped: compact privacy-oriented header, masked subscription/manual
  inputs, compact format selector, safe subscription preview, duplicate
  count, redacted import result messages and bottom navigation.
- Privacy: ordinary Import UI does not show raw URL/URI, endpoint, UUID,
  password, token, subscription URL or base64 payload.
- Preserved behavior: import parser, storage, validation, diagnostics
  redaction and release metadata are unchanged.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Stage 4 Settings/Privacy live mapping status:

- Branch: `codex/p2-live-home-premium-ui`.
- Scope: live Settings/Privacy screen only.
- Mapped: compact privacy header, routing card, privacy-first card,
  system kill switch card and redacted diagnostics card.
- Preserved behavior: routing opens the existing per-app routing flow,
  kill switch uses the existing Android Always-on VPN action, and
  diagnostics opens the existing redacted diagnostics dialog.
- Privacy: Settings UI does not show raw profile URI, endpoint, IP, host,
  domain, port, UUID, password, token, subscription URL, base64 payload
  or raw diagnostics logs.
- Not mapped: final icon fidelity and any release work.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Stage 5 Icon fidelity pass status:

- Branch: `codex/p2-live-home-premium-ui`.
- Scope: live Compose/Canvas icons only.
- Mapped: bottom navigation icons for Home, Profiles, Import and
  Settings; card/action icons for shield/status, routing, diagnostics,
  privacy-first, kill switch, import/download, active/inactive status,
  delete/edit, latency and chevrons.
- Implementation: `GmLineIcon` remains the single live icon source,
  using a consistent 2dp line style with rounded caps and joins.
- PNG reference tiles from `.local/design-assets` were used only as
  local reference material and were not committed.
- Business logic, parser behavior, profile storage, diagnostics
  redaction, release metadata and published assets are unchanged.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Live premium UI review PR:

- PR: `https://github.com/GronGM/GMvpn2/pull/14`;
- source: `codex/p2-live-home-premium-ui`;
- target: `codex/p1-play-compliance-and-device-validation`;
- status: merged into `codex/p1-play-compliance-and-device-validation`;
- merge commit: `5bd4e94be43cce3cb409e461d6966a69f52f46b0`;
- premium v5 live UI is now part of P1, but this is not a release
  approval;
- hidden/bidi Unicode check over changed `.kt`, `.xml`, `.md`, `.kts`,
  `.yml` and `.yaml` files: pass;
- control/format character check over the same changed files: pass;
- mapped flows: Home, Profiles, Import, Settings/Privacy;
- icon fidelity: acceptable for review, not necessarily final forever;
- four-tab physical no-profile visual QA: pass on Android debug build;
- current APK UI privacy dump scan: pass, no forbidden endpoint/profile
  markers found in controlled no-profile dumps;
- basic accessibility label proxy from no-profile UI dumps: pass for
  required visible labels and content descriptions;
- live manual invalid-input check via ADB input was inconclusive:
  the masked field accepted short synthetic input, but no persistent
  user-visible error was captured after save; recheck manually before RC;
- approved real subscription endpoint reachability from Windows: pass,
  redacted endpoint value not recorded;
- manual real subscription import on physical Android: pass, 4 of 4
  profiles imported; raw subscription value was not captured by Codex;
- real VPN smoke: pass on physical Android after manual import;
- connect / disconnect / reconnect: pass, two reconnect cycles ended
  with service active after connect and stopped after disconnect;
- internet through VPN: pass via HTTPS connectivity probe with output
  suppressed;
- diagnostics redaction with a real VPN profile: pass-limited because
  clipboard readback was unavailable; raw diagnostics were not printed,
  exported, or committed;
- synthetic invalid import visibility: pass after clearing the debug app
  back to a no-profile state; user-visible error was present, synthetic
  raw input stayed masked, and no URL/URI/IP/UUID/base64 markers were
  visible in the safe UI dump;
- accessibility/TalkBack: pass-limited; no-profile accessibility proxy
  found no focusable unlabeled blocker and no secret markers, but full
  TalkBack audio QA remains future work;
- crash/ANR markers after smoke and self-validation: 0;
- blockers before any future `v1.1.0-rc.1`: explicit release approval,
  diagnostics redaction full readback if required, full TalkBack QA if
  required, and separate UDP/IPv6 production-readiness decision;
- no release, tag, GitHub Release asset update, APK/AAB upload, or
  Google Play publication is authorized.

PR #13 merged checkpoint:

- PR: `https://github.com/GronGM/GMvpn2/pull/13`;
- merged into `codex/p1-play-compliance-and-device-validation` by merge
  commit `a15088e7fcf7ebe7ff166ee7cc66027fe7e2fdbb`;
- hidden/bidi Unicode and control/format scans passed after removing a
  UTF-8 BOM from `HomeScreen.kt`;
- launcher icon safe-zone was checked on TECNO LG8n: shield/G is visible
  in launcher and app-info masks;
- generated raster sheets are reference-only; only the launcher asset is
  implemented and committed;
- physical synthetic smoke used local synthetic profile text only, not a
  real profile, subscription, endpoint, host, IP, UUID, token, or
  password;
- controlled GMvpn UI dumps covered no-profile Home, Import, synthetic
  manual save, Profiles, Home with active profile, and invalid connect;
- invalid synthetic connect showed a persistent user-visible error and
  did not create a fake connected state;
- high-confidence UI privacy scan over controlled dumps found no raw
  URI, UUID, IP, subscription URL, long base64 payload, or secret
  key/value evidence;
- clickable premium cards/rows now expose merged semantics labels in
  UIAutomator dumps; Material/Compose wrapper nodes can still appear as
  clickable without their own text/content description, so a full
  TalkBack pass remains future QA.

Recommended future tester version for this branch is likely
`v1.1.0-rc.1`, but that requires separate explicit release approval after
tests, physical smoke, signed workflow, and artifact verification.

Post-merge RC readiness gate:

- readiness document: `docs/android-v1.1-rc-readiness.md`;
- candidate source SHA for a future signed RC workflow:
  `66e28ae5aed4b2753cc5d12f33f162be3e20a707`;
- approval phrase recorded there for preparing, but not tagging or
  publishing, signed `android-v1.1.0-rc.1` artifacts;
- no release workflow, tag, GitHub Release asset update, APK/AAB upload,
  version bump, or Google Play publication is authorized by the
  readiness document alone.

## What is done

RC5 includes:

- profile management;
- safe profile names;
- active profile selection;
- rename;
- delete confirmation;
- active-profile reset;
- safe import preview;
- duplicate count;
- redacted diagnostics;
- network validation bench docs with redacted evidence templates.

RC5 verification summary:

- signed workflow run: `27679203026`;
- artifact source SHA:
  `15d0a7f5fd691f9bf517a05ac867fc661be8c233`;
- versionCode: `1000005`;
- versionName: `1.0.0-rc.5`;
- package: `com.gmvpn.client`;
- minSdk: `26`;
- targetSdk: `35`;
- checksums: pass;
- APK signature: pass;
- AAB verification: pass;
- bundletool validate: pass;
- 16 KB ELF alignment: pass;
- APK `zipalign -P 16`: pass;
- physical install/launch: pass;
- crash/ANR scan: pass.

Post-RC5 source hardening:

- diagnostics redaction now collapses profile URIs without host/port;
- diagnostics redaction also masks HTTP URLs, IPv4 addresses, and
  host/destination context in free-form text;
- local unit tests, `lintDebug`, and `assembleDebug` passed;
- public RC5 APK/release assets were not changed.

## Known limitations

Unrestricted `v1.0.0` remains blocked by:

- UDP/iperf: `pass-limited`;
- IPv6: not tested.

DNS is `pass` for the tested device/network, but do not generalize that
claim beyond the recorded RC5 Android-side evidence. Do not claim
production readiness until the remaining gaps are closed or limitations
are explicitly accepted for a limited/MVP release.

## v1.0.0 release decision package

Strict/unrestricted `v1.0.0` is not ready. It remains blocked by:

- UDP threshold/outlier handling after Android-side `pass-limited`
  Termux `iperf3` evidence;
- real IPv6 pass/fail-closed evidence from a network with an external
  IPv6 baseline;
- final signed `1.0.0` workflow from the exact final release source SHA.

Strict approval phrase:

```text
APPROVE UNRESTRICTED V1.0.0 AFTER UDP_THRESHOLD_AND_IPV6_PASS
```

MVP/internal `v1.0.0` is approved for final signed workflow preparation
with explicit limitations accepted. Approval phrase received:

```text
APPROVE MVP V1.0.0 WITH UDP_IPV6_LIMITATIONS_ACCEPTED
```

MVP/internal release notes must state that UDP is functional but
`pass-limited`, IPv6 is not validated because checked networks had no
external IPv6 baseline, and the build is MVP/internal rather than an
unrestricted production rollout. Google Play publish remains separate and
not approved.

Final signed MVP/internal `1.0.0` workflow and local verification:

1. Android metadata is prepared as `versionName` `1.0.0` and
   `versionCode` `1000006`.
2. `android-release.yml` run `27701966507` succeeded with
   `rc_tag=android-v1.0.0` and `version_name=1.0.0`.
3. Artifact source SHA:
   `7daf7145fa53638002480b41f1459ac4b065b8ac`.
4. Signed APK/AAB were downloaded only under ignored `.local/` paths.
5. Checksums, APK signature, AAB jarsigner verification with expected
   self-signed certificate warnings, `bundletool validate`, APK/AAB 16 KB
   ELF alignment, `zipalign -P 16`, and metadata checks passed.
6. APK SHA-256:
   `a43391d0c6812141f913ae48c1642276239bdc0e42c66370c4d0e73a482da72b`.
7. AAB SHA-256:
   `d5d0078e33d07e8cf3d67698e78f932f4342e9422c208109051a791b2ce4dde2`.
8. Physical smoke on TECNO LG8n Android 12/API 31 passed install,
   launch, About version `1.0.0`, connect, disconnect, reconnect,
   privacy-safe saved profile labels, and no GMvpn crash/ANR markers.
   Diagnostics copy redaction remains `pass_limited` because clipboard
   readback was unavailable over ADB; DNS quick sanity was not rerun.
9. Annotated tag `android-v1.0.0` was created after explicit tag
   approval and points to
   `7daf7145fa53638002480b41f1459ac4b065b8ac`.
10. GitHub MVP/internal Pre-release `android-v1.0.0` was created after
    explicit release approval:
    `APPROVE GITHUB MVP RELEASE android-v1.0.0 WITH UDP_IPV6_LIMITATIONS_ACCEPTED`.
    It contains only `GMvpn-android-v1.0.0.apk` and
    `GMvpn-android-v1.0.0.apk.sha256`.

Network evidence plan:

- runbook: `docs/android-network-validation-bench.md`;
- updated on 2026-06-17;
- controlled UDP/iperf requires an approved redacted endpoint;
- full DNS requires two independent methods while VPN is connected;
- IPv6 requires a real external IPv6 network or fail-closed evidence;
- raw logs, endpoints, screenshots, profiles, and packet captures stay
  out of git.

Latest preflight:

- date: 2026-06-17;
- GitHub issues: 0 open;
- script: `scripts/validation/preflight-windows.ps1`;
- runner: `scripts/validation/run-network-validation-windows.ps1`;
- local `adb`: found through standard Android SDK platform-tools;
- authorized physical device: present in the latest run, serial masked in
  console output;
- approved iperf endpoint variables: present locally during the latest
  validation attempt, values not printed or committed;
- local `iperf3`: available through trusted WinGet user portable install
  or PATH lookup;
- controlled VPS endpoint: configured with `iperf3-gmvpn.service`,
  TCP/UDP 5201 firewall rules, SSH key access, and rotated root
  password;
- Windows endpoint TCP/UDP connectivity: pass, endpoint redacted. Latest
  runner captured a 30-second 5M UDP run with 0% loss and 4.249 ms
  jitter, classified as endpoint-only evidence;
- Android GMvpn VPN-path UDP: `pass-limited`. Termux was installed from
  the official `termux/termux-app` GitHub pre-release `v0.119.0-beta.3`
  with local SHA-256 verification, `iperf3` 3.21 was installed inside
  Termux, an approved subscription was imported into GMvpn RC5, and a
  controlled UDP matrix ran through active GMvpn on TECNO LG8n Android
  12/API 31. Endpoint, profile, subscription, raw IP, and raw command
  details were not committed. Best stable row: 5M, payload 1200 bytes,
  three 30-second runs, max packet loss 0.096%, max jitter 2.477 ms,
  GMvpn connected before/after every run. A post-matrix logcat tail scan
  found no case-sensitive GMvpn crash/ANR markers. Keep `pass-limited`
  because no formal release loss threshold is approved and 2M had a
  high-loss outlier, later reproduced once in a 5-run 2M rerun. Current
  release interpretation is `UDP functional validation: pass-limited` and
  `UDP performance validation: needs threshold/review`; unrestricted
  v1.0.0 stays blocked until a maintainer-approved threshold is accepted
  or extra stable validation is captured;
- DNS: `pass` for the tested device/network. BrowserLeaks DNS in Android
  Chrome and a Termux `dig` controlled resolver query both ran while
  GMvpn stayed connected, recorded provider/country-level evidence only,
  and found no private/router DNS;
- IPv6: `not_tested`. A disconnect/baseline/reconnect smoke found no
  real external IPv6 baseline on the current network, so IPv6 cannot be
  marked `pass` or `fail_closed`. A later force-stop baseline check did
  produce a clean pre-VPN shell baseline with `tun0` absent, but the
  tested network still had no external IPv6 route or IPv6 probe success.
  A follow-up network search tried current network, Wi-Fi-only,
  mobile-data-only, and Wi-Fi+mobile modes via adb. No external IPv6
  baseline appeared; after restoring radios, Android reported a validated
  network and IPv4 worked, but IPv6 probes still failed. Re-test on LTE/5G,
  another Wi-Fi, or another network where the Android device has external
  IPv6 before GMvpn is enabled;
- RC5 stability smoke: pass-limited. Force-stop baseline and restore were
  exercised, `tun0` returned after restore, no case-sensitive GMvpn
  crash/ANR markers were found, and the local diagnostics bundle was not
  committed. The adb diagnostics bundle still requires manual review
  before sharing because dumpsys/logcat can contain IP/host-like local
  data;
- no raw logs, profiles, endpoints, APK/AAB files, or `.local/`
  artifacts were committed.

## Next product sprint

Recommended order:

1. Collect RC5 tester feedback.
2. Triage new issues with privacy-sensitive rules.
3. Define an approved UDP release threshold or rerun controlled UDP/iperf
   on another network window using only approved endpoints.
4. Optionally repeat full DNS leak audit on another network before final
   release, keeping only redacted provider/country summaries.
5. Run real IPv6 validation or prove fail-closed behavior.
6. Improve stability:
   - app restart while connected/disconnected;
   - reconnect edge cases;
   - invalid subscription handling;
   - no-profile state;
   - diagnostics copy/export UX.
7. Harden diagnostics with more sensitive-pattern unit tests.
8. Decide whether RC6 is required.

## Release rules

Do not move `android-v1.0.0` or replace its release assets without a new
explicit approval.

Do not create a new RC without:

- version bump;
- signed workflow;
- checksum verification;
- APK signature verification;
- AAB verification if produced;
- 16 KB ELF verification;
- APK `zipalign -P 16`;
- physical smoke;
- release notes;
- explicit tag/release approval.

For tester GitHub Pre-releases, upload only:

- signed APK;
- SHA-256 checksum.

AAB is not uploaded for normal testers unless separately approved.

## Last known safe next step

Continue visual review and QA for the live premium UI on
`codex/p2-live-home-premium-ui`. Home, Profiles, Import,
Settings/Privacy and the current icon fidelity pass are mapped for this
stage.

Before any future tester RC, run full real VPN smoke again. Keep
privacy-safe labels, run debug/manual QA, and do not create a release,
tag, GitHub Release asset update, or Google Play publication without
separate explicit approval.
