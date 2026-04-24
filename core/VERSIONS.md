# Pinned versions

The `/core` wrapper pins the engine version explicitly so builds are
reproducible and a bump is an obvious PR.

| Component             | Version  | Notes                                     |
|-----------------------|----------|-------------------------------------------|
| Xray-core             | TBD      | To be pinned when the first `.aar` is cut.|
| gomobile              | latest   | Installed via `go install golang.org/x/mobile/cmd/gomobile@latest`. |
| Android NDK           | r26+     | Required by `gomobile bind -target=android`.|
| Go                    | 1.22+    | Matches `go.mod`.                         |

## Update procedure

1. Bump the version in `go.mod` / `VERSIONS.md`.
2. Regenerate `core/build/gmvpn.aar` via `make android`.
3. Run Android app against a known-good profile; record the commit.
4. Update `docs/memory/platform-notes.md` if behavior changes.
