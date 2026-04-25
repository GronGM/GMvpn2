# Pinned versions

The `/core` wrapper pins the engine version explicitly so builds are
reproducible and a bump is an obvious PR.

| Component             | Version        | Notes                                     |
|-----------------------|----------------|-------------------------------------------|
| Xray-core             | `v1.260327.0`  | Pinned 2026-04-24. See ADR 0004.          |
| Go toolchain          | `1.26`         | Required by Xray-core `v1.26*`.           |
| gomobile              | latest         | `go install golang.org/x/mobile/cmd/gomobile@latest`. |
| Android NDK           | r26+           | Required by `gomobile bind -target=android`.|

## Update procedure

1. Bump the version in `go.mod` (`go get github.com/xtls/xray-core@<tag>`).
2. Update this file with the new tag and date.
3. `go mod tidy`, `make test`, fix any surface breakage.
4. Regenerate `core/build/gmvpn.aar` via `make android`.
5. Smoke-test the Android app against a known-good profile; record the
   commit hash of the successful run in the PR description.
6. Update `docs/memory/platform-notes.md` if upstream behavior changes
   (e.g. new transport, changed default DNS behavior).

## Why the date-based Xray-core versions

Upstream switched from semver (`v1.8.x`) to calendar tags
(`v1.YYMMDD.N`) in early 2025. The pin is still exact; only the format
changed.
