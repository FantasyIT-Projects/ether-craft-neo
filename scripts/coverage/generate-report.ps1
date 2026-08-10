# generate-report.ps1
# Merges collected .exec files and generates:
#   1. HTML report (build/coverage/report/)
#   2. Per-save coverage matrix: which ether_craft classes each save covered (build/coverage/coverage-matrix.csv)
#   3. Uncovered blind-spot list (build/coverage/uncovered-classes.txt)
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/coverage/generate-report.ps1

param(
    [string]$OutDir = "",
    [string]$ClassesDir = "",
    [string]$SourceDir = ""
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $OutDir)    { $OutDir = Join-Path $repoRoot "build\coverage" }
if (-not $ClassesDir){ $ClassesDir = Join-Path $repoRoot "build\classes\java\main" }
if (-not $SourceDir) { $SourceDir = Join-Path $repoRoot "src\main\java" }

$infoFile = Join-Path $repoRoot "build\moddev\coverageInfo.properties"
$java = "java"

function Read-CoverageInfo {
    param([string]$File)
    $props = @{}
    foreach ($line in Get-Content $File) {
        if ($line -match '^\s*#' -or $line.Trim() -eq '') { continue }
        $eq = $line.IndexOf('=')
        if ($eq -gt 0) { $props[$line.Substring(0, $eq)] = $line.Substring($eq + 1) }
    }
    return $props
}

$info = Read-CoverageInfo $infoFile
$cliCp = $info["cliClasspath"]
if (-not $cliCp) { throw "cliClasspath missing in $infoFile" }

$execDir = Join-Path $OutDir "exec"
$execFiles = @(Get-ChildItem -LiteralPath $execDir -Filter "*.exec" -ErrorAction SilentlyContinue)
if ($execFiles.Count -eq 0) { throw "No .exec files found in $execDir. Run run-coverage.ps1 first." }

# Map exec index (01, 02, ...) back to the original save name via summary.csv
$saveNameByIndex = @{}
$summaryFile = Join-Path $OutDir "summary.csv"
if (Test-Path $summaryFile) {
    $csv = Import-Csv $summaryFile -Encoding UTF8
    foreach ($row in $csv) {
        if ($row.ExecIndex -and $row.Save) {
            $saveNameByIndex[$row.ExecIndex] = $row.Save
        }
    }
}
function Get-SaveDisplayName($execBaseName) {
    $m = [regex]::Match($execBaseName, '^(\d+)$')
    if ($m.Success -and $saveNameByIndex.ContainsKey($m.Groups[1].Value)) {
        return $saveNameByIndex[$m.Groups[1].Value]
    }
    return $execBaseName
}

$merged = Join-Path $OutDir "merged.exec"
$reportDir = Join-Path $OutDir "report"
$matrixCsv = Join-Path $OutDir "coverage-matrix.csv"
$uncoveredTxt = Join-Path $OutDir "uncovered-classes.txt"
$xmlFile = Join-Path $OutDir "report.xml"

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

Write-Host "=== Generate coverage report ==="
Write-Host "Exec files: $($execFiles.Count)"

# 1. Merge
Write-Host "Merging exec files -> merged.exec"
$mergeArgs = @("-cp", $cliCp, "org.jacoco.cli.internal.Main", "merge")
foreach ($f in $execFiles) { $mergeArgs += $f.FullName }
$mergeArgs += "--destfile", $merged
& $java $mergeArgs 2>&1 | Write-Host
if ($LASTEXITCODE -ne 0) { throw "jacococli merge failed (exit $LASTEXITCODE)" }

# 2. HTML + XML total report
Write-Host "Generating HTML + XML report"
$repArgs = @("-cp", $cliCp, "org.jacoco.cli.internal.Main", "report", $merged,
    "--classfiles", $ClassesDir,
    "--sourcefiles", $SourceDir,
    "--html", $reportDir,
    "--xml", $xmlFile,
    "--name", "Ether Craft test-save coverage")
& $java $repArgs 2>&1 | Write-Host
if ($LASTEXITCODE -ne 0) { throw "jacococli report failed (exit $LASTEXITCODE)" }

# 3. Per-save XML to compute coverage matrix
Write-Host "Computing per-save coverage matrix"
$allClasses = @{}          # class -> set of saves that covered it
$perSaveCounts = @{}       # save -> {covered, total}
$tempDir = Join-Path $OutDir "tmp-xml"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

function Get-LineCovered($xmlPath) {
    # returns hashtable classBinaryName -> bool(covered>0)
    [xml]$doc = Get-Content $xmlPath -Encoding UTF8
    $map = @{}
    $ns = New-Object System.Xml.XmlNamespaceManager($doc.NameTable)
    $ns.AddNamespace("x", $doc.DocumentElement.NamespaceURI)
    foreach ($classNode in $doc.SelectNodes("//x:class", $ns)) {
        $name = $classNode.GetAttribute("name")
        $lineCounter = $classNode.SelectSingleNode("x:counter[@type='LINE']", $ns)
        if ($lineCounter) {
            $covered = [int]$lineCounter.GetAttribute("covered")
            $missed = [int]$lineCounter.GetAttribute("missed")
            $map[$name] = @{ Covered = $covered; Missed = $missed }
        }
    }
    return $map
}

foreach ($f in $execFiles) {
    $saveName = $f.BaseName
    $displayName = Get-SaveDisplayName $saveName
    $singleXml = Join-Path $tempDir "$saveName.xml"
    $args = @("-cp", $cliCp, "org.jacoco.cli.internal.Main", "report", $f.FullName,
        "--classfiles", $ClassesDir, "--xml", $singleXml, "--quiet")
    & $java $args 2>&1 | Out-Null
    $map = Get-LineCovered $singleXml
    $covered = 0; $total = 0
    foreach ($k in $map.Keys) {
        $total++
        if ($map[$k].Covered -gt 0) {
            $covered++
            if (-not $allClasses.ContainsKey($k)) { $allClasses[$k] = @() }
            $allClasses[$k] += $displayName
        }
    }
    $perSaveCounts[$displayName] = @{ Covered = $covered; Total = $total }
    Write-Host ("  {0}: {1}/{2} classes covered" -f $displayName, $covered, $total)
}

# 4. Write matrix CSV
$rows = @()
foreach ($cls in ($allClasses.Keys | Sort-Object)) {
    $rows += [PSCustomObject]@{ "class" = $cls; "coveredBy" = ($allClasses[$cls] -join ";") }
}
$rows | Export-Csv -Path $matrixCsv -NoTypeInformation -Encoding UTF8

# 5. Uncovered blind spots: classes with total>0 but covered==0 across ALL saves
$totals = @{}
foreach ($f in $execFiles) {
    $saveName = $f.BaseName
    $singleXml = Join-Path $tempDir "$saveName.xml"
    $map = Get-LineCovered $singleXml
    foreach ($k in $map.Keys) {
        if (-not $totals.ContainsKey($k)) { $totals[$k] = @{ Covered = 0; Missed = 0 } }
        $totals[$k].Covered += $map[$k].Covered
        $totals[$k].Missed += $map[$k].Missed
    }
}
$uncovered = @($totals.Keys | Where-Object { $totals[$_].Covered -eq 0 -and $totals[$_].Missed -gt 0 } | Sort-Object)
Set-Content -Path $uncoveredTxt -Value "== Classes never covered by any test save ==" -Encoding UTF8
Add-Content -Path $uncoveredTxt -Value "(line covered == 0 across all saves)" -Encoding UTF8
Add-Content -Path $uncoveredTxt -Value "" -Encoding UTF8
foreach ($c in $uncovered) { Add-Content -Path $uncoveredTxt -Value $c -Encoding UTF8 }

# Cleanup temp
Remove-Item -Recurse -Force $tempDir -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=== Done ==="
Write-Host "HTML report:     $reportDir\index.html"
Write-Host "XML report:      $xmlFile"
Write-Host "Coverage matrix: $matrixCsv"
Write-Host "Uncovered list:  $uncoveredTxt"
Write-Host "Total classes tracked: $($totals.Count)"
Write-Host "Classes never covered: $($uncovered.Count)"
