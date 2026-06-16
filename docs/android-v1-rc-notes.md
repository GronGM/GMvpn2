# Android v1 RC notes

Status: release candidate, not production/public distribution.

Proposed RC name: `android-v1.0.0-rc.1`

Android package metadata:

- `applicationId`: `com.gmvpn.client`
- debug package: `com.gmvpn.client.debug`
- release package: `com.gmvpn.client`
- `versionCode`: `1000001`
- `versionName`: `1.0.0-rc.1`

The RC tag has not been created. A public release has not been
published. Signed RC artifacts were produced by the manual release
workflow; public distribution still requires separate explicit
tag/release approval.

## RC tag approval package

- RC candidate: `android-v1.0.0-rc.1`
- Artifact source SHA: `1775829107eac1066af911353fc17f8d11f24a18`
- Docs/audit HEAD after artifact verification:
  `a2fe00a5677665a44ab6b1396a50acf2e28f0d42`
- Workflow run URL/ID:
  `https://github.com/GronGM/GMvpn2/actions/runs/27632339860`,
  `27632339860`
- APK/AAB signed: yes
- APK signature verified: yes
- Checksums verified: yes
- Secrets exposed: no
- Tag/release still require explicit approval.

## Confirmed checks

- Debug build and unit tests passed during the 2026-06-15
  release-readiness audit.
- The post-packaging Gradle command passed with `versionCode`
  `1000001` and `versionName` `1.0.0-rc.1`.
- `:app:assembleDebugAndroidTest` passed during the 2026-06-15 audit.
- `:app:lintDebug` passed after the narrow VPN-service lint
  suppression.
- `:app:assembleRelease` and `:app:bundleRelease` passed locally as
  unsigned release-shaped artifacts.
- Local `apksigner verify` confirmed `app-release-unsigned.apk` is not
  signed, which is expected without release signing secrets.
- Manual workflow run `27632339860` on 2026-06-16 produced signed RC
  artifacts for `android-v1.0.0-rc.1`:
  `gmvpn-android-android-v1.0.0-rc.1-signed` and
  `gmvpn-android-android-v1.0.0-rc.1-unsigned-audit`.
- Downloaded signed APK verification passed locally with `apksigner`;
  APK Signature Scheme v2 was verified with one signer.
- Downloaded `signed-rc.sha256` and `unsigned-audit.sha256` matched all
  local artifact files.
- Physical validation passed on TECNO LG8n, Android 12/API 31.
- Real connect/browse/disconnect passed on the physical device.
- IPv4 egress passed through the active VPN path.
- DNS leak audit passed with redacted browser-based evidence.
- Always-on VPN and Block connections without VPN passed on the
  physical device.
- Wi-Fi/cellular handover reconnect passed on the physical device.
- UDP-heavy validation is `pass_limited`: browser WebRTC/STUN plus a
  5-minute YouTube/QUIC-style playback window worked through the VPN.

## Known limitations

- UDP was not measured with a controlled iperf throughput/loss target.
- IPv6 was `not_applicable` on the tested TECNO/network because there
  was no underlying IPv6 default route. Re-run on an IPv6-capable
  network before claiming broad IPv6 tunneling support.
- Release signing and distribution are separate from the local audit.
  Signed RC artifacts exist, but distribution still requires explicit
  tag/release approval.
- A GitHub Release and git tag must not be created without explicit
  approval.

## Known-good device

- TECNO LG8n, Android 12/API 31

## Evidence handling

Do not put raw profile URIs, UUIDs, real IP addresses, server names,
screenshots, raw logs, diagnostics bundles, keystores, or credentials
in these notes. Keep raw evidence only in ignored local artifacts and
share redacted summaries.
