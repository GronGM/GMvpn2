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

The physical device import failure was retested after this
categorization branch was installed. The import now reaches the fetch
stage and fails with the safe typed category `FetchFailed`.

The subscription body is not available on the device after the failed
fetch, so Base64 URI list, plain URI list, and SIP008 parser behavior are
not the current proven blocker. They remain covered only by synthetic
unit tests until a body is fetched successfully.

Until a local-only profile or subscription import succeeds, connected
YOURVPNDEAD retest remains blocked.

## Device fetch failure follow-up

Redacted current state:

- Physical device import reaches fetch stage.
- Current safe category: `FetchFailed`.
- Generic device internet was available during the retest, but the app
  did not receive a subscription body.
- Body is not available, so Base64, URI list, and SIP008 parsers are not
  the current blocker.
- No raw subscription URL, host, path, query, body, profile URI, UUID,
  endpoint, token, password, logcat, screenshots, or UI dumps were
  committed.

Next checks:

- Manifest permissions: confirm `INTERNET` and `ACCESS_NETWORK_STATE`.
- Network security policy: confirm no variant-specific config blocks the
  fetch path.
- Cleartext policy: keep HTTPS-only policy unless a separate
  domain-scoped test/dev exception is explicitly approved.
- HTTP status class.
- Redirect handling.
- Timeout likelihood.
- DNS failure likelihood.
- TLS/certificate failure likelihood.
- Server headers and User-Agent compatibility.

PR #28 adds redaction-safe internal fetch diagnostics with only safe
fields such as URL scheme, query/fragment presence, input length bucket,
HTTP status class, redirect/TLS/DNS/timeout likelihood, and body length
bucket. It does not log or expose host, path, query, token, port, raw URL
or response body.

## App-local redacted diagnostics follow-up

Logcat capture was unreliable during the physical fresh import retest:
no safe `GMvpnImport` line was captured after the maintainer triggered
import manually on the device.

PR #28 now also exposes the last import attempt through the existing
app-local redacted diagnostics copy/export report. This is intentionally
limited to safe fields:

- import category;
- URL scheme only;
- query/fragment presence as booleans;
- input length bucket;
- HTTP status class;
- redirect, cleartext, TLS, DNS, and timeout likelihood;
- body length bucket;
- imported profile count when a save succeeds.

The diagnostics report intentionally excludes raw URL, scheme+host URL,
host, path, query, body, endpoint, port, token, UUID, raw exception
message, stacktrace, and profile URI.

Physical retest is still required with the updated APK: the maintainer
must trigger a fresh import on the device and then copy/export the
redacted diagnostics report. PR #28 should remain draft until import
succeeds, the safe diagnostics explain the blocker, or the maintainer
explicitly accepts this as a diagnostics-only step.

## Unknown failure stage tracing follow-up

The first physical app-local diagnostics retest reached the report
surface, but the result was still not actionable:

- category: `Unknown`;
- fetch fields: `unknown`;
- saved profiles: `0`.

PR #28 now preserves redaction-safe stage diagnostics even when the
Throwable does not map to a typed import or fetch exception. The report
can include only controlled buckets:

- import stage;
- failure origin;
- throwable kind;
- whether a typed cause was present;
- whether fetch diagnostics were present;
- safe input-derived URL scheme, query/fragment presence, and input
  length bucket.

Raw URL, scheme+host URL, host, path, query, body, endpoint, port, token,
UUID, raw exception message, stacktrace, and profile URI remain excluded.

Physical retest is still required with the updated APK. If the category
remains `Unknown` but stage/origin/throwable buckets are present, that is
still progress and should guide the next focused fix. PR #28 should stay
draft unless import succeeds, the safe diagnostic becomes actionable, or
the maintainer explicitly accepts the diagnostics-only step.

## UI fallback narrowing follow-up

The next physical diagnostics pass showed that the import URL reached the
app-local diagnostics surface, but the failure still landed in the broad
UI fallback:

- category: `Unknown`;
- stage: `ui_failure_catch`;
- origin: `ui`;
- throwable kind: `unknown_exception`;
- typed cause: `false`;
- fetch diagnostics: `false`;
- saved profiles: `0`.

PR #28 now narrows the typed import boundary so raw failures from
fetch/decode/save are wrapped before they escape to UI:

- fetch failures become `FetchFailed` with stage `fetch_failed` and
  origin `fetch`;
- decode and UniFFI parser failures become `ParseFailed` or
  `UnsupportedFormat` with stage `decode_failed` and origin `decode`;
- no-profile decoder output becomes `NoProfilesFound`;
- save failures become `SaveFailed` with stage `save_failed` and origin
  `save`.

The diagnostics report still exposes only safe buckets: category, stage,
origin, throwable kind, typed-cause presence, fetch-diagnostics presence,
URL scheme, query/fragment booleans, input length bucket, HTTP status
class, cleartext/TLS/DNS/timeout/redirect likelihood, body length bucket,
and imported profile count.

Raw URL, host, path, query, body, endpoint, port, token, UUID, raw
exception message, stacktrace, and profile URI remain excluded.

Physical retest is still required with the updated APK. PR #28 should
remain draft unless import succeeds, the safe diagnostic becomes
actionable, or the maintainer explicitly accepts this as a
diagnostics-only step.

## Decode body-shape follow-up

The latest physical diagnostics narrowed the blocker from broad UI
fallback to the decode boundary:

- category: `ParseFailed`;
- stage: `decode_failed`;
- origin: `decode`;
- typed cause: `true`;
- fetch diagnostics: `true`;
- saved profiles: `0`.

PR #28 now adds redaction-safe body-shape diagnostics for decode-stage
failures. The report can include only aggregate buckets and yes/no
signals:

- whether a body was available;
- body length bucket;
- line count bucket;
- whether the body looks like Base64, URI list, JSON, SIP008, or HTML;
- whether Base64 decoding is likely to work;
- whether supported URI schemes are present as an aggregate;
- requested subscription format;
- controlled decode failure kind.

The report must not include body preview, first line, raw JSON keys,
profile URI, host, path, query, port, token, UUID, IP, raw exception
message, stacktrace, or subscription body.

Physical retest is still required with the updated APK. Useful outcomes:

- `looksHtml=yes` means the provider may be returning HTML, an error
  page, or a challenge page instead of subscription content;
- `bodyLengthBucket=empty` means the fetch returned an empty body;
- `looksBase64=yes` with `base64DecodeLikely=no` points at malformed
  Base64 or a Base64 handling mismatch;
- `looksUriList=yes` with `ParseFailed` points at a URI decoder or
  format mismatch;
- `looksJson=yes` with `looksSip008=no` means the JSON body is not
  SIP008-shaped;
- `containsSupportedUriScheme=yes` with `ParseFailed` points at an FFI
  decoder or profile parser issue;
- a requested format that differs from the selected UI mode would point
  at a UI format mapping bug.

Do not paste real subscription URLs, raw profile URIs, UUIDs, endpoints,
tokens, passwords, scanner output, raw logcat, screenshots, or UI dumps
into issues, PR comments, docs, or chat.
