// Package gmvpn is the gomobile-friendly wrapper around the engine.
//
// The API is intentionally narrow so it binds cleanly via `gomobile bind`:
// only primitive types, strings, and named interfaces cross the boundary.
// Xray-core integration is not yet wired in — Start returns ErrNotImplemented
// until the engine hook lands. The surface below is stable; the
// implementation is what changes.
package gmvpn
