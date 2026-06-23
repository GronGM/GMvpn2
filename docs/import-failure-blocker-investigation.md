# Import Failure Blocker Investigation

## Context

PR #27 local-proxy listener lifecycle validation is blocked because the
debug build cannot complete a local profile or subscription import on
the physical device. Without an imported profile, connected-state
YOURVPNDEAD coverage cannot proceed honestly.

Observed physical state, redacted:

- PR #27 debug APK installed as `com.gmvpn.client.debug`.
- YOURVPNDEAD package is present.
- Subscription import retry was attempted with the existing masked input,
  clipboard paste, URI list, Base64 URI list, and SIP008 selections.
- Each attempt ended in a generic import failure.
- Profiles screen showed `0` saved profiles after retry.
- UI dumps did not expose raw URI, URL, UUID, endpoint, token, password,
  or IP-like values.

Raw subscription URLs, profile URIs, scanner output, logcat, screenshots,
and UI dumps are intentionally not committed.

## Source Audit Map

| Area | Source | Notes |
| --- | --- | --- |
| Import UI | `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/HomeScreen.kt` | `ImportTab` owns masked subscription input, format picker, fetch button, confirmation dialog, and redacted import message rendering. |
| Import orchestration | `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/MainActivity.kt` | `onFetchSubscription` starts fetch/decode; `onConfirmImport` commits the confirmed preview to `ProfileStore`. |
| Network fetch | `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/SubscriptionFetcher.kt` | HTTPS-only one-shot fetch, no redirects to non-HTTPS, body-size bound. |
| Parser selection | `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/HomeScreen.kt` | The selected `FfiSubscriptionFormat` is passed through from the format picker. |
| Native decode | `clients/android/app/src/main/kotlin/com/gmvpn/client/ui/MainActivity.kt` | Runtime path calls `decodeSubscriptionUris(body, format)` from UniFFI. |
| Default/Base64 parser | native `decodeSubscriptionUris` | Covered by synthetic JVM tests through injected decoder shape, not real secrets. |
| URI list parser | native `decodeSubscriptionUris` | Covered by synthetic JVM tests through injected decoder shape, not real secrets. |
| SIP008 parser | native `decodeSubscriptionUris` | Failure is categorized as unsupported/invalid format when decoder reports format-like failure. |
| Preview building | `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/ProfileEntry.kt` | `buildProfileImportPlan` deduplicates URIs and builds safe preview names. |
| Repository save | `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/ProfileStore.kt` | `replaceAllEntries` encrypts and replaces the library after explicit confirmation. |
| Error mapping | `clients/android/app/src/main/kotlin/com/gmvpn/client/profile/SubscriptionImportPipeline.kt` | Typed internal categories are now produced before UI string mapping. |

No debug-vs-release import path split was found in the Kotlin import
pipeline. Debug and release use the same UI, fetcher, parser call shape,
and profile save path.

## Internal Failure Categories

The import path now uses typed internal categories:

- `EmptyInput`
- `FetchFailed`
- `UnsupportedFormat`
- `ParseFailed`
- `NoProfilesFound`
- `SaveFailed`
- `Unknown`

The UI still maps these categories to existing safe localized strings.
No new user-facing strings were added in this investigation branch.

## Synthetic Test Coverage

Added JVM unit coverage uses only synthetic fixtures:

- `example.invalid` host names;
- a fake all-zero UUID;
- fake profile names;
- fake ports required by URI shape;
- no real subscription URL, raw profile, endpoint, token, password, or
  private key.

Covered cases:

- URI list import produces a preview.
- Base64 URI list import produces a preview.
- SIP008-style synthetic failure is typed and non-generic internally.
- Empty input returns `EmptyInput`.
- Invalid parser input returns `ParseFailed` without raw input in the
  visible exception message.
- Network/fetch failure returns `FetchFailed`.
- Empty decoder output returns `NoProfilesFound`.
- Save failure is distinguishable internally as `SaveFailed`.

## Remaining Blocker

The physical device import failure still needs a real-device retest after
this categorization branch is installed. Until a local-only profile or
subscription import succeeds, connected YOURVPNDEAD retest remains
blocked.

Do not paste real subscription URLs, raw profile URIs, UUIDs, endpoints,
tokens, passwords, scanner output, raw logcat, screenshots, or UI dumps
into issues, PR comments, docs, or chat.
