#!/usr/bin/env bash
# Collect a redacted Android diagnostics bundle for GMvpn device
# validation. The script only uses adb shell/read-only dumpsys calls;
# it does not require root and it never uploads data anywhere.

set -u -o pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_GRADLE="$REPO_ROOT/clients/android/app/build.gradle.kts"

base_package() {
  sed -nE 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' \
    "$APP_GRADLE" | head -n 1
}

debug_suffix() {
  sed -nE 's/^[[:space:]]*applicationIdSuffix[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' \
    "$APP_GRADLE" | head -n 1
}

DEFAULT_PACKAGE="$(base_package)"
DEFAULT_PACKAGE="${DEFAULT_PACKAGE:-com.gmvpn.client}$(debug_suffix)"
PACKAGE_NAME="${1:-${GMVPN_ANDROID_PACKAGE:-$DEFAULT_PACKAGE}}"
TIMESTAMP="$(date -u '+%Y%m%d-%H%M%SZ')"
OUT_DIR="$REPO_ROOT/artifacts/android-diagnostics/$TIMESTAMP"
README="$OUT_DIR/README.txt"

mkdir -p "$OUT_DIR"

log() {
  printf '%s\n' "$*" | tee -a "$README" >&2
}

redact_stream() {
  sed -E \
    -e 's#[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}#<uuid>#g' \
    -e 's#(vless|vmess|trojan|ss)://[^[:space:]"<>]+#\1://<redacted-profile-uri>#g' \
    -e 's#(^|[?&;[:space:]])(password|pwd|pw|secret|token|key|pbk|sid|spx)=([^&[:space:]"]+)#\1\2=<redacted>#g' \
    -e 's#(Authorization|authorization|Cookie|cookie|X-Api-Key|x-api-key)[[:space:]]*[:=][[:space:]]*.*#\1: <redacted>#g'
}

quote_cmd() {
  printf '%q ' "$@"
}

capture_adb() {
  local file="$1"
  shift
  {
    printf '$ '
    quote_cmd "${ADB[@]}" "$@"
    printf '\n'
    "${ADB[@]}" "$@" 2>&1
    local rc=$?
    if [[ "$rc" -ne 0 ]]; then
      printf '\n[gmvpn] command exited with status %s\n' "$rc"
    fi
  } | redact_stream > "$OUT_DIR/$file"
}

write_header() {
  cat > "$README" <<EOF
GMvpn Android diagnostics
timestamp_utc: $TIMESTAMP
package: $PACKAGE_NAME

This bundle is generated locally by scripts/collect-android-diagnostics.sh.
It is redacted before writing, but review every file before sharing.
Manually remove server hostnames, account names, public keys, short IDs,
subscription URLs, access tokens, cookies, and any profile URI that survived
redaction.
EOF
}

write_header

if ! command -v adb >/dev/null 2>&1; then
  log "adb was not found in PATH. Install Android platform-tools and retry."
  log "Output directory: $OUT_DIR"
  exit 0
fi

ADB=(adb)
if [[ -n "${ANDROID_SERIAL-}" ]]; then
  ADB=(adb -s "$ANDROID_SERIAL")
fi

capture_adb "adb-devices.txt" devices -l

if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
  log "No authorized Android device is available to adb."
  log "Connect one device, accept the USB debugging prompt, or set ANDROID_SERIAL."
  log "Output directory: $OUT_DIR"
  exit 0
fi

device_count="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ -z "${ANDROID_SERIAL-}" && "$device_count" -gt 1 ]]; then
  log "Multiple adb devices are connected. Set ANDROID_SERIAL or pass -s through ANDROID_SERIAL."
  log "Output directory: $OUT_DIR"
  exit 0
fi

{
  printf 'package: %s\n' "$PACKAGE_NAME"
  printf 'timestamp_utc: %s\n' "$TIMESTAMP"
  printf 'adb_serial: %s\n' "${ANDROID_SERIAL:-default}"
  printf '\n[device]\n'
  "${ADB[@]}" shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r'
  "${ADB[@]}" shell getprop ro.product.model 2>/dev/null | tr -d '\r'
  "${ADB[@]}" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r'
  "${ADB[@]}" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r'
  "${ADB[@]}" shell getprop ro.build.fingerprint 2>/dev/null | tr -d '\r'
} | redact_stream > "$OUT_DIR/device-info.txt"

if "${ADB[@]}" shell pm path "$PACKAGE_NAME" >/dev/null 2>&1; then
  {
    printf '$ '
    quote_cmd "${ADB[@]}" shell dumpsys package "$PACKAGE_NAME"
    printf '| grep version fields\n'
    "${ADB[@]}" shell dumpsys package "$PACKAGE_NAME" 2>&1 |
      grep -E 'versionCode|versionName|firstInstallTime|lastUpdateTime|installerPackageName|targetSdk|pkgFlags'
  } | redact_stream > "$OUT_DIR/app-version.txt"
else
  printf 'Package %s is not installed on the selected device.\n' "$PACKAGE_NAME" \
    > "$OUT_DIR/app-version.txt"
fi

LOG_TMP="$OUT_DIR/logcat.raw.tmp"
if "${ADB[@]}" logcat -d -v threadtime -t 1200 > "$LOG_TMP" 2>&1; then
  pid="$("${ADB[@]}" shell pidof "$PACKAGE_NAME" 2>/dev/null | tr -d '\r' | awk '{ print $1 }')"
  {
    if [[ -n "$pid" ]]; then
      printf '[gmvpn] filtering logcat by pid %s plus GMvpn tags\n' "$pid"
      awk -v pid="$pid" '
        index($0, " " pid " ") ||
        /GMvpn|Gmvpn|gmvpn|EngineBridge|GmvpnVpnService|TunnelController|DiagnosticsCollector|LogcatTail|VpnService/
      ' "$LOG_TMP"
    else
      printf '[gmvpn] package pid unavailable; filtering logcat by GMvpn tags\n'
      awk '/GMvpn|Gmvpn|gmvpn|EngineBridge|GmvpnVpnService|TunnelController|DiagnosticsCollector|LogcatTail|VpnService/' "$LOG_TMP"
    fi
  } | redact_stream > "$OUT_DIR/logcat-gmvpn.txt"
else
  redact_stream < "$LOG_TMP" > "$OUT_DIR/logcat-gmvpn.txt"
fi
rm -f "$LOG_TMP"

capture_adb "dumpsys-connectivity.txt" shell dumpsys connectivity
capture_adb "dumpsys-netd.txt" shell dumpsys netd
capture_adb "dumpsys-vpn.txt" shell dumpsys vpn

cat >> "$README" <<EOF

Files:
- adb-devices.txt
- app-version.txt
- device-info.txt
- logcat-gmvpn.txt
- dumpsys-connectivity.txt
- dumpsys-netd.txt
- dumpsys-vpn.txt

Recommended before sharing:
- Search the bundle for vless://, vmess://, trojan://, ss://.
- Search for Authorization, Cookie, X-Api-Key, password, token, pbk, sid, spx.
- Remove exact server hostnames, IP addresses, account names, and subscription URLs.
EOF

log "Diagnostics written to $OUT_DIR"
