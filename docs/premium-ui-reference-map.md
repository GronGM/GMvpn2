# GMvpn premium UI reference map

Эта карта разбивает визуальные референсы на небольшие пакеты
реализации. Цель - приблизить Android UI к premium dark VPN app без
большого неконтролируемого редизайна и без риска раскрыть приватные
данные профилей.

## Asset audit - `gmvpn_assets_split_png.zip`

Архив распаковывается только локально в ignored-папку
`.local/design-assets/`. PNG из архива являются concept crops с
непрозрачным фоном, поэтому production UI должен собираться из Compose,
VectorDrawable или вручную подготовленных прозрачных ассетов.

| Asset group | Use in app | Action |
| --- | --- | --- |
| `00_full_screens` | Только reference | Не коммитить |
| `01_brand_identity/app_icons` | Launcher / splash candidate | Использовать только выбранный чистый вариант |
| `01_brand_identity/logos` | Reference only / possible About screen | Не использовать как большой Home header |
| `02_ui_icons_tiles` | Icon style reference | Перерисовать как VectorDrawable / Compose icons |
| `03_status_cards` | Reference для `ConnectionHeroCard` states | Не вставлять PNG-карточки |
| `04_profiles_locations/flags` | Можно использовать точечно | Только если clean PNG, без endpoint mapping |
| `04_profiles_locations/*cards/*rows` | Reference для Compose-компонентов | Не вставлять PNG |

Текущее решение: full-screen, status-card, profile-row и location-card PNG
не входят в production assets. Флаги не добавлены: текущий UI использует
Compose-отрисовку в preview и не делает country inference из endpoint,
IP, URI или host.

## Component fidelity pass

Этот pass уточняет reference previews на уровне компонентов. Цель -
приблизить форму, плотность и состояния к архиву, не вставляя PNG-карты,
строки или экраны в production UI.

| Элемент референса | Текущий статус | Целевая реализация | Gap |
| --- | --- | --- | --- |
| Primary button | ближе | Compose `ReferencePrimaryButton` по архиву | Требуется ручной visual review цвета и icon/tint |
| Profile row | ближе | Compose `ProfileReferenceRow` со состояниями active/inactive/focused/unavailable | Требуется ручной visual review плотности на устройстве |
| Status card | ближе | Compose `ReferenceConnectionHeroCard` | Connecting/testing/error states ещё не вынесены в отдельный live flow |
| Bottom nav | ближе | Compose/Material nav по архиву | Требуется ручной visual review баланса icon/label |
| Line icons | примерно | `GmLineIcon`/Canvas line icon set | Нужен отдельный stroke/color polish pass перед live rollout |
| Badges/pills | ближе | `ReferenceStatusBadge`, `ReferenceLatencyPill`, `ReferenceActiveBadge` | Требуется проверить длинные русские labels |
| Outline action | ближе | `ReferenceOutlineButton` для действия профиля | Требуется проверить обрезку на малых ширинах |
| Destructive action | ближе | `ReferenceDestructiveButton` | Требуется проверить muted red на реальном экране |

Preview-only scope:

- только mock data;
- без реальных профилей, endpoints, raw URI, UUID, tokens или
  subscription URLs;
- screenshots остаются в ignored `.local/premium-reference-preview/`;
- live Home/Profile/Import/Settings flow не переключается на эти previews,
  пока визуальное направление не принято.

## v3 visual gaps

### Home

- Hero card still too heavy.
- Primary CTA too flat/large.
- Shield icon looks placeholder-like.
- Status card needs more archive-like glass/highlight.

### Profiles

- Action pills/buttons too bulky.
- Flags are too simplified.
- Active/inactive/focused rows need closer archive styling.
- Kebab/menu icon too generic.

### Import

- Inputs/buttons need archive-like height, radius and internal highlight.
- Cards need lighter glass feel.
- Helper text should be calmer and shorter.

### Privacy

- Icons need premium line style.
- Cards need lighter structure.
- Text hierarchy should be calmer.

## v4 review

Status: close but not final.

Accepted:

- Общая структура Home.
- Общая структура Profiles.
- Общая структура Import.
- Общая структура Privacy.
- Bottom navigation direction.
- Profile row direction.
- Button direction.

Still needs polish:

- Home hero is still too visually heavy.
- Icons are still too generic.
- Import cards and CTA are still too large/heavy.
- Privacy cards still have too much text weight.
- Bottom nav can be lighter.
- Overall archive fidelity should be improved one more pass.

## v5 acceptance

Status: accepted as the visual baseline for live UI mapping.

Decision:

```text
v5 reference previews accepted as baseline.
Layout, density, cards, buttons, profile rows and bottom navigation are accepted for live UI mapping.
Icons are accepted as temporary/reference-quality and can be improved in a later dedicated icon fidelity pass.
No release/tag is authorized.
```

Accepted for live mapping:

- Home structure, density and first-screen hierarchy.
- Connection hero proportions and primary CTA direction.
- Active profile card and saved profile preview direction.
- Profiles rows, active/inactive/unavailable states and compact action
  pills.
- Import screen card structure, masked input direction and CTA density.
- Privacy/settings cards and shorter trust-boundary copy.
- Bottom navigation height, icon/label balance and inactive alpha.

Not final:

- Icons are good enough for the first live mapping pass, but a later icon
  fidelity pass is allowed.
- Reference previews remain mock-only and do not become live product UI
  by themselves.
- Screenshots stay local in `.local/premium-reference-preview/` and are
  not committed.

## Stage 1 live Home mapping

Status: started on `codex/p2-live-home-premium-ui`.

Mapped in this stage:

- shared premium dark theme tokens;
- shared glass card, border, radius, button and profile row styling;
- app background;
- bottom navigation shell styling;
- live Home compact app mark;
- live Home connection hero;
- live Home active profile card;
- live Home routing/diagnostics tools;
- live Home saved profiles preview.

Not mapped deeply in this stage:

- Profiles live screen;
- Import live screen;
- Settings live screen;
- final icon fidelity.

Privacy notes:

- Home active/saved profile UI uses safe profile summaries only.
- Home error detail is redacted before display.
- No reference PNG screen/card/row assets are used as production UI.
- Business logic, tunnel lifecycle, import parsing, diagnostics export
  and release metadata are unchanged.

## Stage 2 live Profiles mapping

Status: started on `codex/p2-live-home-premium-ui`.

Mapped in this stage:

- live Profiles count/header area;
- active profile row;
- inactive profile rows;
- safe profile name, protocol and latency;
- active badge;
- outline select action;
- kebab/details action;
- muted destructive clear action.

Not mapped deeply in this stage:

- Import live screen;
- Settings live screen;
- unavailable/disabled profile state, because the current runtime model
  does not expose a separate unavailable profile source;
- final icon fidelity.

Behavior notes:

- profile storage model is unchanged;
- active profile selection uses the existing `onSelectProfile` path;
- details/rename/delete confirmation use the existing dialogs;
- active-profile reset behavior stays in the existing removal path.

Privacy notes:

- profile rows use safe profile summaries only;
- no endpoint, host/domain, port, UUID, password, raw URI, base64 payload
  or subscription URL is shown by the mapped rows;
- real-profile screenshots/UI dumps remain disallowed.

## Package 1 - Brand / App Identity

Источник: reference sheet с launcher icon, wordmark, splash,
favicon/badges.

Реализуется:

- adaptive launcher icon safe-zone;
- foreground/background layers для launcher icon;
- точечное использование чистого shield/G asset.

Файлы:

- `clients/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`;
- `clients/android/app/src/main/res/drawable/ic_launcher_foreground.xml`;
- `clients/android/app/src/main/res/drawable/ic_launcher_monochrome.xml`;
- `clients/android/app/src/main/res/mipmap-*/ic_launcher_foreground.png`.

Acceptance:

- shield/G не обрезается системной маской;
- icon читается в launcher и app info;
- целые raster reference sheets не коммитятся;
- splash/wordmark не добавляются без чистого transparent/vector source.

Privacy constraints:

- brand assets не должны содержать профили, endpoint, IP, host, UUID,
  subscription URL или приватные скриншоты.

## Package 2 - Icon System

Источник: reference sheet `24 Essential Icons`.

Реализуется:

- единый Compose line-icon набор `GmIconKind` / `GmLineIcon`;
- rounded caps/joins и единый stroke width;
- semantic colors через `GmStatusTone` и theme tokens;
- интерактивные иконки получают accessibility labels.

Файлы:

- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/components/PremiumComponents.kt`;
- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/HomeScreen.kt`.

Acceptance:

- иконки выглядят как единая система;
- нет PNG reference sheet в production assets;
- декоративные иконки не подменяют текстовые состояния;
- навигационные символы `<`, `>`, `...` заменены на line icons.

Privacy constraints:

- иконки не кодируют endpoint/location как источник истины;
- server/location icon не раскрывает host/IP.

## Package 3 - Connection State Cards

Источник: reference sheet с карточками `Не подключено`,
`Подключение…`, `Подключено и защищено`, `Тестирование профилей`,
`Импорт успешно завершён`, `Ошибка подключения`.

Реализуется:

- `ConnectionHeroCard` для основных tunnel states;
- progress/error sub-row;
- primary CTA только когда он нужен;
- diagnostics action для error state.

Файлы:

- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/HomeScreen.kt`;
- `clients/android/app/src/main/res/values/strings.xml`;
- `clients/android/app/src/main/res/values-ru/strings.xml`.

Acceptance:

- состояние понятно за 3 секунды;
- no-profile, disconnected, preparing, connected и error различимы;
- error не echo raw URI, URL, host или профиль;
- fake connected не создаётся.

Privacy constraints:

- active profile row показывает только safe display name, protocol и
  latency;
- error reason остаётся safe/user-visible category.

## Package 4 - Profile / Location Cards

Источник: reference sheet `VPN-локации и профили`.

Реализуется:

- `ProfileListItem` как основная safe profile row;
- `LatencyPill` для задержки;
- `ProfileBadge` для compact state labels;
- `LocationCard` и `CompactServerCard` для previews/design reuse.

Файлы:

- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/components/PremiumComponents.kt`;
- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/HomeScreen.kt`.

Acceptance:

- active/inactive state читается без агрессивного свечения;
- latency buckets различаются по semantic tone;
- cards/rows выглядят из одной design system;
- disabled/unavailable state должен быть добавлен отдельным follow-up,
  если появится runtime source для unavailable profiles.

Privacy constraints:

- profile/location cards не показывают endpoint, IP, host/domain, port,
  UUID, password, raw URI, base64 или subscription URL;
- flags/placeholders можно показывать только как user-facing label, а не
  как доказательство реального endpoint location.

## Package 5 - App Shell / Navigation / Screens

Источник: reference image с 4 телефонами.

Реализуется:

- bottom navigation: `Главная`, `Профили`, `Импорт`, `Настройки`;
- Home: compact `GMvpn` mark, hero, active profile, tools, saved preview;
- Profiles: count, profile rows, test all, clear;
- Import: subscription/manual cards with masked inputs;
- Settings: privacy/routing/kill-switch/diagnostics cards.

Файлы:

- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/HomeScreen.kt`;
- `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/theme/Theme.kt`;
- `clients/android/app/src/main/res/values*/strings.xml`.

Acceptance:

- пользователь понимает текущую вкладку;
- bottom labels всегда видимы;
- первый крупный блок Home - VPN status / CTA, а не брендовый header;
- один главный CTA на Home;
- нет одного хаотичного scroll screen.

Privacy constraints:

- import/manual inputs остаются masked;
- diagnostics остаётся redacted-first;
- обычный UI не раскрывает profile internals.

## Package 6 - Compose Previews

Реализуется:

- Home disconnected;
- Home connecting;
- Home connected;
- Home empty/no-profile;
- Home error;
- Profiles tab;
- Import tab;
- Privacy settings tab;
- Location/server cards.

Mock data:

- `Нидерланды`;
- `Германия`;
- `Франция`;
- `Великобритания`;
- `США`;
- `Япония`;
- `Сингапур`;
- `Финляндия`.

Acceptance:

- preview data не содержит real endpoint, raw URI, UUID, IP, host,
  subscription URL или password;
- previews помогают сверять visual system без физического устройства.

## Package 7 - Visual QA / Acceptance

Перед future RC:

- `git diff --check`;
- unit tests;
- `lintDebug`;
- `assembleDebug`;
- values / values-ru parity;
- changed/staged/tracked secret scans;
- physical smoke на реальном Android;
- UI dump privacy scan;
- full TalkBack/accessibility pass для основных flows.

Release note:

- этот reference map не одобряет новый tag/release;
- рекомендуемый future tester version для этого UI направления:
  `v1.1.0-rc.1`, только после отдельного explicit release approval.

## Live UI mapping plan

| Reference component | Live target | Notes |
| --- | --- | --- |
| Reference shell/background | App scaffold/theme | Use tokens, no image backgrounds |
| Home reference | HomeScreen | Preserve real tunnel states |
| ConnectionHeroCard | Home connection state block | Map Idle/Preparing/Connected/Error |
| ToolsCard | Routing/Diagnostics actions | No endpoint data |
| ActiveProfileCard | Active profile section | Safe labels only, no redundant heading |
| SavedProfilesPreview | Profiles tab, not Home | Removed from live Home in Stage 2.5 |
| Profiles reference | Profile management section/screen | Active/inactive/delete |
| Import reference | Import flow | Mask inputs, no raw URI echo |
| Privacy reference | Settings/privacy screen | Routing/privacy/kill-switch |
| ReferenceLineIcons | Temporary icon set | Can be improved later |

Implementation order:

1. Theme/tokens live integration: colors, shapes, typography, spacing and
   surfaces.
2. Home live mapping: connection hero, active profile, tools, saved
   profiles preview and bottom nav.
3. Profiles live mapping: rows, active/inactive state, rename, delete,
   details and safe labels only.
4. Stage 2.5 Home/Menu correction: Home/main menu is not visually
   accepted yet, so Import remains blocked until Home is accepted. This
   correction removes the redundant active profile heading and removes
   the saved profiles preview from live Home.
5. Import live mapping: masked subscription/manual input, safe preview
   and errors without raw URL/URI echo.
6. Privacy/settings live mapping: routing, privacy-first, kill switch and
   diagnostics notice.
7. Icon fidelity pass later; it is not a blocker for the first live
   layout/component mapping.

## Stage 2.5 Home/Menu correction map

Mapped in this correction pass:

- Home header is compact and does not include a large subtitle block.
- Connection hero is lighter, with a smaller status mark and one primary
  CTA.
- Routing and diagnostics remain secondary actions.
- Active profile card stays on Home, but without the redundant `Active
  profile` heading.
- Saved profiles preview is not shown on Home; profile browsing belongs
  to the Profiles tab.
- Bottom nav is lower and visually lighter.

Not mapped in this pass:

- Import screen;
- Settings/privacy screen;
- new icon fidelity pass;
- release or tag work.

Privacy checks required during live mapping:

- saved profiles list does not show endpoint;
- active profile card does not show endpoint;
- details do not show endpoint;
- import errors do not echo raw URL/URI;
- diagnostics copy/export remains redacted;
- synthetic UI dumps are clean;
- real profile UI dumps stay local only and are not committed.

Release boundary:

- do not create a tag;
- do not create a GitHub Release;
- do not publish Google Play;
- run debug/manual QA only until a separate explicit release approval is
  given.
