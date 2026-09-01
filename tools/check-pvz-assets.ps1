$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Root = Join-Path $ProjectRoot "assets\pvz"
$AnimationMapPath = Join-Path $Root "pvz-animation-map.json"

Write-Host "PvZ graphics asset check"
Write-Host "Project: $ProjectRoot"
Write-Host ""

$resources = (Test-Path (Join-Path $Root "RESOURCES.json")) -or (Test-Path (Join-Path $Root "resources.json"))
$animations = Test-Path (Join-Path $Root "animations.json")
$atlasDir = Join-Path $Root "ATLASES"
$imagesDir = Join-Path $Root "IMAGES"
$atlasCount = 0
if (Test-Path $atlasDir -PathType Container) {
    $atlasCount = @(Get-ChildItem -LiteralPath $atlasDir -File | Where-Object { $_.Extension -ieq ".png" }).Count
}

Write-Host ("RESOURCES.json : " + $(if ($resources) { "OK" } else { "MISSING" }))
Write-Host ("animations.json  : " + $(if ($animations) { "OK" } else { "MISSING" }))
Write-Host "Atlas PNG files : $atlasCount"

$found = 0
$total = 0
$missing = @()
if (Test-Path $AnimationMapPath -PathType Leaf) {
    $map = Get-Content -LiteralPath $AnimationMapPath -Raw | ConvertFrom-Json
    foreach ($groupName in @("plants", "zombies")) {
        $group = $map.$groupName
        foreach ($property in $group.PSObject.Properties) {
            if ($null -eq $property.Value -or [string]::IsNullOrWhiteSpace($property.Value.pam)) {
                $missing += "$groupName/$($property.Name): no mapping in animations.json"
                continue
            }
            $total++
            $pamPath = $property.Value.pam -replace '/', '\'
            $full = Join-Path $imagesDir $pamPath
            if (Test-Path $full -PathType Leaf) {
                $found++
            } else {
                $missing += "$groupName/$($property.Name): $($property.Value.pam)"
            }
        }
    }
}

Write-Host "Mapped PAM files : $found/$total"
Write-Host ""
if ($atlasCount -eq 0) {
    Write-Warning "No direct PNG files are present in assets\pvz\ATLASES. libPVZ will stay in fallback mode."
}
if ($found -eq 0) {
    Write-Warning "No mapped PAM files are present under assets\pvz\IMAGES. libPVZ will stay in fallback mode."
}

if ($missing.Count -gt 0) {
    Write-Host "Missing/unmapped entries (first 30):"
    $missing | Select-Object -First 30 | ForEach-Object { Write-Host "  - $_" }
    if ($missing.Count -gt 30) { Write-Host "  ... plus $($missing.Count - 30) more" }
}

Write-Host ""
if ($resources -and $atlasCount -gt 0 -and $found -gt 0) {
    Write-Host "READY: libPVZ has the minimum directory structure it needs."
    Write-Host "Now run: gradlew.bat lwjgl3:run"
} else {
    Write-Host "NOT READY: copy the missing ATLASES/IMAGES content, then rerun this script."
}
