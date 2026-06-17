# GMvpn2

Современный кроссплатформенный VPN-клиент на базе [Xray-core].

Общая доменная логика находится в Rust workspace; каждая платформа
поставляет собственный нативный UI и системную интеграцию.

## Структура репозитория

```text
core/         обёртка Xray-core и сборки движка для платформ (Go)
shared/       Rust workspace:
  gmvpn-core  доменная логика: профили, подписки, URI-парсеры, routing
  gmvpn-ffi   FFI-граница для клиентов: UniFFI + C-ABI, планируется
clients/      нативные приложения: android/ios/macos/windows/linux
schemas/      JSON Schemas — единый источник правды для формы конфигов
docs/         архитектура, ADR, заметки по платформам, постоянный контекст
```

## С чего начать

- [`CLAUDE.md`](CLAUDE.md) — стек, принципы и соглашения
- [`docs/architecture.md`](docs/architecture.md) — слоистая архитектура
- [`docs/memory/project-context.md`](docs/memory/project-context.md) — цели
  и границы проекта
- [`docs/memory/pending-decisions.md`](docs/memory/pending-decisions.md) —
  открытые решения

## Быстрый старт shared crate

```sh
cd shared
make test       # cargo test (workspace)
make clippy     # cargo clippy -D warnings
make fmt-check  # cargo fmt --check

# UniFFI bindings: генерируются локально, не коммитятся
make kotlin     # bindings/kotlin/…
make swift      # bindings/swift/…
make python     # bindings/python/…
```

CI запускает fmt, clippy и тесты на каждом push и PR, которые затрагивают
`shared/`.

## Быстрый старт Core (Go)

```sh
cd core
make test   # go test ./...
make vet
```

`make android` собирает `build/gmvpn.aar` через `gomobile bind`, когда
установлены Android NDK и gomobile toolchain. Подробности — в
[`core/README.md`](core/README.md).

## Android-клиент

```sh
cd clients/android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest

# Нужен эмулятор или подключённое устройство; это пока не обязательная часть CI.
./gradlew :app:connectedDebugAndroidTest
```

Требуются JDK 17+ и Android SDK с `compileSdk 35`.

## Android APK для тестеров

Текущая опубликованная ручная тестовая сборка — GitHub Pre-release
[`android-v1.0.0-rc.4`][android-rc4]. Для установки скачивайте
`GMvpn-android-v1.0.0-rc.4.apk` из assets релиза. Не скачивайте
GitHub-архивы "Source code" для тестирования Android-приложения:
это снимки исходного кода, а не устанавливаемые APK.

RC4 (`versionName` `1.0.0-rc.4`, `versionCode` `1000004`) опубликован
как pre-release после privacy-fix отображения сохранённых профилей. RC4
скрывает из обычных
лейблов сохранённых профилей IP сервера, hostname/domain, port, UUID,
password, raw URI, query-like secrets и base64 payload; в списке
профилей остаются только безопасный человекочитаемый fragment /
`vmess.ps` либо общий fallback вроде `VLESS профиль`. RC3 остаётся
доступен как предыдущая тестовая сборка, но для новых проверок
рекомендуется RC4.

Известные ограничения RC4: DNS leak audit остаётся `pass-limited`,
UDP/iperf не проверялся, IPv6 не проверялся. Это тестовая pre-release
APK-сборка, не production release.

Перед установкой по возможности проверьте APK по файлу
`GMvpn-android-v1.0.0-rc.4.apk.sha256`. Установите APK на тестовое
Android-устройство, подтвердите системный Android VPN permission
dialog, проверьте подключение, отключение и повторное подключение с
тестовым профилем и оставьте баг или отзыв через GitHub issue templates.

В отчётах не прикрепляйте приватные VPN-профили, ссылки подписок,
subscription URLs, пароли, токены, приватные ключи, raw logcat,
нередактированные IP-адреса или скриншоты с персональными данными.

Два нативных артефакта, с которыми линкуется приложение, собираются одним
скриптом:

```sh
./scripts/build-android-libs.sh
```

CI делает то же самое на каждом push в `shared/` или `core/` через
`.github/workflows/android-aar.yml`; артефакты загружаются как
`gmvpn-android-libs-<sha>.zip`. Полная инструкция находится в
[`clients/android/README.md`](clients/android/README.md).

## Статус

Для Android v1 release candidate уже есть signed GitHub Actions
артефакты для `android-v1.0.0-rc.1`; этот tag остаётся привязан к
исходному RC1 source SHA. В post-RC/P1 source есть signed RC3 candidate
артефакты для SDK 35, 16 KB native readiness и проверки release
blocker cleanup. После этого опубликован GitHub Pre-release RC4 с
privacy-safe отображением сохранённых профилей. Это не заявление о
production/public distribution: GitHub Pre-release RC4 предназначен
только для ручного тестирования APK, physical validation остаётся
pass-limited, публикация в Google Play не начиналась, а финальный
`android-v1.0.0` всё ещё требует отдельного release decision. Тестеры
могут скачать текущий опубликованный signed APK из
[GMvpn Android v1.0.0 RC4 pre-release][android-rc4].

Что уже есть:

- **Shared Rust core** — модели профилей, подписок и routing, парсеры
  `vless://`, `vmess://`, `trojan://`, `ss://` (SIP002 + legacy),
  декодер подписок (uri-list, base64-uri-list, SIP008), serde JSON,
  согласованный со `schemas/`.
- **UniFFI boundary** — `gmvpn-ffi` отдаёт типизированный Kotlin / Swift /
  Python API для общей логики через `#[uniffi::export]`. Bindings
  регенерируются командой `make kotlin|swift|python` в `shared/`.
- **Go Xray-core wrapper** — gomobile-friendly API `Tunnel` /
  `StatusListener`, покрытый unit-тестами. Он встраивает pinned Xray-core
  и gVisor tun2socks bridge для TCP и SOCKS5 UDP ASSOCIATE.
- **Android-клиент** — Gradle project, Compose UI, `VpnService` +
  foreground notification, VPN permission flow, encrypted multi-profile
  storage, subscription import confirmation, per-app routing, reconnect
  при изменении сети, diagnostics export и typed tunnel state machine.
  Если native artifacts отсутствуют, приложение показывает
  engine-unavailable error вместо crash. В P1-ветке отображение
  сохранённых профилей теперь privacy-safe: без IP/host/domain/port,
  UUID/password/raw URI/base64 в title/subtitle.
- **Android validation** — debug build/tests, physical validation на
  TECNO LG8n, release APK build и release bundle build прошли. UDP-heavy
  задокументирован как `pass_limited`, потому что доступный тест был
  browser WebRTC/STUN плюс 5-минутное YouTube/QUIC-style playback window,
  а не controlled iperf throughput/loss. IPv6 был `not_applicable` на
  проверенной связке TECNO/network, потому что не было underlying IPv6
  default route.
- **Android release packaging** — manual workflow run `27632339860`
  собрал signed RC APK/AAB artifacts и checksums 2026-06-16. Позже был
  создан RC1 tag; GitHub Release тогда не создавался. RC3 workflow run
  `27643689894` собрал signed APK, который сейчас прикреплён к GitHub
  Pre-release `android-v1.0.0-rc.3`; там опубликованы только APK и
  SHA-256 checksum для скачивания тестерами. RC4 workflow run
  `27672658765` собрал signed APK/AAB, а GitHub Pre-release
  `android-v1.0.0-rc.4` опубликован только с APK и SHA-256 checksum для
  тестеров. AAB не загружался в release assets.

Ключевые ADR:
[0001 Rust shared core](docs/adr/0001-rust-shared-core.md),
[0002 Android first + gomobile](docs/adr/0002-android-first-gomobile.md),
[0003 UniFFI bindings](docs/adr/0003-uniffi-bindings.md),
[0004 Xray-core pin + tun2socks](docs/adr/0004-xray-core-pin.md).

Дальше см. [docs/android-release-signing.md](docs/android-release-signing.md),
[docs/android-v1-rc-notes.md](docs/android-v1-rc-notes.md),
[docs/release-roadmap.md](docs/release-roadmap.md) и
[docs/android-device-validation.md](docs/android-device-validation.md):
там находится audit trail Android v1 release candidate, signing workflow
и оставшиеся шаги распространения.

[Xray-core]: https://github.com/XTLS/Xray-core
[android-rc3]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3
[android-rc4]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.4
