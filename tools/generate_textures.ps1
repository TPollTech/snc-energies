# SNC Energies - texture pipeline entry point.
# All 128x128 textures are drawn by tools/generate_textures.py (Pillow + numpy):
# layered noise, anisotropic brushed metal, bevel lighting and emissive glows.
# The old System.Drawing scripts are archived in tools/legacy/.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$python=Join-Path $root '.venv-textures/Scripts/python.exe'
if(-not (Test-Path $python)){$python='python'}
& $python (Join-Path $PSScriptRoot 'generate_textures.py')
if($LASTEXITCODE -ne 0){throw 'Texture generation failed'}
# Models and resources that reference the regenerated textures:
& (Join-Path $PSScriptRoot 'generate_wood_stove.ps1')
# The large-stove splitter needs pwsh 7 (-AsHashtable); models ship pre-built in git.
$pwsh7=Get-Command pwsh -ErrorAction SilentlyContinue
if($pwsh7){& pwsh -NoProfile -File (Join-Path $PSScriptRoot 'generate_large_wood_stove.ps1')}
else{Write-Host 'pwsh 7 not found; skipping generate_large_wood_stove.ps1 (models already shipped).' }
& (Join-Path $PSScriptRoot 'generate_colonial.ps1')
& (Join-Path $PSScriptRoot 'generate_industry.ps1')
Write-Host 'Texture pipeline complete.'
