# run-coverage.ps1
# Collects JaCoCo coverage by loading each test save in a dedicated-server run.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/coverage/run-coverage.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/coverage/run-coverage.ps1 -TickSeconds 30 -Worlds "test1,简单测试"
#
# Params:
#   -SavesDir       Directory holding the test saves (default ../test_saves relative to repo root)
#   -TickSeconds    How long to run the server tick loop after world load (default 60)
#   -Worlds         Comma-separated save names to process; empty = all
#   -OutDir         Where to write .exec files (default ../build/coverage)
#   -LogDir         Where to write server console logs (default ../build/coverage/logs)
#   -StartupTimeout Max seconds to wait for "Done" (default 300)
#   -RunDir         Working directory for the server (default ../run)

param(
    [string]$SavesDir = "",
    [int]$TickSeconds = 60,
    [string]$Worlds = "",
    [string]$OutDir = "",
    [string]$LogDir = "",
    [int]$StartupTimeout = 300,
    [string]$RunDir = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $SavesDir) { $SavesDir = Join-Path $repoRoot "test_saves" }
if (-not $OutDir)   { $OutDir = Join-Path $repoRoot "build\coverage" }
if (-not $LogDir)   { $LogDir = Join-Path $OutDir "logs" }
if (-not $RunDir)   { $RunDir = Join-Path $repoRoot "run" }

$infoFile = Join-Path $repoRoot "build\moddev\coverageInfo.properties"
$vmArgsFile = Join-Path $repoRoot "build\moddev\serverRunVmArgs.txt"
$progArgsFile = Join-Path $repoRoot "build\moddev\serverRunProgramArgs.txt"
$classesDir = Join-Path $repoRoot "build\classes\java\main"
$resourcesDir = Join-Path $repoRoot "build\resources\main"

function Read-CoverageInfo {
    param([string]$File)
    $props = @{}
    foreach ($line in Get-Content $File) {
        if ($line -match '^\s*#' -or $line.Trim() -eq '') { continue }
        $eq = $line.IndexOf('=')
        if ($eq -gt 0) {
            $k = $line.Substring(0, $eq)
            $v = $line.Substring($eq + 1)
            $props[$k] = $v
        }
    }
    return $props
}

Write-Host "=== Ether Craft coverage collection ==="
Write-Host "SavesDir: $SavesDir"
Write-Host "OutDir:   $OutDir"
Write-Host "TickSeconds: $TickSeconds"
Write-Host ""

if (-not (Test-Path $SavesDir)) { throw "SavesDir not found: $SavesDir" }
if (-not (Test-Path $infoFile)) { throw "Coverage info not found. Run: gradlew writeCoverageInfo --no-daemon" }
if (-not (Test-Path $vmArgsFile)) { throw "serverRunVmArgs.txt not found: $vmArgsFile" }
if (-not (Test-Path $progArgsFile)) { throw "serverRunProgramArgs.txt not found: $progArgsFile" }

New-Item -ItemType Directory -Force -Path (Join-Path $OutDir "exec") | Out-Null
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$info = Read-CoverageInfo $infoFile
$agentJar = $info["agentJar"]
$classpath = $info["classpath"]
if (-not $agentJar) { throw "agentJar missing in $infoFile" }
if (-not $classpath) { throw "classpath missing in $infoFile" }

# Ensure eula accepted
$eulaFile = Join-Path $RunDir "eula.txt"
New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
if (-not (Test-Path $eulaFile)) { Set-Content -Path $eulaFile -Value "eula=true" -Encoding UTF8 }
else {
    $content = Get-Content $eulaFile -Raw
    if ($content -notmatch 'eula\s*=\s*true') { Set-Content -Path $eulaFile -Value "eula=true" -Encoding UTF8 }
}

# Build fml.modFolders argument (same shape as the IDE run config)
$modFolders = "ether_craft%%$classesDir;ether_craft%%$resourcesDir"

# Read VM args (one per line)
$vmArgs = @(Get-Content $vmArgsFile | Where-Object { $_.Trim() -ne "" })

# Gather save names
$saveDirs = Get-ChildItem -LiteralPath $SavesDir -Directory | Select-Object -ExpandProperty Name
if ($Worlds) {
    $want = @($Worlds -split "," | ForEach-Object { $_.Trim() })
    $saveDirs = $saveDirs | Where-Object { $_ -in $want }
}
Write-Host "Saves to process: $($saveDirs.Count)"
foreach ($s in $saveDirs) { Write-Host "  - $s" }
Write-Host ""

$results = @()
$i = 0
foreach ($saveName in $saveDirs) {
    $i++
    $sanitized = $saveName -replace '[\\/:*?"<>|]', '_'
    $execIndex = "{0:D2}" -f $i
    # Use an ASCII index-based exec filename: JaCoCo's -javaagent destfile is parsed by the JVM
    # with the system codepage, so non-ASCII names would be mangled on disk.
    $execFile = Join-Path $OutDir "exec\$execIndex.exec"
    $logFile = Join-Path $LogDir "$execIndex-$sanitized.log"
    $logOutFile = $logFile + ".out"
    $logErrFile = $logFile + ".err"
    Write-Host "[$i/$($saveDirs.Count)] $saveName"

    # Remove previous exec so a crash/early exit is detectable
    if (Test-Path $execFile) { Remove-Item $execFile -Force }

    # JaCoCo agent
    $agentArg = "-javaagent:$agentJar=destfile=$execFile,append=false,includes=studio.fantasyit.**"

    # Program args: reuse generated file (contains --nogui), then append universe/world
    $progArgs = "@$progArgsFile", "--universe", $SavesDir, "--world", $saveName

    $allArgs = @()
    foreach ($a in $vmArgs) { $allArgs += $a }
    $allArgs += "-Dfml.modFolders=$modFolders"
    $allArgs += $agentArg
    $allArgs += "-cp"
    $allArgs += $classpath
    $allArgs += "net.neoforged.devlaunch.Main"
    foreach ($a in $progArgs) { $allArgs += $a }

    # Build an escaped command-line string (PowerShell 5.1 has no ArgumentList)
    $escapedArgs = $allArgs | ForEach-Object {
        if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "java"
    $psi.Arguments = ($escapedArgs -join ' ')
    $psi.WorkingDirectory = $RunDir
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi

    $status = "unknown"
    Remove-Item "$logFile*" -ErrorAction SilentlyContinue
    try {
        # Forward stdout/stderr into shared ConcurrentQueues (object references passed via
        # -MessageData, shared across the event runspace). The main loop drains the queues and
        # appends to the log files, so no file-lock races occur.
        $logOutFile = $logFile + ".out"
        $logErrFile = $logFile + ".err"
        $outQueue = New-Object System.Collections.Concurrent.ConcurrentQueue[string]
        $errQueue = New-Object System.Collections.Concurrent.ConcurrentQueue[string]
        $evtOut = Register-ObjectEvent -InputObject $proc -EventName OutputDataReceived `
            -MessageData $outQueue -Action {
                if ($EventArgs.Data) {
                    $Event.MessageData.Enqueue($EventArgs.Data)
                }
            }
        $evtErr = Register-ObjectEvent -InputObject $proc -EventName ErrorDataReceived `
            -MessageData $errQueue -Action {
                if ($EventArgs.Data) {
                    $Event.MessageData.Enqueue($EventArgs.Data)
                }
            }

        [void]$proc.Start()
        $proc.BeginOutputReadLine()
        $proc.BeginErrorReadLine()
        $procId = $proc.Id

        $swOut = New-Object System.IO.StreamWriter($logOutFile, $true, [System.Text.Encoding]::UTF8)
        $swErr = New-Object System.IO.StreamWriter($logErrFile, $true, [System.Text.Encoding]::UTF8)
        $swOut.AutoFlush = $true
        $swErr.AutoFlush = $true

        # Wait for "Done" (server ready) while draining the queues into the log files.
        $ready = $false
        $crashedMarker = "Failed to start|Fatal|Exception in thread|Shutting down|Error during loading of the world|Failed to load the level"
        $deadline = (Get-Date).AddSeconds($StartupTimeout)
        while ((Get-Date) -lt $deadline) {
            if ($proc.HasExited) { break }
            Start-Sleep -Milliseconds 200
            $line = $null
            while ($outQueue.TryDequeue([ref]$line)) {
                $swOut.WriteLine($line)
                if ($line.IndexOf('Done (') -ge 0) { $ready = $true }
                if ($line -match $crashedMarker) { $ready = $false; break }
            }
            while ($errQueue.TryDequeue([ref]$line)) {
                $swErr.WriteLine($line)
            }
            if ($ready) { break }
        }
        # Drain anything left
        while ($outQueue.TryDequeue([ref]$line)) { $swOut.WriteLine($line) }
        while ($errQueue.TryDequeue([ref]$line)) { $swErr.WriteLine($line) }
        $swOut.Flush(); $swOut.Close()
        $swErr.Flush(); $swErr.Close()
        if (-not $ready) {
            if ($proc.HasExited) {
                $status = "crashed-before-ready (exit $($proc.ExitCode))"
                Write-Host "  -> $status"
            } else {
                $status = "timeout-waiting-ready"
                Write-Host "  -> $status"
                try { $proc.StandardInput.WriteLine("stop") } catch {}
            }
        } else {
            $status = "ready"
            Write-Host "  -> server ready, ticking ${TickSeconds}s..."
            Start-Sleep -Seconds $TickSeconds
            try { $proc.StandardInput.WriteLine("stop") } catch {}
        }

        # Wait for graceful exit
        $shutdownWait = 120
        if (-not $proc.WaitForExit($shutdownWait * 1000)) {
            Write-Host "  -> not exiting, force killing"
            try { $proc.Kill() } catch {}
            $proc.WaitForExit()
            $status = "$status-force-killed"
        } else {
            Write-Host "  -> exited (code $($proc.ExitCode))"
            if ($status -ne "crashed-before-ready" -and $status -ne "timeout-waiting-ready") {
                $status = if ($proc.ExitCode -eq 0) { "ok" } else { "exit-$($proc.ExitCode)" }
            }
        }

        Unregister-Event -SourceIdentifier $evtOut.Name -ErrorAction SilentlyContinue
        Unregister-Event -SourceIdentifier $evtErr.Name -ErrorAction SilentlyContinue
    } catch {
        $status = "error: $($_.Exception.Message)"
        Write-Host "  -> $status"
    } finally {
        if ($proc -and -not $proc.HasExited) { try { $proc.Kill() } catch {} }
    }

    $hasExec = Test-Path $execFile
    $execSize = if ($hasExec) { (Get-Item $execFile).Length } else { 0 }
    $results += [PSCustomObject]@{
        Save = $saveName
        ExecIndex = $execIndex
        Status = $status
        Exec = $hasExec
        ExecBytes = $execSize
    }
    Write-Host "  -> exec: $hasExec ($execSize bytes)"
    Write-Host ""
}

Write-Host "=== Summary ==="
$results | Format-Table -AutoSize | Out-String | Write-Host
$summaryFile = Join-Path $OutDir "summary.csv"
$results | Export-Csv -Path $summaryFile -NoTypeInformation -Encoding UTF8
Write-Host "Summary written to $summaryFile"
