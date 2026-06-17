# Android network validation bench

This runbook defines the practical network evidence required before
claiming unrestricted Android v1.0.0 network readiness. Keep raw
endpoints, VPN profiles, subscription URLs, screenshots with personal
data, and raw logs out of git.

## Evidence handling rules

- Record only redacted summaries in committed docs.
- Do not commit raw IP addresses, private hostnames, profile URIs,
  subscription URLs, UUIDs, passwords, private keys, or raw logcat.
- Screenshots are acceptable only after all private data is fully
  redacted.
- If a required network or controlled endpoint is unavailable, record
  `blocked` or `not_tested`, not `pass`.

## Status vocabulary

Use these values consistently in release docs and checklists:

- `pass` - the required evidence exists and was reviewed.
- `pass_limited` - useful smoke evidence exists, but it does not meet
  the full release bar.
- `blocked` - the test could not run because a required endpoint,
  network, tool, device, or approval is missing.
- `not_tested` - no attempt has been made in the current release
  window.
- `fail` - the test ran and showed an unacceptable leak, disconnect,
  crash, or other release-blocking behavior.

Keep raw working files under an ignored local path such as
`.local/network-validation/<date>/`. Commit only the final redacted
summary.

## Windows preflight and runner

Use the Windows preflight before any physical-device or UDP validation:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validation/preflight-windows.ps1
```

The script checks git state, `adb`, authorized Android devices,
`iperf3`, `GMVPN_IPERF_HOST`, and `GMVPN_IPERF_PORT`. It may find
`adb.exe` in standard Android SDK locations and add it only to the
current PowerShell process. It may also find a user-installed WinGet
portable `iperf3.exe` even when the current shell has not reloaded PATH.
It does not print endpoint values and masks device serials in console
output.

To create a local redacted validation summary:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validation/run-network-validation-windows.ps1
```

The runner writes raw command output only under
`.local/validation/<timestamp>/` and writes a redacted
`summary-redacted.md` in the same ignored directory. Do not commit that
directory. If `iperf3` or the approved endpoint env vars are missing,
UDP is recorded as `blocked`. A successful Windows-host iperf run is
recorded as `pass_limited` unless the Android VPN traffic path is
separately confirmed. DNS and IPv6 remain manual evidence steps until
real evidence is captured.

## Controlled UDP / iperf

Use only an approved controlled endpoint. The endpoint can be any trusted
Linux host outside the phone's local network and outside the VPN server
itself. Do not use random public iperf servers as release evidence unless
they are explicitly approved for the release decision.

Before running the test, confirm:

- endpoint owner approved the test window and bitrate;
- endpoint is not the VPN server itself;
- endpoint is outside the phone's Wi-Fi/mobile local network;
- endpoint hostname/IP is treated as private unless explicitly public;
- endpoint address is redacted from committed docs;
- Android phone stays connected to the signed tester APK VPN profile;
- raw terminal logs and packet captures stay outside git.

Local preflight checks may record tool/endpoint availability without
printing private values:

```powershell
Get-Command iperf3 -ErrorAction SilentlyContinue
Get-ChildItem Env: | Where-Object {
    $_.Name -match '^(GMVPN_IPERF|IPERF3)_'
} | Select-Object -ExpandProperty Name
```

If those checks show no approved endpoint, record UDP as
`blocked: no approved controlled endpoint` even when local `iperf3`
tooling is present.

Server side:

```sh
iperf3 -s
```

Client side, from a trusted host behind the phone/VPN test path or from
Android-side tooling if available:

```sh
iperf3 -c <REDACTED_ENDPOINT> -u -b 5M -t 30 --get-server-output
```

Record:

- command with endpoint redacted;
- duration;
- target bitrate;
- packet loss;
- jitter;
- pass/fail;
- whether GMvpn stayed connected for the whole run.

Passing UDP evidence requires a completed run against the controlled
endpoint with acceptable packet loss/jitter for the test environment and
no tunnel disconnect over the Android GMvpn path. Windows PC to endpoint
connectivity is useful endpoint readiness evidence only; it remains
`pass_limited` and does not replace Android VPN-path throughput/loss
evidence. Browser WebRTC/STUN or video playback can be useful smoke
evidence, but it remains `pass_limited` and does not replace controlled
iperf throughput/loss evidence.

The Windows runner only treats UDP as release-grade `pass` when the
Android VPN path has been separately proven and
`GMVPN_ANDROID_VPN_PATH_CONFIRMED=yes` is set for that local run. Do not
set this variable for ordinary Windows PC to endpoint checks.

Redacted evidence template:

```yaml
controlled_udp_iperf:
  date: YYYY-MM-DD
  tester: redacted
  device: redacted_model_android_api
  apk: android-v1.0.0-rc.5
  vpn_profile: approved_profile_redacted
  endpoint: redacted_controlled_endpoint
  endpoint_redacted: true
  server_command: "iperf3 -s"
  client_command: "iperf3 -c <REDACTED_ENDPOINT> -u -b 5M -t 30 --get-server-output"
  duration_seconds: 30
  target_bitrate: 5M
  packet_loss_percent: value_or_redacted
  jitter_ms: value_or_redacted
  vpn_stayed_connected: true_or_false
  result: pass_or_fail_or_blocked
  notes: "No raw IPs, hostnames, profile data, or logs committed."
```

### Latest RC5 Android-side UDP evidence

2026-06-17 physical-device follow-up installed Termux from the official
`termux/termux-app` GitHub pre-release `v0.119.0-beta.3`, verified the
APK SHA-256 locally, installed `iperf3` 3.21 inside Termux, imported an
approved subscription into GMvpn RC5, and ran Android-side UDP tests
through active GMvpn on TECNO LG8n Android 12/API 31. Endpoint,
subscription, raw resolver addresses, raw IPs, hostnames, profiles, and
raw command output stayed under ignored `.local/` paths or were redacted;
none are committed.

Command shape:

```sh
iperf3 -c <REDACTED_ENDPOINT> -p <REDACTED_PORT> -u -b <BITRATE> -l 1200 -t 30 --get-server-output
```

Redacted aggregate matrix:

| Bitrate | Runs | VPN before/after | Packet loss min/avg/max | Jitter min/avg/max |
| --- | ---: | --- | --- | --- |
| 1M | 3 | yes/yes | 0% / 0% / 0% | 1.370 / 5.574 / 8.041 ms |
| 2M | 3 | yes/yes | 0% / 14.333% / 43% | 0.941 / 9.110 / 22.166 ms |
| 3M | 3 | yes/yes | 0% / 0.004% / 0.011% | 1.037 / 2.506 / 3.511 ms |
| 5M | 3 | yes/yes | 0% / 0.041% / 0.096% | 0.906 / 1.854 / 2.477 ms |

Best stable UDP result in this matrix: 5M, payload 1200 bytes, three
successful Android-side Termux runs over active GMvpn, max packet loss
0.096%, max jitter 2.477 ms, VPN still connected after each run.
Post-matrix logcat tail scan with case-sensitive GMvpn crash/ANR markers
found no GMvpn crash or ANR.

Overall UDP status remains `pass_limited`, not unrestricted `pass`,
because the release project has not approved a formal packet-loss
threshold, the 2M row had one high-loss outlier, and DNS/IPv6 release
gates remain incomplete.

## Full DNS leak audit

Run at least two independent DNS checks while the VPN is connected.

Acceptable methods include:

- a browser-based DNS leak page;
- a second independent web DNS leak page;
- a controlled resolver query from the test path.

Record only provider/country-level summaries:

- local ISP/router DNS observed: yes/no;
- VPN/provider DNS observed: yes/no;
- countries/providers observed, redacted as needed;
- whether GMvpn stayed connected.

Expected result: no local mobile/Wi-Fi ISP or router DNS appears while
VPN is connected. If only one browser-level method is available, keep the
status as `pass_limited`, not full `pass`.

Redacted evidence template:

```yaml
full_dns_leak_audit:
  date: YYYY-MM-DD
  tester: redacted
  device: redacted_model_android_api
  apk: android-v1.0.0-rc.5
  method_1: browser_dns_leak_page_redacted
  method_2: independent_dns_check_redacted
  local_isp_or_router_dns_observed: true_or_false
  vpn_or_provider_dns_observed: true_or_false
  providers_countries_observed: provider_country_summary_only
  vpn_stayed_connected: true_or_false
  screenshots_committed: false
  result: pass_or_pass_limited_or_fail
  notes: "No raw resolver IPs, account data, or screenshots committed."
```

## Real IPv6 validation

Test only on a network with real external IPv6.

Before VPN:

- confirm the device/network has an external IPv6 path;
- record only a redacted summary.

During VPN, acceptable results are:

1. IPv6 routes through the VPN endpoint, or
2. IPv6 is blocked/fail-closed with no local IPv6 leak.

Unacceptable result: local ISP IPv6 remains visible while GMvpn is
connected.

If no IPv6-capable network is available, record `not_tested`, not `pass`.
The v1.0.0 release decision must then either block on IPv6 evidence or
explicitly accept the IPv6 limitation for an MVP/internal release.

Redacted evidence template:

```yaml
real_ipv6_validation:
  date: YYYY-MM-DD
  tester: redacted
  device: redacted_model_android_api
  apk: android-v1.0.0-rc.5
  baseline_external_ipv6_available: true_or_false
  baseline_provider_country: provider_country_summary_only
  vpn_connected: true_or_false
  during_vpn_ipv6_result: tunneled_or_fail_closed_or_leaked_or_not_tested
  local_isp_ipv6_visible_during_vpn: true_or_false
  raw_ipv6_addresses_committed: false
  result: pass_or_fail_or_not_tested
  notes: "No raw IPv6 addresses or network dumps committed."
```

## Decision update checklist

After each network-validation run:

- update `docs/android-v1-validation-checklist.md`;
- update `docs/release-roadmap.md` if release gating changes;
- update `docs/agent-handoff.md` with the current tester-safe status;
- keep raw logs, screenshots, profiles, endpoints, and packet captures
  out of git;
- keep final `android-v1.0.0` blocked unless the selected approval path
  is explicitly authorized.

## v1.0.0 decision matrix

Current RC5 candidate status before any final v1.0.0 decision:

- DNS is now Android-side `pass` for this device/network. A 2026-06-17
  follow-up used two independent methods while GMvpn RC5 was connected:
  BrowserLeaks DNS in Android Chrome and a Termux `dig` controlled
  resolver query. Both recorded only provider/country-level evidence,
  observed no private/router DNS, and committed no raw IPs or screenshots.
- Controlled UDP/iperf is now Android-side `pass_limited` for RC5:
  Termux `iperf3` ran through active GMvpn against the approved
  controlled endpoint with endpoint redacted. The 1M, 3M, and 5M rows
  were stable with payload 1200 bytes. The 2M row had a high-loss
  outlier, and a 5-run 2M rerun reproduced one high-loss run
  (min/avg/max loss 0 / 6.803 / 34%), so do not claim unrestricted UDP
  readiness.
- Real external IPv6 remains `not_tested` until an IPv6-capable network
  proves either tunneled IPv6 or fail-closed behavior with no local IPv6
  leak. The latest disconnect/baseline/reconnect smoke found no external
  IPv6 baseline on the current network, so IPv6 cannot be promoted to
  `pass` or `fail_closed`.
- RC5 stability smoke is `pass_limited`: disconnect/reconnect restored
  `tun0`, no case-sensitive GMvpn crash/ANR markers were found, and the
  local diagnostics bundle was not committed. The diagnostics bundle
  still contained IP/host-like local data, so it must be reviewed before
  sharing and is not public-safe raw evidence.

Unrestricted v1.0.0 requires:

- controlled UDP/iperf: pass;
- full DNS leak audit with at least two independent methods: pass;
- IPv6 routes through the VPN or fails closed on a real IPv6 network:
  pass;
- final signed workflow from the exact release source SHA: pass;
- physical install/connect/disconnect/reconnect smoke: pass.

MVP/internal v1.0.0 can proceed only with explicit limitations accepted:

- UDP/iperf: pass-limited;
- DNS: pass;
- IPv6: not tested;
- release notes disclose those limits;
- rollout starts with internal/limited testing, not broad production.
