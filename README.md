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
- [`docs/memory/project-context.md`](docs/memory/project-context.md) —
  цели и границы проекта
- [`docs/memory/pending-decisions.md`](docs/memory/pending-decisions.md)
  — открытые решения

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

CI запускает fmt, clippy и тесты на каждом push и PR, которые
затрагивают `shared/`.

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

Текущая рекомендуемая ручная тестовая сборка - GitHub Pre-release
[`android-v1.1.0-rc.1`][android-v110-rc1]. Для установки скачивайте из
assets именно `GMvpn2-android-v1.1.0-rc.1-signed.apk`. Не скачивайте
GitHub-архивы `Source code zip/tar.gz` для тестирования
Android-приложения: это исходники, а не устанавливаемые APK.

`android-v1.1.0-rc.1` опубликован как GitHub pre-release для APK
тестеров. Это не unrestricted production/latest release и не публикация
в Google Play. AAB не загружен как пользовательский GitHub asset, потому
что Google Play не является ближайшей целью; для тестеров публикуются
signed APK и SHA-256 checksum.

Перед установкой по возможности проверьте APK по файлу
`GMvpn2-android-v1.1.0-rc.1-signed.apk.sha256`.

Ожидаемый SHA-256 APK:

```text
f8d64b5ee2e4d6e14c9aa0606124847ab747b1a8a683756ff7690e68a1325848
```

Установите APK на тестовое Android-устройство, подтвердите системный
Android VPN permission dialog, проверьте подключение, отключение и
повторное подключение с тестовым профилем и оставьте баг или отзыв через
GitHub issue templates.

В отчётах не прикрепляйте приватные VPN-профили, ссылки подписок,
пароли, токены, приватные ключи, raw logcat, нередактированные IP-адреса
или скриншоты с персональными данными.

## Android v1.1.0-rc.1

`android-v1.1.0-rc.1` опубликован как GitHub Pre-release с явно
принятыми RC-ограничениями. Это не unrestricted production release и не
публикация в Google Play.

В release notes для `1.1.0-rc.1` зафиксированы:

- diagnostics clipboard/export full readback: limited;
- full TalkBack QA: limited;
- UDP: `pass_limited`;
- IPv6: `not_tested`;
- AAB: не загружен как GitHub asset для обычных тестеров;
- Google Play не публикуется.

Исторический MVP/internal [`android-v1.0.0`][android-v100] остаётся
опубликованным GitHub Pre-release, но больше не является текущей
рекомендуемой сборкой для новых ручных проверок.

Два нативных артефакта, с которыми линкуется приложение, собираются
одним скриптом:

```sh
./scripts/build-android-libs.sh
```

CI делает то же самое на каждом push в `shared/` или `core/` через
`.github/workflows/android-aar.yml`; артефакты загружаются как
`gmvpn-android-libs-<sha>.zip`. Полная инструкция находится в
[`clients/android/README.md`](clients/android/README.md).

## Статус

Опубликован GitHub Pre-release `android-v1.1.0-rc.1` для ручного
тестирования Android APK. Release создан на annotated tag
`android-v1.1.0-rc.1`, который указывает на artifact source SHA
`9105255fefe077756b32df82ac898ab9d121c335` из workflow run
`27824970999`. В release assets загружены только:

- `GMvpn2-android-v1.1.0-rc.1-signed.apk`;
- `GMvpn2-android-v1.1.0-rc.1-signed.apk.sha256`.

AAB не загружался для обычных тестеров. Production/latest release и
Google Play publication не создавались. `android-v1.0.0` и RC5 остаются
историческими pre-release сборками; для новых ручных проверок
используйте [`android-v1.1.0-rc.1`][android-v110-rc1].

Что уже есть:

- **Shared Rust core** — модели профилей, подписок и routing, парсеры
  `vless://`, `vmess://`, `trojan://`, `ss://` (SIP002 + legacy),
  декодер подписок (uri-list, base64-uri-list, SIP008), serde JSON,
  согласованный со `schemas/`.
- **UniFFI boundary** — `gmvpn-ffi` отдаёт типизированный Kotlin / Swift
  / Python API для общей логики через `#[uniffi::export]`. Bindings
  регенерируются командой `make kotlin|swift|python` в `shared/`.
- **Go Xray-core wrapper** — gomobile-friendly API `Tunnel` /
  `StatusListener`, покрытый unit-тестами. Он встраивает pinned
  Xray-core и gVisor tun2socks bridge для TCP и SOCKS5 UDP ASSOCIATE.
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
  browser WebRTC/STUN плюс 5-минутное YouTube/QUIC-style playback
  window, а не controlled iperf throughput/loss. IPv6 был
  `not_applicable` на проверенной связке TECNO/network, потому что не
  было underlying IPv6 default route.
- **Android release packaging** — manual workflow run `27632339860`
  собрал signed RC APK/AAB artifacts и checksums 2026-06-16. Позже был
  создан RC1 tag; GitHub Release тогда не создавался. RC3 workflow run
  `27643689894` собрал signed APK, который сейчас прикреплён к GitHub
  Pre-release `android-v1.0.0-rc.3`; там опубликованы только APK и
  SHA-256 checksum для скачивания тестерами. RC4 workflow run
  `27672658765` собрал signed APK/AAB, а GitHub Pre-release
  `android-v1.0.0-rc.4` опубликован только с APK и SHA-256 checksum для
  тестеров. AAB не загружался в release assets. RC5 workflow run
  `27679203026` собрал signed APK/AAB, а GitHub Pre-release
  `android-v1.0.0-rc.5` опубликован только с APK и SHA-256 checksum для
  тестеров. RC5 включает profile management UX, safe profile names,
  rename, delete confirmation, active-profile reset, safe import
  preview, redacted diagnostics и network validation bench docs;
  известные ограничения DNS/UDP/IPv6 остаются прежними до отдельной
  сетевой проверки.

Ключевые ADR:

- [0001 Rust shared core](docs/adr/0001-rust-shared-core.md)
- [0002 Android first + gomobile](docs/adr/0002-android-first-gomobile.md)
- [0003 UniFFI bindings](docs/adr/0003-uniffi-bindings.md)
- [0004 Xray-core pin + tun2socks](docs/adr/0004-xray-core-pin.md)

Дальше см.:

- [docs/android-release-signing.md](docs/android-release-signing.md)
- [docs/android-v1-rc-notes.md](docs/android-v1-rc-notes.md)
- [docs/release-roadmap.md](docs/release-roadmap.md)
- [docs/android-device-validation.md](docs/android-device-validation.md)

Там находится audit trail Android v1 release candidate, signing workflow
и оставшиеся шаги распространения.

[Xray-core]: https://github.com/XTLS/Xray-core
[android-v110-rc1]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.1.0-rc.1
[android-v100]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0
[android-rc3]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3
[android-rc4]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.4
[android-rc5]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.5
