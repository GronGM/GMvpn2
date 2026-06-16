# Android release signing

Status: Android v1 release candidate signed artifacts produced; tag
approval pending. This is not a production/public distribution
approval. Creating a tag, publishing a GitHub Release, uploading to a
store, or distributing a signed build requires a separate explicit
approval.

## Release candidate vs public release

A release candidate proves that the package can be built, validated,
signed, and handed off for review. It can still carry known limitations
and must not be described as ready for production use.

A production/public release additionally needs an approved tag, signed
artifacts from a trusted workflow run, release notes, distribution
approval, and any store-specific review steps.

## Required GitHub secrets

The manual `.github/workflows/android-release.yml` workflow requires
these repository secrets before it can produce signed RC artifacts:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

The workflow decodes `RELEASE_KEYSTORE_BASE64` only under
`runner.temp`, exports the decoded path as `RELEASE_KEYSTORE_PATH`, and
passes the remaining values to Gradle through environment variables.
It does not write secrets to `gradle.properties` and does not print
secret values.

For `android-v1.0.0-rc.1`, the required secret names were present and
manual workflow run `27632339860` produced signed RC artifacts. Secret
values were not printed or copied into the repository.

## Create an upload/release keystore

Create the keystore outside the git checkout. Example PowerShell path:

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\.gmvpn" | Out-Null
keytool -genkeypair `
  -v `
  -storetype PKCS12 `
  -keystore "$env:USERPROFILE\.gmvpn\gmvpn-android-upload.keystore" `
  -alias "gmvpn-upload" `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Use a long random store password and key password. Store the keystore
and passwords in the team's password manager or approved secret store,
not in this repository.

## Encode the keystore for GitHub Actions

PowerShell:

```powershell
$keystore = "$env:USERPROFILE\.gmvpn\gmvpn-android-upload.keystore"
$encoded = "$env:USERPROFILE\.gmvpn\gmvpn-android-upload.keystore.b64"
[Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore)) |
  Set-Content -NoNewline $encoded
```

Git Bash:

```sh
base64 -w 0 ~/.gmvpn/gmvpn-android-upload.keystore \
  > ~/.gmvpn/gmvpn-android-upload.keystore.b64
```

Do not commit the keystore or the `.b64` file.

## Add repository secrets

GitHub UI path:

1. Open `GronGM/GMvpn2` on GitHub.
2. Go to Settings -> Secrets and variables -> Actions.
3. Add each required repository secret.

GitHub CLI path:

```powershell
gh auth status
gh secret set RELEASE_KEYSTORE_BASE64 --repo GronGM/GMvpn2 < "$env:USERPROFILE\.gmvpn\gmvpn-android-upload.keystore.b64"
gh secret set RELEASE_KEYSTORE_PASSWORD --repo GronGM/GMvpn2
gh secret set RELEASE_KEY_ALIAS --repo GronGM/GMvpn2
gh secret set RELEASE_KEY_PASSWORD --repo GronGM/GMvpn2
gh secret list --repo GronGM/GMvpn2
```

When `gh secret set` is run without `--body`, paste the value at the
interactive prompt. Do not put secret values in shell history, docs,
issue comments, commits, or logs.

## Local unsigned and signed builds

Without signing environment variables, local release builds remain
unsigned by design:

```powershell
cd C:\Users\Gron\Documents\gmvpn2\clients\android
.\gradlew.bat :app:assembleRelease :app:bundleRelease --stacktrace
```

For a local signed build, point Gradle at an existing keystore outside
the repository:

```powershell
$env:RELEASE_KEYSTORE_PATH = "$env:USERPROFILE\.gmvpn\gmvpn-android-upload.keystore"
$env:RELEASE_KEYSTORE_PASSWORD = "<store-password>"
$env:RELEASE_KEY_ALIAS = "gmvpn-upload"
$env:RELEASE_KEY_PASSWORD = "<key-password>"
.\gradlew.bat :app:assembleRelease :app:bundleRelease --stacktrace
```

Do not commit local signed artifacts.

## Run the manual workflow

The proposed RC label is `android-v1.0.0-rc.1`; the Android
`versionName` is `1.0.0-rc.1`.

```powershell
gh workflow run android-release.yml `
  --repo GronGM/GMvpn2 `
  -f rc_tag=android-v1.0.0-rc.1 `
  -f version_name=1.0.0-rc.1
```

Expected behavior:

- The workflow runs only from `workflow_dispatch`.
- It does not create a git tag.
- It does not publish a GitHub Release.
- It builds native artifacts through `scripts/build-android-libs.sh`.
- It runs Android unit tests, lint, release APK build, and release
  bundle build.
- It uploads unsigned audit artifacts as GitHub Actions artifacts.
- If any signing secret is missing, the signed stage fails with a
  clear error and no signed RC artifact is produced.
- If all signing secrets are present, it uploads signed APK/AAB
  artifacts and checksums as GitHub Actions artifacts.

## Verified RC artifact run

Run:
`https://github.com/GronGM/GMvpn2/actions/runs/27632339860`

Inputs:

- `rc_tag`: `android-v1.0.0-rc.1`
- `version_name`: `1.0.0-rc.1`
- branch: `claude/relaxed-euler-1Vr2R`
- head SHA: `1775829107eac1066af911353fc17f8d11f24a18`

Produced artifacts:

- `gmvpn-android-android-v1.0.0-rc.1-signed`
- `gmvpn-android-android-v1.0.0-rc.1-unsigned-audit`

Downloaded local verification path:
`.local/release-artifacts/android-v1.0.0-rc.1/`

Local verification results:

- `apksigner verify --verbose --print-certs` passed for the signed APK.
- APK Signature Scheme v2 verified with one signer.
- `signed-rc.sha256` matched the signed APK and signed AAB.
- `unsigned-audit.sha256` matched all five unsigned audit files.
- No git tag or GitHub Release was created.

## What must never be committed

- keystores: `.jks`, `.keystore`, `.p12`, `.pfx`
- keystore base64 files
- signing passwords or aliases tied to a private key
- release APK/AAB artifacts
- raw diagnostics, raw logs, screenshots, profiles, or server names

## Key rotation

If a signing key or password is exposed:

1. Stop distributing builds signed with the exposed key.
2. Remove or overwrite the affected GitHub secrets.
3. Generate a replacement upload/release key outside the repository.
4. Add new GitHub secrets.
5. Run the manual workflow and verify the signed artifact.
6. Record the incident and rotation in release notes or the security
   tracker without publishing the compromised secret values.

Production/public publication still requires separate explicit
approval after a successful signed workflow run.
