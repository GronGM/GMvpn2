# GMvpn premium UI plan

Status: started on branch `codex/p2-premium-ui-system`.

This sprint improves product quality and trust after the GitHub
MVP/internal `android-v1.0.0` pre-release. It does not approve a new
release, tag, GitHub Release asset, or Google Play publication.

Suggested future version: `v1.1.0-rc.1`, not an immediate release.

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
- Avoid color-only meaning; use labels such as `Защищено`, `Ошибка`,
  `Требуется профиль`.
- Keep Russian strings short enough for compact phones.
- Maintain `values` / `values-ru` string parity.
- Do not rely on screenshots for validation when real profiles are
  visible.

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
