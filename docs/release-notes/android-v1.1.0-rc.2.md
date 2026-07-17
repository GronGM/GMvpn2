# GMvpn Android v1.1.0 RC2

Это тестовый GitHub-only RC build для ручной установки APK. Google Play
не используется.

## Что изменилось относительно RC1

- Импорт подписок: устойчивый Base64-декод — поддержан по-строчный
  Base64 (каждая строка — отдельный конверт) и двойной Base64.
  Прежний блокер импорта (`ParseFailed` на валидном Base64-теле)
  на этой сборке не воспроизводится.
- XHTTP: `type=xhttp` принимается как алиас `splithttp` в vless/vmess/
  trojan URI; для этого транспорта теперь генерируются `xhttpSettings`
  (path/host) и network `xhttp` в конфиге Xray. Раньше такие профили
  тихо падали в `tcp` без настроек транспорта.
- Stage 4 UI adoption: главный Home connection hero использует safe
  `ConnectionState` path; UI не показывает optimistic connected-state
  от button click или legacy `TunnelStatus` alone.

## Валидация этой сборки

- CI shared (fmt + clippy + tests) — pass; unit-тесты 60/60.
- Физический импорт реальной подписки (TECNO LG8n): pass — сохранён
  1 из 2 профилей; второй элемент — `hysteria2://`, протокол
  сознательно не поддерживается (per-line warning, roadmap P5).
- Физический connect smoke (строгий метод): pass — активная VPN-сеть
  с `IS_VPN`/`IS_VALIDATED`/`INTERNET`, интерфейс `tun0`, маршруты
  `0.0.0.0/0` и `::/0` через туннель; crash/ANR маркеров 0.

## Как тестировать

1. Скачать signed APK из GitHub Pre-release (только APK-asset, не
   Source code zip/tar.gz).
2. Проверить SHA-256 по `.sha256`-файлу.
3. Установить APK вручную (обновление поверх RC1 сохраняет профили).
4. Импортировать профиль или подписку безопасным способом.
5. Проверить connect / disconnect / reconnect и restart-поведение.

В отчётах не прикладывайте приватные профили, ссылки подписок, пароли,
токены, ключи, raw logcat, нередактированные IP или скриншоты с
персональными данными.

## Ограничения

- `hysteria2://` профили не поддерживаются (импорт помечает их
  предупреждением, остальные профили импортируются);
- DNS leak audit: `pass-limited` (уровень RC1);
- UDP/iperf: `pass-limited` (уровень RC1, порог не утверждён);
- IPv6: `not_tested`;
- полный TalkBack QA: future work.

Это не unrestricted `v1.0.0`/`v1.1.0` readiness и не публикация в
Google Play.
