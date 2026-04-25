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
Makefile         build + test targets
VERSIONS.md      pinned Xray-core / Go / gomobile / NDK versions
```

## API surface (stable)

- `Tunnel` interface: `Start(configJSON, tunFD) / Stop() / Stats()`.
- `StatusListener` interface: `OnStatusChanged(status, detail)`.
- `TrafficStats` struct: uplink / downlink byte counters.
- `Version() string` — wrapper version.
- `XrayVersion() string` — pinned engine version.
- String constants: `StatusIdle`, `StatusStarting`, `StatusConnected`,
  `StatusReconnecting`, `StatusStopping`, `StatusError`.
- Sentinel errors: `ErrAlreadyRunning`, `ErrNotRunning`.

Types are deliberately restricted to primitives + named interfaces so
they survive the `gomobile bind` filter.

## Status

The wrapper now embeds Xray-core for real:

- `Start` parses the JSON config via `core.LoadConfig("json", …)`,
  builds an `*core.Instance`, calls `Start()`, and stores it.
- `Stop` calls `instance.Close()` and resets state.
- All registered protocols (VLESS / VMess / Trojan / Shadowsocks /
  Reality / WS / gRPC / etc.) are available because
  `_ "github.com/xtls/xray-core/main/distro/all"` is imported.

What is **not** yet wired: the TUN ↔ Xray bridge. The TUN fd is
accepted but currently held without effect. ADR 0004 §3 describes the
tun2socks layer that closes that gap; the FFI surface stays unchanged
when it lands.

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
