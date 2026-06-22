# GMvpn2

Современный кроссплатформенный VPN-клиент на базе [Xray-core].

Общая доменная логика находится в Rust workspace.

Каждая платформа поставляет собственный нативный UI и системную
интеграцию.

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

- [`CLAUDE.md`](CLAUDE.md) — стек, принципы и соглашения.
- [`docs/architecture.md`](docs/architecture.md) — слоистая
  архитектура.
- [`docs/memory/project-context.md`](docs/memory/project-context.md) —
  цели и границы проекта.
- [`docs/memory/pending-decisions.md`](docs/memory/pending-decisions.md)
  — открытые решения.

## Быстрый старт shared crate

```sh
cd shared
make test
make clippy
make fmt-check

# UniFFI bindings генерируются локально и не коммитятся.
make kotlin
make swift
make python
```

CI запускает fmt, clippy и тесты на каждом push и PR, которые
затрагивают `shared/`.

## Быстрый старт Core (Go)

```sh
cd core
make test
make vet
```

`make android` собирает `build/gmvpn.aar` через `gomobile bind`, когда
установлены Android NDK и gomobile toolchain.

Подробности находятся в [`core/README.md`](core/README.md).

## Android-клиент

```sh
cd clients/android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest

# Нужен эмулятор или подключённое устройство.
# Это пока не обязательная часть CI.
./gradlew :app:connectedDebugAndroidTest
```

Требуются JDK 17+ и Android SDK с `compileSdk 35`.

## Android APK для тестеров

Текущая рекомендуемая ручная тестовая сборка — GitHub Pre-release
[`android-v1.1.0-rc.1`][android-v110-rc1].

Для установки скачивайте из release assets именно:

```text
GMvpn2-android-v1.1.0-rc.1-signed.apk
```

Не скачивайте GitHub-архивы `Source code zip/tar.gz` для тестирования
Android-приложения.

Это исходники, а не устанавливаемые APK.

`android-v1.1.0-rc.1` опубликован как GitHub pre-release для APK
тестеров.

Это не unrestricted production/latest release.

Это не публикация в Google Play.

AAB не загружен как пользовательский GitHub asset.

Google Play не является ближайшей целью.

Для тестеров публикуются signed APK и SHA-256 checksum.

Перед установкой по возможности проверьте APK по файлу:

```text
GMvpn2-android-v1.1.0-rc.1-signed.apk.sha256
```

Ожидаемый SHA-256 APK:

```text
f8d64b5ee2e4d6e14c9aa0606124847ab747b1a8a683756ff7690e68a1325848
```

Проверка на устройстве:

1. Установите APK на тестовое Android-устройство.
2. Подтвердите системный Android VPN permission dialog.
3. Проверьте подключение.
4. Проверьте отключение.
5. Проверьте повторное подключение с тестовым профилем.
6. Оставьте баг или отзыв через GitHub issue templates.

## Privacy warning для отчётов

В отчётах не прикрепляйте:

- приватные VPN-профили;
- ссылки подписок;
- raw URI;
- UUID;
- IP-адреса;
- host/domain/port;
- пароли;
- токены;
- приватные ключи;
- raw logcat;
- raw diagnostics;
- нередактированные скриншоты с персональными данными.

## Android v1.1.0-rc.1

`android-v1.1.0-rc.1` опубликован как GitHub Pre-release с явно
принятыми RC-ограничениями.

Это не unrestricted production release.

Это не публикация в Google Play.

В release notes для `1.1.0-rc.1` зафиксированы:

- diagnostics clipboard/export full readback: limited;
- full TalkBack QA: limited;
- UDP: `pass_limited`;
- IPv6: `not_tested`;
- AAB: не загружен как GitHub asset для обычных тестеров;
- Google Play: не опубликован.

Исторический MVP/internal [`android-v1.0.0`][android-v100] остаётся
опубликованным GitHub Pre-release.

Он больше не является текущей рекомендуемой сборкой для новых ручных
проверок.

## Android native artifacts

Два нативных артефакта, с которыми линкуется приложение, собираются
одним скриптом:

```sh
./scripts/build-android-libs.sh
```

CI делает то же самое на каждом push в `shared/` или `core/` через
`.github/workflows/android-aar.yml`.

Артефакты загружаются как `gmvpn-android-libs-<sha>.zip`.

Полная инструкция находится в
[`clients/android/README.md`](clients/android/README.md).

## Статус

Опубликован GitHub Pre-release `android-v1.1.0-rc.1` для ручного
тестирования Android APK.

Release создан на annotated tag `android-v1.1.0-rc.1`.

Tag указывает на artifact source SHA:

```text
9105255fefe077756b32df82ac898ab9d121c335
```

Signed workflow run:

```text
27824970999
```

В release assets загружены только:

- `GMvpn2-android-v1.1.0-rc.1-signed.apk`;
- `GMvpn2-android-v1.1.0-rc.1-signed.apk.sha256`.

AAB не загружался для обычных тестеров.

Production/latest release не создавался.

Google Play publication не создавалась.

`android-v1.0.0` и RC5 остаются историческими pre-release сборками.

Для новых ручных проверок используйте
[`android-v1.1.0-rc.1`][android-v110-rc1].

## Что уже есть

### Shared Rust core

Shared Rust core содержит:

- модели профилей;
- модели подписок;
- routing-модели;
- парсеры VLESS, VMess, Trojan и Shadowsocks;
- декодер подписок;
- serde JSON;
- согласование со `schemas/`.

### UniFFI boundary

`gmvpn-ffi` отдаёт типизированный Kotlin, Swift и Python API для общей
логики через `#[uniffi::export]`.

Bindings регенерируются командой `make kotlin|swift|python` в
`shared/`.

### Go Xray-core wrapper

Go wrapper предоставляет gomobile-friendly API:

- `Tunnel`;
- `StatusListener`.

Wrapper покрыт unit-тестами.

Он встраивает pinned Xray-core и gVisor tun2socks bridge для TCP и
SOCKS5 UDP ASSOCIATE.

### Android-клиент

Android-клиент включает:

- Gradle project;
- Compose UI;
- `VpnService`;
- foreground notification;
- VPN permission flow;
- encrypted multi-profile storage;
- subscription import confirmation;
- per-app routing;
- reconnect при изменении сети;
- diagnostics export;
- typed tunnel state machine.

Если native artifacts отсутствуют, приложение показывает
engine-unavailable error вместо crash.

В P1-ветке отображение сохранённых профилей privacy-safe:

- без IP;
- без host/domain/port;
- без UUID/password;
- без raw URI;
- без base64 в title/subtitle.

### Android validation

Проверены:

- debug build/tests;
- physical validation на TECNO LG8n;
- release APK build;
- release bundle build.

UDP-heavy задокументирован как `pass_limited`.

Доступный тест был browser WebRTC/STUN плюс 5-минутное YouTube/QUIC-style
playback window.

Это не controlled iperf throughput/loss.

IPv6 был `not_applicable` на проверенной связке TECNO/network, потому
что не было underlying IPv6 default route.

### Android release packaging

Manual workflow run `27632339860` собрал signed RC APK/AAB artifacts и
checksums 2026-06-16.

Позже был создан RC1 tag.

GitHub Release тогда не создавался.

RC3 workflow run `27643689894` собрал signed APK, который был
прикреплён к GitHub Pre-release `android-v1.0.0-rc.3`.

В RC3 release были опубликованы только APK и SHA-256 checksum.

RC4 workflow run `27672658765` собрал signed APK/AAB.

GitHub Pre-release `android-v1.0.0-rc.4` опубликован только с APK и
SHA-256 checksum для тестеров.

AAB не загружался в release assets.

RC5 workflow run `27679203026` собрал signed APK/AAB.

GitHub Pre-release `android-v1.0.0-rc.5` опубликован только с APK и
SHA-256 checksum для тестеров.

RC5 включает:

- profile management UX;
- safe profile names;
- rename;
- delete confirmation;
- active-profile reset;
- safe import preview;
- redacted diagnostics;
- network validation bench docs.

Известные ограничения DNS/UDP/IPv6 остаются до отдельной сетевой
проверки или явно принятого release decision.

## Ключевые ADR

- [0001 Rust shared core](docs/adr/0001-rust-shared-core.md)
- [0002 Android first + gomobile](docs/adr/0002-android-first-gomobile.md)
- [0003 UniFFI bindings](docs/adr/0003-uniffi-bindings.md)
- [0004 Xray-core pin + tun2socks](docs/adr/0004-xray-core-pin.md)
- [0005 Connection Orchestrator and Runtime Transport Override](docs/adr/0005-connection-orchestrator-and-transport-override.md)

## Дополнительная документация

- [docs/android-release-signing.md](docs/android-release-signing.md)
- [docs/android-v1-rc-notes.md](docs/android-v1-rc-notes.md)
- [docs/release-roadmap.md](docs/release-roadmap.md)
- [docs/android-device-validation.md](docs/android-device-validation.md)
- [docs/product-direction.md](docs/product-direction.md)

Эти документы содержат audit trail Android release candidates, signing
workflow, validation notes и дальнейшее направление продукта.

[Xray-core]: https://github.com/XTLS/Xray-core
[android-v110-rc1]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.1.0-rc.1
[android-v100]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0
[android-rc3]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.3
[android-rc4]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.4
[android-rc5]: https://github.com/GronGM/GMvpn2/releases/tag/android-v1.0.0-rc.5
