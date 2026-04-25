# /core — Xray-core wrapper

Go module that wraps Xray-core behind a gomobile-friendly API so Android
(and later iOS) can call it with no JNI boilerplate of our own.

Decisions recorded in:

- `docs/adr/0002-android-first-gomobile.md` — Android first via gomobile.
- `docs/adr/0004-xray-core-pin.md` — pinned Xray version + tun2socks plan.

Pinned versions: `VERSIONS.md`.

## Layout

```
go.mod
gmvpn/           Go package exposed via gomobile bind
  doc.go         package docs
  tunnel.go      Tunnel / StatusListener / TrafficStats API
  tunnel_test.go lifecycle tests (real Xray-core, no network)
tun2socks/       TUN ↔ SOCKS5 bridge
  bridge.go      Bridge interface + validating stub
  bridge_test.go validation tests
Makefile         build + test targets
VERSIONS.md      pinned Xray-core / Go / gomobile / NDK versions
```

## API surface (stable)

`gmvpn` package:
- `Tunnel` interface: `Start(configJSON, tunFD, mtu, socksPort) / Stop() / Stats()`.
- `StatusListener` interface: `OnStatusChanged(status, detail)`.
- `TrafficStats` struct: uplink / downlink byte counters.
- `Version() string` — wrapper version.
- `XrayVersion() string` — pinned engine version.
- String constants: `StatusIdle`, `StatusStarting`, `StatusConnected`,
  `StatusReconnecting`, `StatusStopping`, `StatusError`.
- Sentinel errors: `ErrAlreadyRunning`, `ErrNotRunning`.

`tun2socks` package:
- `Bridge` interface: `Start(tunFD, mtu, socks5Addr) / Stop() / IsRunning()`.
- `New() Bridge` returns the default implementation.
- Sentinel errors: `ErrInvalidTunFD`, `ErrInvalidMTU`, `ErrEmptySocks5Addr`,
  `ErrAlreadyRunning`, `ErrNotImplemented` (current stub).

Types are deliberately restricted to primitives + named interfaces so
they survive the `gomobile bind` filter.

## Status

The wrapper embeds Xray-core for real:

- `Start` parses the JSON config via `core.LoadConfig("json", …)`,
  builds an `*core.Instance`, calls `Start()`, and stores it.
- `Stop` shuts the bridge down first, then calls `instance.Close()`.
- All registered protocols (VLESS / VMess / Trojan / Shadowsocks /
  Reality / WS / gRPC / etc.) are available because
  `_ "github.com/xtls/xray-core/main/distro/all"` is imported.

The tun2socks `Bridge` interface, lifecycle, and integration with
`gmvpn.Tunnel` are in place. The actual netstack engine — the bit
that reads IP packets off the TUN fd and turns them into SOCKS5
connections — is **a stub**: it validates inputs and returns
`ErrNotImplemented`. The blocker (gVisor version conflict between
Xray-core and `xjasonlyu/tun2socks/v2`) is documented in
`docs/adr/0004-xray-core-pin.md` §3 and
`docs/memory/pending-decisions.md` §8. When the engine lands, only
`tun2socks/bridge.go` changes; the public API and the `gmvpn`
integration are stable.

## Building

Local Go checks:

```sh
cd core
make test    # go test ./... (covers Start/Stop with a freedom outbound)
make vet
```

Android `.aar` (requires Android NDK r26+ and gomobile):

```sh
cd core
make gomobile-install   # one-time
make android            # → build/gmvpn.aar
```

## Non-goals

- Domain logic (profile models, routing, subscriptions) lives in
  `shared/gmvpn-core` — do not duplicate here.
- No Xray-core patches. If upstream behavior is wrong, fix it upstream
  or wrap it externally.
