# AGENTS.md

## Назначение проекта

GMvpn2 - Android VPN-клиент с собственным Android UI, нативным
core/shared слоем и release pipeline через GitHub Actions. Текущий путь
развития:

1. GitHub APK pre-release для ручного тестирования.
2. Сбор feedback через GitHub Issues.
3. Усиление privacy/security/UX.
4. Закрытие сетевых validation gaps.
5. Только после этого - `v1.0.0` и возможная публикация в Google Play.

Проект развивается privacy-first. Для VPN-приложения доверие
пользователя важнее быстрых релизов.

## Текущий публичный канал тестирования

Актуальная публичная тестовая сборка на момент добавления этого файла:

- `android-v1.0.0-rc.5`
- GitHub Pre-release APK
- APK asset: `GMvpn-android-v1.0.0-rc.5.apk`

Если вышел новый RC, обнови этот раздел и `docs/agent-handoff.md`.

## Постоянные запреты

Никогда не коммить:

- APK/AAB;
- `.local/`;
- raw logs;
- raw diagnostics;
- VPN profiles;
- subscription URLs;
- IP/host/domain серверов;
- UUID/password/token/key;
- screenshots с приватными профилями;
- keystore, `.jks`, `.b64`;
- private release artifacts.

Никогда не печатай значения secrets. Разрешено проверять только имена
secrets.

Никогда не заменяй APK asset в уже опубликованном GitHub Release задним
числом. Новый build - новый tag/release.

Никогда не двигай существующие tags без отдельного явного approval.

Никогда не создавай `android-v1.0.0`, production/latest GitHub Release
или Google Play release без отдельного явного approval.

## Privacy rules для профилей

В обычном UI, preview, diagnostics, logs и issue-friendly отчётах нельзя
показывать:

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

Допустимо показывать:

- безопасное пользовательское имя профиля;
- безопасный fragment/label, если он не похож на endpoint/secret;
- тип протокола: VLESS, VMess, Trojan, Shadowsocks;
- безопасный fallback: `VLESS профиль`, `VMess профиль`,
  `Trojan профиль`, `Shadowsocks профиль`, `Профиль N`;
- latency, если она не раскрывает endpoint.

Если сомневаешься, лучше показать безопасный fallback, чем endpoint.

## Release policy

### RC releases

RC release можно делать только после:

- version bump;
- successful signed workflow;
- checksum verification;
- APK signature verification;
- AAB verification, если AAB produced;
- 16 KB ELF alignment check;
- APK `zipalign -P 16`;
- metadata check: package, versionCode, versionName, minSdk, targetSdk;
- basic physical smoke, если изменение затрагивает UX/runtime;
- secret scan.

RC tag должен указывать на exact artifact source SHA.

GitHub Pre-release для тестеров должен прикладывать только:

- signed APK;
- SHA-256 checksum.

AAB не загружать для обычных тестеров без отдельной причины.

### Production v1.0.0

`android-v1.0.0` заблокирован, пока не будет отдельного approval.

Unrestricted `v1.0.0` требует:

- UDP/iperf validation;
- full DNS leak audit;
- real IPv6 pass or fail-closed evidence;
- signed final workflow from exact release SHA;
- physical validation;
- privacy/compliance review.

MVP/internal `v1.0.0` возможен только с явным approval и явным указанием
ограничений DNS/UDP/IPv6.

## Known current blockers

На момент RC5 unrestricted `v1.0.0` блокируют:

- DNS leak audit: `pass-limited`;
- UDP/iperf: not tested;
- IPv6: not tested.

Не называй продукт production-ready, пока эти пункты не закрыты или
ограничения явно не приняты.

## Build and test commands

Базовые проверки Android:

```bash
./gradlew.bat :app:testDebugUnitTest --stacktrace
./gradlew.bat :app:lintDebug :app:assembleDebug --stacktrace
```

Перед release-related изменениями:

```bash
./gradlew.bat :app:assembleRelease :app:bundleRelease --stacktrace
```

Перед каждым commit/push:

```bash
git diff --check
git status --short --branch
```

Также выполняй:

- changed-files secret scan;
- staged secret scan;
- tracked high-confidence secret scan.

Если изменялись native libs/build scripts/release workflow:

- проверить 16 KB ELF alignment;
- проверить APK zipalign;
- проверить workflow commands;
- не считать pass без реального evidence.

## Код и стиль

- Делай маленькие commits.
- Commit message - короткий, imperative style, с областью:
  - `android: ...`
  - `docs: ...`
  - `ci: ...`
  - `release: ...`
- Не смешивай code changes и release publishing в одном commit.
- Не делай большой refactor без необходимости.
- Любой user-facing behavior change документируй в docs/README.
- Для строк UI поддерживай parity `values` / `values-ru`, если
  применимо.
- Новые privacy/security-sensitive функции должны иметь unit tests.

## Issue triage rules

Все новые issues:

- добавить `needs-triage`.

Если issue касается профилей, имён, IP, URI, скриншотов, логов или
imported configs:

- добавить `privacy-sensitive`;
- не цитировать приватные данные пользователя;
- попросить пользователя удалить/замазать секреты;
- не переносить секреты в docs/backlog.

Рекомендуемые labels:

- `android`
- `apk`
- `rc5`
- `bug`
- `tester-feedback`
- `needs-triage`
- `privacy-sensitive`
- `profile-label`
- `connectivity`
- `vpn-permission`
- `profile-import`
- `dns`
- `ipv6`
- `udp`
- `compatibility`

## Diagnostics rules

Redacted diagnostics may include:

- app version;
- versionCode;
- Android version/API;
- package name;
- connection state;
- last error category;
- protocol type;
- timestamp;
- device model only with user consent.

Diagnostics must not include:

- raw logcat;
- raw profile;
- full URI;
- IP/host/domain;
- UUID;
- password;
- token;
- subscription URL;
- private keys;
- cookies/auth headers.

## Network validation rules

Для release evidence не использовать случайные публичные endpoints.

UDP/iperf evidence должен быть от approved controlled endpoint.

DNS audit должен использовать минимум два независимых метода.

IPv6 acceptable outcomes:

1. IPv6 идёт через VPN, или
2. IPv6 blocked/fail-closed.

Unacceptable:

- local ISP IPv6 visible while VPN is connected.

Если нет real IPv6 network, статус `not tested`, не `pass`.

## Когда делать новый RC

Новый RC нужен, если есть:

- privacy/security bug;
- crash/ANR;
- install blocker;
- массовая проблема подключения;
- важный UX-fix, который нужен тестерам;
- существенная новая функция для тестирования.

Новый RC не нужен только из-за docs-only изменений, если тестерам не
требуется новый APK.

## Что делать в начале каждой задачи

1. Прочитать `AGENTS.md`.
2. Проверить текущую ветку.
3. Проверить `git status --short --branch`.
4. Понять, затрагивает ли задача:
   - code;
   - release;
   - docs;
   - privacy;
   - signing;
   - public assets.
5. Выбрать минимальный безопасный путь.
6. Не выполнять release/tag/publish действия без явного approval.

## Формат финального отчёта

Всегда возвращай:

- Branch:
- Commit hash:
- Files changed:
- Tests/checks run:
- Privacy/security impact:
- Release impact:
- Tags changed:
- GitHub Release changed:
- Google Play published:
- Secrets exposed:
- Remaining blockers:
- Need new RC:
- Safe next step:
