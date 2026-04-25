# ADR 0004: Pin Xray-core and tunnel data path

- Status: accepted
- Date: 2026-04-24
- Builds on: ADR 0002 (Android first + gomobile), ADR 0001 (Rust shared core)

## Context

ADR 0002 chose Xray-core as the engine and `gomobile bind` as the
Android consumption path. The wrapper in `/core` was published with a
deliberate `ErrNotImplemented` so the rest of the project could integrate
the FFI surface without waiting on the engine. With the parsers,
subscription decoder, and FFI now in place, the missing piece is the
engine itself: which version, which API entry point, and how packets
flow from the Android `VpnService` TUN fd through to outbound traffic.

## Decision

### 1. Engine version

Pin **`github.com/xtls/xray-core@v1.260327.0`** in `core/go.mod`.

Upstream switched from semver (`v1.8.x`) to calendar tags (`v1.YYMMDD.N`)
in early 2025; this is the most recent stable tag at the time of the
decision. The pin is exact; only `core/VERSIONS.md` and a coordinated
PR may change it.

### 2. Embedding API

Use the high-level Xray-core embedding API:

- `core.LoadConfig("json", reader)` to parse the JSON profile produced
  by `gmvpn-core` (Rust).
- `core.New(cfg)` to build an `*core.Instance`.
- `instance.Start()` to bring features up.
- `instance.Close()` to tear down.

`_ "github.com/xtls/xray-core/main/distro/all"` is imported once in
`core/gmvpn/tunnel.go` to register every protocol module — without it
`LoadConfig` only accepts a trivial subset.

### 3. Tunnel data path on Android

The Android `VpnService` produces a TUN file descriptor that delivers
raw IP packets. Xray-core does not consume IP packets directly; it
expects connections at one of its inbound protocols. The bridge between
the two is a **tun2socks layer**.

The plan is a two-stage data path:

```
 ┌──────────────┐  IP pkts   ┌──────────────┐  SOCKS5  ┌──────────────┐
 │ Android TUN  │──────────▶│ tun2socks    │─────────▶│ Xray-core    │
 │ fd (kernel)  │           │ (gVisor /    │          │ socks inbound │
 │              │           │  hev / go)   │          │              │
 └──────────────┘           └──────────────┘          └──────────────┘
```

The Xray config built by the Android client therefore always contains:

- A `socks` inbound bound to a localhost port (loopback only, picked at
  runtime).
- The user's profile as the `outbound`.
- Routing that sends everything out through that outbound by default.

The TUN fd is handed to a tun2socks process inside the same Android
service; Xray-core itself never sees the fd. This keeps Xray-core
unmodified (ADR 0002 non-negotiable) and contains the kernel-pkts
boundary in one component we can swap.

Concrete tun2socks options, in order of current preference:

1. **`hev-socks5-tunnel`** (C, very small, used by sing-box/v2rayNG).
   Ship as a prebuilt `.so` per ABI, drive from Kotlin via JNI.
2. **gVisor netstack + Go SOCKS5 client**, embedded into the same
   gomobile-bound module. Single binary, larger size.
3. **`badvpn-tun2socks`**. Mature but ageing; only if (1) and (2) fail.

The pick is deferred — see open question §1 below — but the API shape of
this wrapper is committed: `Start(configJSON, tunFD)` accepts a TUN fd
today and the bridge will plug in without touching the FFI surface.

## Rationale

- The high-level `core.LoadConfig`/`core.New` API is the same one used by
  the Xray CLI. It is the most stable embedding surface upstream offers
  and has not had breaking changes inside the v1.x line.
- Calling Xray-core only with a JSON config keeps `gmvpn-core` (Rust)
  the single source of profile-to-config truth. We do not duplicate the
  config schema in Go.
- Putting tun2socks outside Xray-core means we can later swap engines
  (sing-box, custom) without changing the Android tunnel service or
  the FFI surface.

## Consequences

- `core/go.mod` now requires Go **1.26**; CI and developer machines must
  match. `core.yml` is updated accordingly.
- `core/gmvpn` produces a real `*core.Instance`. Tests cover lifecycle
  (start → connected → idle, double-start, malformed config, stats while
  running) using a one-line freedom outbound config that needs no
  network.
- Building `gmvpn.aar` via `gomobile bind` now pulls Xray-core's full
  dependency graph (gVisor, quic-go, sing, utls, websocket, …). Expect
  a `.aar` in the 8–15 MB range per ABI before compression.
- Wrapper version bumped to `0.0.2`. `XrayVersion()` is exposed for
  diagnostics.

## Open questions

1. **tun2socks pick.** Tracked in `docs/memory/pending-decisions.md`
   §8 (added by this ADR).
2. **DNS.** Leaning: route DNS through the SOCKS inbound + Xray's DNS
   outbound, never through the OS resolver while connected. Confirm
   when bridge lands.
3. **IPv6.** Leaning: tunnel IPv6 if the profile supports it; block
   otherwise. Same caveat — confirm with first live tunnel.

## Alternatives considered

- **Calling Xray internals directly to inject a TUN fd.** Would require
  patching upstream. Rejected per ADR 0002 non-negotiable #2.
- **Using `libXray` from the v2rayNG fork.** Useful prior art but adds a
  third party we don't fully control. Worth revisiting if the picked
  tun2socks proves too heavy.
- **Skipping tun2socks and using a SOCKS-only proxy app (no TUN).**
  Cripples the product (per-app routing, kill switch). Rejected.
