# Industrial textures come exclusively from tools/generate_textures.py
# (single texture source per AGENTS.md 128x128 rule).
# This script regenerates the five-tier resources: multiblock models, tin,
# steam ducts, tier recipes, loot, translations and previews.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'generate_colonial.ps1')
python (Join-Path $PSScriptRoot 'generate_industry.py')
if($LASTEXITCODE -ne 0){throw 'Industrial resource generation failed'}
