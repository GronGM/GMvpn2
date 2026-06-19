# GMvpn premium UI plan

Status: started on branch `codex/p2-premium-ui-system`.

This sprint improves product quality and trust after the GitHub
MVP/internal `android-v1.0.0` pre-release. It does not approve a new
release, tag, GitHub Release asset, or Google Play publication.

Suggested future version: `v1.1.0-rc.1`, not an immediate release.

## Current premium UI status

Status after PR #13:

- PR #13 technically passes checks and was merged into
  `codex/p1-play-compliance-and-device-validation`.
- The current premium UI is not considered visually accepted by the
  user and must not be treated as the final visual direction.
- The next step is a reference-first mock screen pass using synthetic
  data only.
- `v1.1.0-rc.1` is not ready.
- Do not create a release, tag, GitHub Release asset update, or Google
  Play publication from the current visual state.
- Reference screens should be reviewed visually before their components
  are moved into the live Home / Profiles / Import / Settings flows.
- A debug-only `PremiumReferenceHostActivity` can render the mock
  reference screens on a device for local screenshots. It must remain a
  review tool only and must not be treated as live product UI.

Status after v5 reference previews:

- v5 reference previews are accepted as the visual baseline for further
  work.
- Layout, density, cards, buttons, profile rows and bottom navigation
  are accepted for live UI mapping.
- Icons are accepted as temporary/reference-quality and can be improved
  in a later dedicated icon fidelity pass.
- Business logic and live UI were not changed by the preview acceptance
  step.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Status after Stage 1 live Home mapping:

- Working branch: `codex/p2-live-home-premium-ui`.
- Stage 1 scope is intentionally limited to shared premium tokens,
  shared surface/button/row styling, bottom navigation, and live Home.
- Live Home initially followed the accepted v5 hierarchy with compact
  `GMvpn` mark, connection hero, one primary CTA, active profile card,
  tools, saved profiles preview, and bottom navigation.
- Stage 2.5 changes the live Home direction: the active profile card
  remains, but its redundant `Active profile` heading is removed; the
  saved profiles preview block is removed because the `Profiles` tab
  already owns profile browsing.
- Active profile and saved profile rows continue to use
  `profileDisplaySummary`, so only safe profile labels, protocol and
  latency are shown.
- Home connection errors are passed through the existing diagnostics
  `Redactor` before display so raw URI/URL/IP/UUID/host-like details do
  not appear in ordinary UI.
- Profiles, Import and Settings live screens are not deeply redesigned in
  this stage. Their visual changes are limited to shared tokens and
  shared components.
- Business logic, tunnel controller, profile storage, import parsing,
  diagnostics export, release metadata, `versionCode` and `versionName`
  are unchanged.
- Icon fidelity remains a separate future pass.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

Status after Stage 2 live Profiles mapping:

- Working branch: `codex/p2-live-home-premium-ui`.
- Home physical QA is `pass-limited`: debug APK installed and launched
  on the physical Android device, and GMvpn crash/ANR markers were not
  found in the checked logcat tail. Screenshots/UI dumps with real
  profile data were not captured.
- Stage 2 maps the live Profiles screen and shared profile rows to the
  accepted v5 direction.
- Profiles now use standalone premium rows with active/inactive visual
  state, safe profile name, protocol, latency, active badge, outline
  select action, and kebab/details action.
- The destructive clear action remains a muted red outline, not a
  primary CTA.
- Profile details, rename, delete confirmation, active profile selection
  and active-profile reset still use the existing callbacks and dialogs.
- Import and Settings are still future live mapping stages.
- Business logic, tunnel controller, profile storage, import parsing,
  diagnostics export, release metadata, `versionCode` and `versionName`
  are unchanged.
- No release, tag, GitHub Release asset update, or Google Play
  publication is authorized.

## Target visual reference

The current visual target is a professional Android VPN client in a
premium dark / glass security app style. It should feel closer to a
mature security utility or private network product than to a decorative
cyberpunk mockup.

Reference qualities:

- near-black navy background with subtle blue radial glow;
- tonal glass-like dark cards with thin muted borders;
- calm blue primary CTA;
- calm green connected/active state;
- muted red only for destructive actions;
- bottom navigation with four always-visible tabs:
  - `Главная`;
  - `Профили`;
  - `Импорт`;
  - `Настройки`;
- structured screens instead of one endless settings page;
- one primary action per screen;
- line-style icons;
- safe profile names only;
- no endpoint, URI, UUID, password, IP, host, port, subscription URL, or
  base64 payload in normal UI.

Implemented direction on `codex/p2-premium-ui-system`:

- app shell with Home / Profiles / Import / Privacy Settings tabs;
- subtle dark navy radial background;
- glass/tonal cards and bottom navigation;
- Home hero status card with shield icon and single primary CTA;
- dedicated profile list screen;
- dedicated import screen;
- dedicated privacy/settings screen;
- synthetic Compose previews for Home, Profiles, Import, Settings,
  empty, connected, and error states.

## Visual acceptance criteria

1. The app looks like a professional security/VPN product, not a school
   project or decorative template.
2. The Home screen answers in three seconds:
   - whether VPN is connected;
   - which profile is active;
   - what to press next.
3. Each screen has one main scenario.
4. Bottom navigation is clear and always visible.
5. Cards use one visual system.
6. Buttons and status indicators do not compete with each other.
7. Glow/decor is subtle and never required for understanding.
8. UI does not show IP, host, port, UUID, password, raw URI, subscription
   URL, or base64 payload.
9. Russian strings are readable and not clipped in normal phone widths.
10. Status is understandable from text, not only color.

## Current UI audit

Home / connection screen:

- Previous layout exposed most functions as plain Material cards with
  little visual hierarchy.
- Connection state was technically correct, but the main screen did not
  make the trust state feel clear or premium.
- No-profile recovery was present but too technical and easy to miss.
- Error state was persistent, but diagnostics copy was not available from
  the error surface.

Saved profiles:

- Profile labels already use the privacy-safe `profileDisplaySummary`
  path.
- The list was functional but dense and not visually distinct from import
  and routing controls.
- Active profile was shown as text only; it needed stronger visual
  hierarchy without revealing endpoint data.

Profile details:

- Details show safe metadata, but the layout looked utilitarian.
- Rename/delete/set-active actions were available, but the privacy
  boundary was not explained near the metadata.
- Delete confirmation did not strongly call out active-profile reset.

Import subscription flow:

- Import preview already showed safe names, protocols, duplicate count,
  and skipped count.
- The subscription URL field and manual profile field displayed the raw
  entered value; this was a privacy risk for shoulder-surfing and
  screenshots.
- Network/HTTP import errors could include unsafe endpoint context if an
  exception carried a URL-like message.

Diagnostics report:

- Diagnostics are redacted by design, but the About screen did not make
  this trust boundary visually prominent.
- Copy/export actions were plain controls without enough warning that the
  user should still review before sharing.

Empty/error/reconnect/disconnect states:

- Empty state existed but did not provide a clear premium recovery path.
- Preparing/reconnecting/stopping used the same plain button area.
- Permission cancel fix remains a state-machine behavior; the UI should
  not imply a fake connected state after cancel.

Accessibility/localization:

- Touch targets are mostly Android standard controls.
- Russian strings existed, but some labels were mixed English/Russian.
- The old interface had limited semantic status affordances beyond text.
- Contrast depended on default Material colors rather than explicit
  semantic tokens.

## Professional UI correction

The first premium pass improved consistency but still risked looking like
a decorative prototype instead of a mature security product. Correction
goal: remove visual noise, make the hierarchy obvious, and help an
ordinary user answer three questions within three seconds: am I
protected, which profile is active, and what should I press.

1. Visual hierarchy

- Problem: status, orb, cards, and tools had similar visual weight.
- Fix: status text and the primary CTA become the dominant module;
  secondary tools move below the active profile.
- Affected screens: Home, error state, no-profile state.
- Acceptance criteria: the main status and one primary button are visible
  before profile/import controls compete for attention.

2. Spacing and rhythm

- Problem: repeated cards made the screen feel like a stack of unrelated
  blocks.
- Fix: keep the 8dp rhythm, add section headers, and use quieter card
  surfaces.
- Affected screens: Home, profile list, import block, diagnostics/About.
- Acceptance criteria: screen sections read top-to-bottom without nested
  card clutter or uneven gaps.

3. Typography consistency

- Problem: many blocks used similar text weight, so hierarchy depended
  on color and card position.
- Fix: one headline for connection state, one section-title style, one
  body style, one caption style.
- Affected screens: Home, About/diagnostics, profile details.
- Acceptance criteria: headings, explanatory text, and metadata are
  visually distinct without more than a few text sizes.

4. Status communication

- Problem: the large decorative status orb could read as a toy indicator
  instead of operational state.
- Fix: replace it with a compact status mark plus explicit status title
  and explanation.
- Affected screens: Home connection block.
- Acceptance criteria: state is understandable from text alone; color is
  only reinforcement.

5. Component consistency

- Problem: selected/warning/error cards and pills used strong borders and
  could look like separate visual systems.
- Fix: reduce border alpha/elevation, keep one accent plus semantic
  success/warning/error colors.
- Affected screens: all Compose UI cards and status labels.
- Acceptance criteria: cards feel related, not like a collage of badges.

6. Too much decoration / glow / gradients

- Problem: glow-like treatments and a large orb were not necessary for a
  VPN utility.
- Fix: remove glow token, remove the hero orb, keep flat surfaces and
  subtle borders.
- Affected screens: Home and shared components.
- Acceptance criteria: no decorative glow/gradient/orb is required to
  understand the screen.

7. Weak empty/error states

- Problem: empty and error states existed, but they were framed like
  technical messages.
- Fix: short human copy, persistent error card, and a direct redacted
  diagnostics action.
- Affected screens: no-profile, import failure, connection error.
- Acceptance criteria: the user knows what happened and what to do next
  without reading technical details.

8. Ordinary-user clarity

- Problem: too many equally visible controls made the next action unclear.
- Fix: one main connect/disconnect/retry button, with secondary actions
  grouped under tools and profile/import sections.
- Affected screens: Home.
- Acceptance criteria: a non-technical tester can point to the main action
  immediately.

9. Accessibility/readability

- Problem: small low-contrast explanatory text carried too much meaning.
- Fix: reduce long helper text, keep visible text labels for state, and
  preserve standard Material touch targets.
- Affected screens: Home, diagnostics/About, import.
- Acceptance criteria: status is not color-only, Russian strings are not
  clipped in normal phone widths, and buttons remain large enough.

10. Privacy-safe presentation

- Problem: manual/import fields were already masked, but the UI needed a
  cleaner trust story rather than repeating long legal copy.
- Fix: keep masked inputs, safe profile summaries, safe import preview,
  and short trust notes.
- Affected screens: manual profile, subscription import, profile list,
  diagnostics.
- Acceptance criteria: visible UI dump contains no endpoint, URI, UUID,
  password, IP, host, subscription URL, or base64 payload.

## Premium visual direction

- Premium dark by default.
- Calm security-product surfaces: deep neutral background, quiet cards,
  restrained blue accent and muted success/warning/error colors.
- Trust-first hierarchy: connection state, active profile, import,
  diagnostics, and routing are visually separated.
- No cheap hacker styling, no aggressive neon, no busy animations.
- Compact status marks and text are used for state clarity.

## Design principles

- Show state, not secrets.
- Saved profile UI must show only safe profile names, protocol type, and
  non-sensitive status metadata.
- Keep primary connection action large and predictable.
- Keep error recovery persistent and visible.
- Prefer short Russian copy with clear user outcomes.
- Keep diagnostics framed as redacted but still user-reviewed.

## Target screens

- Home / connection screen.
- Saved profile list and profile details dialog.
- Manual profile entry.
- Subscription import and preview.
- Diagnostics/About screen.
- Per-app routing screen.
- Empty/no-profile state.
- Error state.
- Preparing/reconnecting/disconnect states.

## Component inventory

Implemented or started:

- `GmCard`: premium surface with semantic neutral/selected/warning/error
  tones.
- `ConnectionStatusMark`: compact operational status indicator, not a
  decorative orb.
- `PremiumConnectButton`: large connect/disconnect/retry action.
- `ProfileListItem`: safe profile name, protocol type, active indicator,
  latency, no endpoint data.
- `StatusPill`: compact state label for protected/disconnected/preparing
  and warning/error states.
- `PrivacyNotice`: short trust boundary card for local profiles and
  redacted diagnostics.

## Live UI mapping plan

The accepted v5 reference screens are the design baseline, not production
UI. Move them into live screens in small reviewable steps.

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

Recommended order:

1. Theme/tokens live integration:
   - colors;
   - shapes;
   - typography;
   - spacing;
   - surfaces.
2. Home live mapping:
   - connection hero;
   - active profile;
   - tools;
   - saved profiles preview;
   - bottom nav.
3. Profiles live mapping:
   - rows;
   - active/inactive state;
   - rename/delete/details;
   - safe labels only.
4. Import live mapping:
   - masked subscription/manual input;
   - safe preview;
   - errors without raw URL/URI echo.
5. Privacy/settings live mapping:
   - routing;
   - privacy-first;
   - kill switch;
   - diagnostics notice.
6. Icon pass later:
   - improve icon fidelity after layout/components are mapped;
   - do not block the first live mapping pass only because icons are not
     final.

Do not move every preview component into live UI in one large commit.
Each major mapping step should run unit tests, lint, debug assemble,
values parity, secret scans and artifact guards.

## Color/typography/shape plan

Theme files now centralize:

- premium dark palette;
- light palette kept available through the theme parameter;
- semantic colors for connected, disconnected, preparing, warning, error,
  privacy-safe, and neutral states;
- typography scale for title/body/label hierarchy;
- rounded card/control/dialog shapes;
- spacing tokens;
- subtle elevation/border tokens.

Screens should use these theme/tokens and avoid ad-hoc color literals.

## Motion plan

- No heavy motion in this sprint.
- Status mark is static and readable.
- Future motion should use centralized `GmMotion` durations and remain
  subtle enough for VPN/trust context.

## Accessibility checklist

- Keep text-based status labels alongside visual indicators.
- Keep standard Material buttons and fields for touch target size.
- Provide content descriptions for non-text status indicators.
- Merge semantics for clickable cards/rows so assistive tools announce
  the action label instead of only the decorative child nodes.
- Avoid color-only meaning; use labels such as `Защищено`, `Ошибка`,
  `Требуется профиль`.
- Keep Russian strings short enough for compact phones.
- Maintain `values` / `values-ru` string parity.
- Do not rely on screenshots for validation when real profiles are
  visible.

## PR #13 review checkpoint

PR: `https://github.com/GronGM/GMvpn2/pull/13`.

Merged into `codex/p1-play-compliance-and-device-validation` by merge
commit `a15088e7fcf7ebe7ff166ee7cc66027fe7e2fdbb`.

Checked on branch `codex/p2-premium-ui-system` before merge:

- hidden/bidi Unicode scan: passed after removing a UTF-8 BOM from
  `HomeScreen.kt`;
- control/format character scan: passed after the same cleanup;
- launcher icon safe-zone: checked on TECNO LG8n, shield/G fits inside
  the launcher and app-info masks;
- visual reference package: Variant 1 shield/G is implemented as the
  launcher asset; generated raster sheets remain reference-only and are
  not committed;
- physical synthetic smoke: clean install/launch, no-profile Home,
  Import, synthetic manual profile save, Profiles, Home with active
  profile, and invalid synthetic connect path;
- invalid synthetic connect result: persistent user-visible error,
  button returns to `Подключить`, no fake connected state;
- UI privacy scan over controlled GMvpn UI dumps: no raw URI, UUID, IP,
  subscription URL, long base64 payload, or secret key/value evidence;
- accessibility/semantics pass-limited: primary clickable cards/rows now
  expose useful merged labels in UIAutomator dumps, but Material/Compose
  wrapper nodes can still appear as clickable nodes without their own
  text/content description; a full TalkBack pass remains future QA.

No screenshots, UI dumps, raw evidence, APK/AAB, private configs, or
real profile data are committed.

## Stage 2.5 Home/Menu correction

Home/main menu is accepted for the current stage. No additional
Home/Menu visual pass is planned before Import mapping.

Scope for Stage 2.5:

- compact Home mark, without a large subtitle/header block;
- lighter connection hero with one readable status and one primary CTA;
- secondary routing/diagnostics tools that do not dominate the screen;
- compact active profile card without the redundant `Active profile`
  label;
- no saved profiles preview block on Home;
- lighter bottom navigation with readable active/inactive states.

Remaining UI work:

1. Full real VPN smoke before any RC.

Release, tag and asset changes remain out of scope.

## Stage 3 Import live mapping

Import is the next accepted live mapping target after Home/Profile.
Scope:

- compact Import header consistent with Home/Profiles;
- subscription card with masked URL input, format selector and one
  primary `Fetch and import` CTA;
- manual profile card with masked input and one primary `Save profile`
  CTA;
- safe import preview: safe names, protocols, duplicate count and
  skipped count only;
- redacted runtime import messages and errors, with no raw URL/URI,
  endpoint, UUID, password or base64 payload echo.

This stage must not change parser logic, storage model, TunnelController,
release metadata, diagnostics redaction, or the profile safe-name
formatter.

## Stage 4 Settings/Privacy live mapping

Settings/Privacy is mapped after Home, Profiles and Import. Scope:

- compact privacy header consistent with the live Home/Profiles/Import
  system;
- routing card with the existing app-routing action;
- privacy-first card explaining local profile storage and hidden private
  data;
- system kill switch card with the existing Android Always-on VPN action;
- diagnostics card that opens the existing redacted diagnostics dialog;
- no profile details, endpoint, URI, UUID, IP, host, port, password,
  token, subscription URL or raw diagnostics logs in ordinary Settings UI.

This stage must not change VPN business logic, import parser behavior,
storage, diagnostics redaction, release metadata or published assets.

## Stage 5 Icon fidelity pass

The dedicated icon fidelity pass follows the live Home, Profiles, Import
and Settings/Privacy mapping. Scope:

- replace the temporary live line icons with a more consistent
  `GmLineIcon` Canvas set;
- keep a single 2dp line style with rounded caps and joins;
- align bottom navigation icons for Home, Profiles, Import and Settings;
- align card/action icons for shield/status, routing, diagnostics,
  privacy, kill switch, import/download, active status, edit, delete,
  latency and chevrons;
- use generated PNG icon tiles only as reference material from ignored
  `.local/design-assets`;
- do not commit PNG icon tiles or full reference sheets.

This stage must not change VPN business logic, import parser behavior,
profile storage, diagnostics redaction, release metadata or published
assets.

## Review PR #14 checkpoint

PR #14: `https://github.com/GronGM/GMvpn2/pull/14`.

Scope:

- Home mapped.
- Profiles mapped.
- Import mapped.
- Settings/Privacy mapped.
- Icon fidelity pass done.
- Privacy UI preserved.
- Business logic unchanged.
- Release, tag and GitHub Release assets unchanged.

Review status:

- Hidden/bidi Unicode check over changed text files: pass.
- Control/format character check over changed text files: pass.
- Four-tab no-profile physical visual QA: pass on debug build.
- UI privacy dump scan: pass for controlled no-profile dumps.
- Basic accessibility label proxy from UI dumps: pass.
- Live manual invalid-input check via ADB input was inconclusive; no
  persistent user-visible error was captured after saving short
  synthetic input, so this needs manual recheck before RC.
- Crash/ANR markers after smoke: 0.
- Approved real subscription endpoint reachability from Windows: pass,
  with the endpoint value redacted from docs and reports.
- Manual real subscription import on physical Android: pass, 4 of 4
  profiles imported. Codex did not receive or print the raw
  subscription value.
- Real VPN smoke: pass after manual import.
- Internet through VPN: pass via HTTPS connectivity probe with output
  suppressed.
- Connect / disconnect / reconnect: pass, two reconnect cycles.
- Diagnostics redaction with real VPN profile: pass-limited because
  clipboard readback was unavailable; no raw diagnostics were printed,
  exported, or committed.
- Synthetic invalid-import error visibility: pass after returning the
  debug app to a no-profile state; the error was visible, synthetic raw
  input stayed masked, and no URL/URI/IP/UUID/base64 markers were
  visible in the safe UI dump.
- Accessibility/TalkBack: pass-limited; no-profile accessibility proxy
  found no focusable unlabeled blocker, but full TalkBack audio QA
  remains future work.

Remaining blockers before any tester RC:

- final visual acceptance;
- optional full diagnostics clipboard/export readback if required;
- optional full TalkBack audio QA if required;
- separate UDP/IPv6 production-readiness decision.

## Privacy constraints

Normal UI must not show:

- server IP;
- host/domain;
- port;
- UUID;
- password;
- raw URI;
- base64 payload;
- subscription URL;
- query-like secret labels;
- private config contents.

Allowed UI data:

- safe profile name;
- protocol type;
- active/saved state;
- latency;
- non-sensitive created/updated/source metadata;
- redacted diagnostics result message.

Manual profile and subscription inputs are masked on screen. Import
preview uses safe names only. Subscription errors are mapped to safe
categories instead of echoing raw exception text.

## Release risk

This is a product UI branch. It changes runtime UI and subscription error
copy, so it requires:

- unit tests;
- lint;
- debug build;
- physical smoke on a real device before any future RC;
- privacy check that no endpoint/URI/UUID/password appears in ordinary
  UI;
- explicit approval before any `v1.1.0-rc.1` or other release.

No release, tag, release asset update, or Google Play action is approved
by this plan.
