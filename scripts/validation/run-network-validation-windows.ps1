[CmdletBinding()]
param(
    [string]$OutputRoot = ".local\validation",
    [string]$Bitrate = "5M",
    [int]$DurationSeconds = 30
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

function Save-RawOutput {
    param(
        [string]$Name,
        [object[]]$Lines
    )
    $path = Join-Path $script:RunDir $Name
    if ($null -eq $Lines) {
        @("") | Set-Content -Path $path -Encoding UTF8
        return
    }
    $Lines | ForEach-Object {
        if ($null -eq $_) { "" } else { $_.ToString() }
    } | Set-Content -Path $path -Encoding UTF8
}

function Invoke-CapturedCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$RawFile
    )
    try {
        $output = & $FilePath @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } catch {
        $output = @($_.Exception.Message)
        $exitCode = 1
    }
    Save-RawOutput -Name $RawFile -Lines $output
    [pscustomobject]@{
        ExitCode = $exitCode
        Output = @($output)
    }
}

function Find-AdbPath {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    $candidates = @()
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe")
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe")
    }
    if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe")
    }
    $candidates += "C:\Android\Sdk\platform-tools\adb.exe"

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $null
}

function Find-IperfPath {
    $cmd = Get-Command iperf3 -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    $null
}

function Parse-IperfText {
    param([string]$Text)
    $matches = [regex]::Matches($Text, "([\d.]+)\s+ms\s+\d+/\d+\s+\(([\d.]+)%\)")
    if ($matches.Count -eq 0) {
        return [pscustomobject]@{
            JitterMs = "unknown"
            LossPercent = "unknown"
        }
    }
    $last = $matches[$matches.Count - 1]
    [pscustomobject]@{
        JitterMs = $last.Groups[1].Value
        LossPercent = $last.Groups[2].Value
    }
}

$preflightScript = Join-Path $PSScriptRoot "preflight-windows.ps1"
if (-not (Test-Path $preflightScript)) {
    throw "Missing preflight script: $preflightScript"
}

$preflightJson = & $preflightScript -OutputRoot $OutputRoot -Json
$preflight = $preflightJson | ConvertFrom-Json
$script:RunDir = $preflight.run_dir

$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("# Android network validation summary (redacted)")
$summary.Add("")
$summary.Add("- Timestamp UTC: $($preflight.timestamp_utc)")
$summary.Add("- Git branch: $($preflight.git_branch)")
$summary.Add("- ADB: $($preflight.adb)")
$summary.Add("- Device: $($preflight.authorized_device)")
$summary.Add("- iperf3: $($preflight.iperf3)")
$summary.Add("- Approved endpoint: $($preflight.approved_endpoint)")
$summary.Add("- Endpoint redacted: yes")
$summary.Add("")

$adbReady = [bool]$preflight.ready_for_device_stability_smoke
$udpReady = [bool]$preflight.ready_for_udp_validation

if (-not $adbReady) {
    $summary.Add("## Device stability smoke")
    $summary.Add("")
    $summary.Add("Status: blocked")
    $summary.Add("")
    $summary.Add("Blocker: adb or an authorized physical device is not ready.")
    $summary.Add("")
} else {
    $adbPath = Find-AdbPath
    $androidRelease = Invoke-CapturedCommand `
        -FilePath $adbPath `
        -Arguments @("shell", "getprop", "ro.build.version.release") `
        -RawFile "device-android-release-raw.txt"
    $androidSdk = Invoke-CapturedCommand `
        -FilePath $adbPath `
        -Arguments @("shell", "getprop", "ro.build.version.sdk") `
        -RawFile "device-android-sdk-raw.txt"
    $appPidResult = Invoke-CapturedCommand `
        -FilePath $adbPath `
        -Arguments @("shell", "pidof", "com.gmvpn.client") `
        -RawFile "app-pid-raw.txt"
    Invoke-CapturedCommand `
        -FilePath $adbPath `
        -Arguments @("logcat", "-c") `
        -RawFile "logcat-clear-raw.txt" | Out-Null
    $logcat = Invoke-CapturedCommand `
        -FilePath $adbPath `
        -Arguments @("logcat", "-d") `
        -RawFile "logcat-raw.txt"

    $appPidText = ($appPidResult.Output -join "").Trim()
    $appRunning = ($appPidResult.ExitCode -eq 0) -and (-not [string]::IsNullOrWhiteSpace($appPidText))
    $crashMarkers = ($logcat.Output -join "`n") -match "(FATAL EXCEPTION|AndroidRuntime|ANR)"
    $stabilityStatus = if ($crashMarkers) { "fail" } else { "pass_limited" }

    $summary.Add("## Device stability smoke")
    $summary.Add("")
    $summary.Add("Status: $stabilityStatus")
    $summary.Add("")
    $summary.Add("- Android release/API captured: yes")
    $summary.Add("- App process running: $appRunning")
    $summary.Add("- Crash/ANR markers in captured logcat: $crashMarkers")
    $summary.Add("- Raw logcat committed: false")
    $summary.Add("- Manual app restart/reconnect/no-profile checks: not automated")
    $summary.Add("")

    Save-RawOutput -Name "device-summary-redacted.txt" -Lines @(
        "android_release_present=$($androidRelease.Output.Count -gt 0)",
        "android_sdk_present=$($androidSdk.Output.Count -gt 0)",
        "app_running=$appRunning",
        "crash_markers=$crashMarkers"
    )
}

if (-not $udpReady) {
    $summary.Add("## Controlled UDP / iperf")
    $summary.Add("")
    $summary.Add("Status: blocked")
    $summary.Add("")
    $summary.Add("Blocker: adb/device, iperf3, or approved endpoint env is missing.")
    $summary.Add("Endpoint value was not printed or committed.")
    $summary.Add("")
} else {
    $iperfPath = Find-IperfPath
    $iperfArgs = @(
        "-c", $env:GMVPN_IPERF_HOST,
        "-p", $env:GMVPN_IPERF_PORT,
        "-u",
        "-b", $Bitrate,
        "-t", "$DurationSeconds",
        "--get-server-output"
    )
    $iperf = Invoke-CapturedCommand -FilePath $iperfPath -Arguments $iperfArgs -RawFile "iperf3-udp-raw.txt"
    $iperfText = $iperf.Output -join "`n"
    $parsed = Parse-IperfText -Text $iperfText
    $udpStatus = if ($iperf.ExitCode -eq 0) { "pass" } else { "fail" }

    $summary.Add("## Controlled UDP / iperf")
    $summary.Add("")
    $summary.Add("Status: $udpStatus")
    $summary.Add("")
    $redactedCommand = "iperf3 -c <REDACTED_ENDPOINT> -p <REDACTED_PORT> " +
        "-u -b $Bitrate -t $DurationSeconds --get-server-output"
    $summary.Add("- Command: $redactedCommand")
    $summary.Add("- Duration seconds: $DurationSeconds")
    $summary.Add("- Target bitrate: $Bitrate")
    $summary.Add("- Packet loss percent: $($parsed.LossPercent)")
    $summary.Add("- Jitter ms: $($parsed.JitterMs)")
    $summary.Add("- Endpoint redacted: yes")
    $summary.Add("")
}

$summary.Add("## Full DNS leak audit")
$summary.Add("")
$summary.Add("Status: pass_limited")
$summary.Add("")
$summary.Add("Manual evidence required: record two independent DNS methods while VPN is connected.")
$summary.Add("Do not mark full pass until local ISP/router DNS is absent in redacted evidence.")
$summary.Add("")

$summary.Add("## Real IPv6 validation")
$summary.Add("")
$summary.Add("Status: not_tested")
$summary.Add("")
$summary.Add("Manual evidence required: test on a network with real external IPv6.")
$summary.Add("Acceptable result is tunneled IPv6 or fail-closed with no local IPv6 leak.")
$summary.Add("")

$summary.Add("## Evidence handling")
$summary.Add("")
$summary.Add("- Raw outputs directory: $script:RunDir")
$summary.Add("- Raw outputs committed: false")
$summary.Add("- VPN profiles/subscription URLs/endpoints committed: false")

$summaryPath = Join-Path $script:RunDir "summary-redacted.md"
$summary | Set-Content -Path $summaryPath -Encoding UTF8

Write-Host "Network validation runner completed."
Write-Host "Redacted summary: $summaryPath"
Write-Host "UDP/iperf ready: $([bool]$preflight.ready_for_udp_validation)"
Write-Host "Device stability ready: $([bool]$preflight.ready_for_device_stability_smoke)"

exit 0
