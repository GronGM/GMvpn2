[CmdletBinding()]
param(
    [string]$OutputRoot = ".local\validation",
    [switch]$Json
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

function New-RunDirectory {
    param([string]$Root)
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $dir = Join-Path $Root $timestamp
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    (Resolve-Path $dir).Path
}

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

    if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        $wingetRoot = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages"
        if (Test-Path $wingetRoot) {
            $candidate = Get-ChildItem `
                -Path $wingetRoot `
                -Recurse `
                -Filter "iperf3.exe" `
                -ErrorAction SilentlyContinue |
                Select-Object -First 1
            if ($candidate) {
                return $candidate.FullName
            }
        }
    }

    $null
}

function Add-AdbSdkToSessionPath {
    param([string]$AdbPath)
    if ([string]::IsNullOrWhiteSpace($AdbPath)) {
        return
    }
    $platformTools = Split-Path $AdbPath -Parent
    $sdkRoot = Split-Path $platformTools -Parent
    if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $env:ANDROID_HOME = $sdkRoot
    }
    if ([string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        $env:ANDROID_SDK_ROOT = $sdkRoot
    }
    $cmdlineTools = Join-Path $sdkRoot "cmdline-tools\latest\bin"
    $pathParts = $env:Path -split ";"
    if ($pathParts -notcontains $platformTools) {
        $env:Path = "$platformTools;$env:Path"
    }
    if ((Test-Path $cmdlineTools) -and ($pathParts -notcontains $cmdlineTools)) {
        $env:Path = "$cmdlineTools;$env:Path"
    }
}

function Mask-Serial {
    param([string]$Serial)
    if ([string]::IsNullOrWhiteSpace($Serial)) {
        return "<unknown>"
    }
    if ($Serial.Length -le 8) {
        return "<masked>"
    }
    "$($Serial.Substring(0, 4))...$($Serial.Substring($Serial.Length - 4))"
}

function Parse-AdbDevices {
    param([object[]]$Lines)
    $devices = @()
    foreach ($rawLine in $Lines) {
        $line = $rawLine.ToString().Trim()
        if ($line.Length -eq 0 -or $line -like "List of devices attached*") {
            continue
        }
        if ($line -match "^(\S+)\s+(\S+)(.*)$") {
            $serial = $Matches[1]
            $state = $Matches[2]
            $detail = $Matches[3]
            $model = "unknown"
            if ($detail -match "\bmodel:(\S+)") {
                $model = $Matches[1]
            }
            $devices += [pscustomobject]@{
                SerialMasked = Mask-Serial -Serial $serial
                State = $state
                Model = $model
            }
        }
    }
    $devices
}

function Test-EndpointEnv {
    $hostPresent = -not [string]::IsNullOrWhiteSpace($env:GMVPN_IPERF_HOST)
    $portPresent = -not [string]::IsNullOrWhiteSpace($env:GMVPN_IPERF_PORT)
    $portValid = $false
    if ($portPresent) {
        $parsed = 0
        $portValid = [int]::TryParse($env:GMVPN_IPERF_PORT, [ref]$parsed) -and
            $parsed -gt 0 -and $parsed -le 65535
    }
    [pscustomobject]@{
        HostPresent = $hostPresent
        PortPresent = $portPresent
        PortValid = $portValid
        Ready = $hostPresent -and $portPresent -and $portValid
    }
}

$script:RunDir = New-RunDirectory -Root $OutputRoot

$gitBranch = "unknown"
$gitClean = $false
if (Get-Command git -ErrorAction SilentlyContinue) {
    try {
        $gitBranch = (& git rev-parse --abbrev-ref HEAD 2>$null).Trim()
        $gitStatus = @(& git status --porcelain 2>$null)
        Save-RawOutput -Name "git-status-raw.txt" -Lines $gitStatus
        $gitClean = $gitStatus.Count -eq 0
    } catch {
        Save-RawOutput -Name "git-status-raw.txt" -Lines @($_.Exception.Message)
    }
}

$adbPath = Find-AdbPath
Add-AdbSdkToSessionPath -AdbPath $adbPath
$adbAvailable = $false
$authorizedDevices = @()
$deviceRows = @()
if (-not [string]::IsNullOrWhiteSpace($adbPath)) {
    $adbVersion = Invoke-CapturedCommand -FilePath $adbPath -Arguments @("version") -RawFile "adb-version-raw.txt"
    $adbAvailable = $adbVersion.ExitCode -eq 0
    $adbDevices = Invoke-CapturedCommand -FilePath $adbPath -Arguments @("devices", "-l") -RawFile "adb-devices-raw.txt"
    $deviceRows = @(Parse-AdbDevices -Lines $adbDevices.Output)
    $authorizedDevices = @($deviceRows | Where-Object { $_.State -eq "device" })
} else {
    Save-RawOutput -Name "adb-version-raw.txt" -Lines @("adb not found")
    Save-RawOutput -Name "adb-devices-raw.txt" -Lines @("adb not found")
}

$iperfPath = Find-IperfPath
$iperfAvailable = $false
if ($iperfPath) {
    $iperfVersion = Invoke-CapturedCommand `
        -FilePath $iperfPath `
        -Arguments @("--version") `
        -RawFile "iperf3-version-raw.txt"
    $iperfAvailable = $iperfVersion.ExitCode -eq 0
} else {
    Save-RawOutput -Name "iperf3-version-raw.txt" -Lines @("iperf3 not found")
}

$endpoint = Test-EndpointEnv
$deviceReady = $adbAvailable -and $authorizedDevices.Count -gt 0
$udpReady = $deviceReady -and $iperfAvailable -and $endpoint.Ready

$adbStatus = if ($adbAvailable) { "pass" } else { "fail" }
$deviceStatus = if ($deviceReady) { "pass" } else { "fail" }
$iperfStatus = if ($iperfAvailable) { "pass" } else { "fail" }
$endpointStatus = if ($endpoint.Ready) { "present" } else { "missing" }
$udpReadyText = if ($udpReady) { "yes" } else { "no" }
$stabilityReadyText = if ($deviceReady) { "yes" } else { "no" }

$summary = [ordered]@{
    timestamp_utc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    run_dir = $script:RunDir
    git_branch = $gitBranch
    git_clean = $gitClean
    adb = $adbStatus
    authorized_device = $deviceStatus
    authorized_device_count = $authorizedDevices.Count
    authorized_devices_masked = @($authorizedDevices | ForEach-Object { "$($_.SerialMasked) $($_.Model)" })
    iperf3 = $iperfStatus
    approved_endpoint = $endpointStatus
    approved_endpoint_host_present = $endpoint.HostPresent
    approved_endpoint_port_present = $endpoint.PortPresent
    approved_endpoint_port_valid = $endpoint.PortValid
    ready_for_udp_validation = $udpReady
    ready_for_device_stability_smoke = $deviceReady
}

$jsonText = $summary | ConvertTo-Json -Depth 4
$jsonText | Set-Content -Path (Join-Path $script:RunDir "preflight-redacted.json") -Encoding UTF8

$summaryLines = @(
    "# Android validation preflight (redacted)",
    "",
    "- Git branch: $gitBranch",
    "- Git clean: $gitClean",
    "- ADB: $adbStatus",
    "- Device: $deviceStatus",
    "- Authorized devices: $($authorizedDevices.Count)",
    "- iperf3: $iperfStatus",
    "- Approved endpoint: $endpointStatus",
    "- Ready for UDP validation: $udpReadyText",
    "- Ready for device stability smoke: $stabilityReadyText",
    "",
    "Raw command output is stored only under this ignored local directory:",
    "",
    '```text',
    $script:RunDir,
    '```'
)
$summaryLines | Set-Content -Path (Join-Path $script:RunDir "preflight-summary-redacted.md") -Encoding UTF8

if ($Json) {
    $jsonText
    exit 0
}

Write-Host "ADB: $adbStatus"
Write-Host "Device: $deviceStatus"
if ($authorizedDevices.Count -gt 0) {
    $masked = @($authorizedDevices | ForEach-Object { "$($_.SerialMasked) $($_.Model)" })
    Write-Host ("Authorized device masked: " + ($masked -join ", "))
}
Write-Host "iperf3: $iperfStatus"
Write-Host "Approved endpoint: $endpointStatus"
Write-Host "Ready for UDP validation: $udpReadyText"
Write-Host "Ready for device stability smoke: $stabilityReadyText"
Write-Host "Redacted preflight summary: $(Join-Path $script:RunDir 'preflight-summary-redacted.md')"

if (-not $adbAvailable) {
    Write-Warning "ADB is missing. Install Android SDK Platform-Tools or add platform-tools to PATH."
}
if (-not $deviceReady) {
    Write-Warning "No authorized Android device is ready."
}
if (-not $iperfAvailable) {
    Write-Warning "iperf3 is missing from PATH."
}
if (-not $endpoint.Ready) {
    Write-Warning "GMVPN_IPERF_HOST / GMVPN_IPERF_PORT are missing or invalid. Endpoint value was not printed."
}

exit 0
