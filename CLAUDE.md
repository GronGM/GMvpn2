# GMvpn2 — engineering context for Claude

This file is the source of truth for any future Claude session working in this
repo. Keep it short; use `docs/` and `docs/memory/` for depth.

## Product

Modern cross-platform VPN client built on top of **Xray-core**
(VLESS / VMess / Trojan / Shadowsocks, XTLS, Reality). Goal: above-commodity
quality — clean architecture, native platform integration, safe networking,
a real product, not a thin wrapper.

## Stack (decided)

- **Engine:** Xray-core (Go). Used as an embedded library / subprocess per
  platform. Not forked — wrapped.
- **Shared layer:** **Rust** (Variant A). One domain core, compiled to static
  or dynamic libraries per platform, exposed via FFI (UniFFI for
  Kotlin/Swift, C-ABI for Windows/Linux).
- **UIs:** native per platform.
  - Android: Kotlin + `VpnService`.
  - iOS: Swift + `NEPacketTunnelProvider`.
  - macOS: SwiftUI + `NEPacketTunnelProvider` / system extension.
  - Windows: WinUI 3 (or Qt) + WinTUN + background service.
  - Linux: Qt / GTK tray + systemd unit + TUN device.
- **Config source of truth:** JSON schemas under `/schemas`. All clients and
  the shared core consume the same models.

## Repo map

```
/core         Xray-core wrapper and per-platform builds (Go)
/shared       Rust workspace: domain core + FFI surface
  gmvpn-core  Profiles, subscriptions, URI parsers, routing, logging
  gmvpn-ffi   FFI bindings (UniFFI + C-ABI)
/clients
  /android    Kotlin app + VpnService
  /ios        Swift app + NEPacketTunnelProvider
  /macos      SwiftUI app + NetworkExtension
  /windows    WinUI/Qt app + WinTUN service
  /linux      Qt/GTK tray + systemd service
/schemas      JSON Schemas (profile, subscription, routing)
/docs         Architecture, ADRs, platform notes
/docs/memory  Persistent working context for future sessions
/.github      CI, templates
```

## Non-negotiables

1. **Security over speed.** No feature ships if it can leak traffic, keys, or
   DNS. Kill switch must default to on for anything marketed as "always on".
2. **No invented platform APIs.** If a capability needs a real entitlement,
   system extension, or kernel module, say so. Do not mock it as available.
3. **Shared logic belongs in Rust.** Duplicated parsers, routing rules, and
   subscription handling are a bug.
4. **Native UI per platform.** No cross-platform UI framework that breaks
   system integration for tunnels (VpnService, NEPacketTunnelProvider,
   WinTUN). Shared UI is only a long-term optional experiment.
5. **Secrets never in git.** Keystores, provisioning profiles, signing keys,
   subscription tokens — all outside the repo. `.gitignore` enforces this.

## Working agreements

- Default branch for ongoing agent work: `claude/relaxed-euler-1Vr2R`.
- Commits: imperative mood, scoped prefix where useful
  (`shared: …`, `android: …`, `docs: …`).
- Prefer small, reviewable commits over monster PRs.
- Every architectural choice that is hard to reverse goes into
  `docs/adr/NNNN-*.md` as a short ADR.
- Open questions live in `docs/memory/pending-decisions.md` until resolved.

## What to do first in a fresh session

1. Read this file.
2. Read `docs/memory/project-context.md` and `docs/memory/pending-decisions.md`.
3. Read `docs/release-roadmap.md` — the prioritised list of work to v1.
4. Check `git status` and recent commits before editing.
5. Only then start the task.
