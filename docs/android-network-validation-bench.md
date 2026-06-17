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

## Controlled UDP / iperf

Use only an approved controlled endpoint. The endpoint can be any trusted
Linux host outside the phone's local network and outside the VPN server
itself. Do not use random public iperf servers as release evidence unless
they are explicitly approved for the release decision.

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
no tunnel disconnect. Browser WebRTC/STUN or video playback can be useful
smoke evidence, but it remains `pass_limited` and does not replace
controlled iperf throughput/loss evidence.

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

## v1.0.0 decision matrix

Unrestricted v1.0.0 requires:

- controlled UDP/iperf: pass;
- full DNS leak audit with at least two independent methods: pass;
- IPv6 routes through the VPN or fails closed on a real IPv6 network:
  pass;
- final signed workflow from the exact release source SHA: pass;
- physical install/connect/disconnect/reconnect smoke: pass.

MVP/internal v1.0.0 can proceed only with explicit limitations accepted:

- UDP/iperf: blocked or not tested;
- DNS: pass-limited;
- IPv6: not tested;
- release notes disclose those limits;
- rollout starts with internal/limited testing, not broad production.
