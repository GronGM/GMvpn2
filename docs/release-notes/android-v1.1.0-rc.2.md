# GMvpn Android v1.1.0 RC2

Это тестовый GitHub-only RC build для ручной установки APK. Google Play
не используется.

## Что изменилось

- Добавлен первый небольшой Stage 4 UI adoption: главный Home connection
  hero использует safe `ConnectionState` path.
- UI больше не должен показывать optimistic connected-looking state от
  button click или legacy `TunnelStatus` alone.

## Как тестировать

1. Скачать signed APK из draft GitHub Release.
2. Установить APK вручную.
3. Импортировать профиль безопасным способом.
4. Проверить disconnected, connect, connected, disconnect и restart UI
   behavior.

## Ограничения

- connected-state local proxy non-reachability: `pass-limited`;
- DNS leak audit: `pass-limited`;
- UDP/iperf: not tested;
- IPv6: not tested;
- Transport Override: not included.

Это не unrestricted `v1.0.0` readiness и не RC-ready for Google Play.
Not unrestricted `v1.0.0` readiness.
