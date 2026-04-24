# Repo defaults

## Repository

- GitHub: `GronGM/GMvpn2`
- Default working branch for agent sessions: `claude/relaxed-euler-1Vr2R`
- Do not push to `main` from an agent session without explicit user approval.

## Branch strategy (initial)

- `main` — stable, release-ready.
- `claude/<topic>-<id>` — agent development branches.
- Feature branches for humans: `feat/<area>-<short-desc>`,
  `fix/<area>-<short-desc>`.
- One topic per branch. Squash-merge into `main` by default.

## Commit style

- Imperative mood: "add subscription parser", not "added" / "adds".
- Scope prefix when it helps: `shared:`, `android:`, `ios:`, `core:`,
  `schemas:`, `docs:`, `ci:`.
- Body explains *why*, not *what* — the diff already shows *what*.

## Pull requests

- Title under 70 characters.
- Body: summary (1–3 bullets) + test plan.
- Link ADRs when the PR changes an architectural decision.
- Do not open PRs unless the user asks. Agent sessions commit and push to
  the working branch; opening the PR is the user's call.

## Code style

- Rust: `rustfmt` default, `clippy` with `-D warnings` in CI.
- Kotlin: ktlint / detekt (to be added when Android module starts).
- Swift: swiftformat / swiftlint (to be added when iOS/macOS modules start).
- Go: `gofmt`, `go vet`.
- Indentation: per `.editorconfig`.

## Secrets

- Never commit: keystores, provisioning profiles, signing keys, API tokens,
  subscription URLs with credentials, `.env*` files.
- Development secrets live outside the repo, referenced by env vars or local
  untracked config files listed in `.gitignore`.
