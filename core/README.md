# /core — Xray-core wrapper

Go module that wraps Xray-core behind a gomobile-friendly API so Android
(and later iOS) can call it with no JNI boilerplate of our own.

Decision recorded in `docs/adr/0002-android-first-gomobile.md`.
Pinned versions: `VERSIONS.md`.

## Layout

```
go.mod
gmvpn/           Go package exposed via gomobile bind
  doc.go         package docs
  tunnel.go      Tunnel / StatusListener / TrafficStats API
  tunnel_test.go unit tests (no engine required)
Makefile         build + test targets
VERSIONS.md      pinned Xray-core / gomobile / NDK versions
```

## API surface (stable)

- `Tunnel` interface: `Start(configJSON, tunFD) / Stop() / Stats()`.
- `StatusListener` interface: `OnStatusChanged(status, detail)`.
- `TrafficStats` struct: uplink / downlink byte counters.
- `Version() string`.
- String constants: `StatusIdle`, `StatusStarting`, `StatusConnected`,
  `StatusReconnecting`, `StatusStopping`, `StatusError`.
- Sentinel errors: `ErrNotImplemented`, `ErrAlreadyRunning`, `ErrNotRunning`.

Types are deliberately restricted to primitives + named interfaces so
they survive the `gomobile bind` filter.

## Status

The public API is in place and tested. The engine call itself is
**not yet wired** — `Start` deliberately returns `ErrNotImplemented`
so a platform client can integrate the wrapper before Xray-core is
pulled in, and later swap in the real engine without an API change.

## Building

Local Go checks:

```sh
cd core
make test    # go test ./...
make vet
```

Android `.aar` (requires Android NDK + gomobile toolchain):

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
