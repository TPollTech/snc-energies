# Colonial textures come exclusively from tools/generate_textures.py
# (single texture source per AGENTS.md 128x128 rule).
# This script only regenerates the canonical resources:
# native models, recipes, loot tables and translations.
$ErrorActionPreference='Stop'
python (Join-Path $PSScriptRoot 'generate_colonial.py')
if ($LASTEXITCODE -ne 0) { throw 'Colonial resource generation failed' }
Write-Host 'Colonial resources regenerated (textures from generate_textures.py).'
