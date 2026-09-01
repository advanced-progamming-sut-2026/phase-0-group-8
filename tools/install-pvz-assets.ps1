param(
    [Parameter(Mandatory=$true)]
    [string]$Source
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Destination = Join-Path $ProjectRoot "assets\pvz"
$Source = (Resolve-Path $Source).Path

$SourceAtlases = Join-Path $Source "ATLASES"
$SourceImages = Join-Path $Source "IMAGES"
if (-not (Test-Path $SourceAtlases -PathType Container)) {
    throw "ATLASES folder not found under: $Source"
}
if (-not (Test-Path $SourceImages -PathType Container)) {
    throw "IMAGES folder not found under: $Source"
}

New-Item -ItemType Directory -Force -Path (Join-Path $Destination "ATLASES") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $Destination "IMAGES") | Out-Null

Write-Host "Copying direct atlas PNG files..."
$atlasFiles = Get-ChildItem -LiteralPath $SourceAtlases -File | Where-Object { $_.Extension -ieq ".png" }
foreach ($file in $atlasFiles) {
    Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $Destination "ATLASES") -Force
}

$nestedAtlasPngs = Get-ChildItem -LiteralPath $SourceAtlases -Recurse -File | Where-Object {
    $_.Extension -ieq ".png" -and $_.DirectoryName -ne $SourceAtlases
}
if ($nestedAtlasPngs.Count -gt 0) {
    Write-Warning "Found nested atlas PNGs. libPVZ scans ATLASES only at the top level. Copying them by filename to the top level as well."
    foreach ($file in $nestedAtlasPngs) {
        Copy-Item -LiteralPath $file.FullName -Destination (Join-Path $Destination "ATLASES\$($file.Name)") -Force
    }
}

Write-Host "Copying the IMAGES tree (this can take a while)..."
Copy-Item -Path (Join-Path $SourceImages "*") -Destination (Join-Path $Destination "IMAGES") -Recurse -Force

foreach ($jsonName in @("RESOURCES.json", "resources.json", "animations.json")) {
    $candidate = Join-Path $Source $jsonName
    if (Test-Path $candidate -PathType Leaf) {
        Copy-Item -LiteralPath $candidate -Destination $Destination -Force
    }
}

Write-Host ""
Write-Host "Asset copy finished. Running validation..."
& (Join-Path $PSScriptRoot "check-pvz-assets.ps1")
