# Native Minecraft cuboid models for the wood stove.
# Textures (wood_stove_*.png) come exclusively from tools/generate_textures.py
# (single texture source per AGENTS.md 128x128 rule; this script only builds models).
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$assets=Join-Path $root 'src/main/resources/assets/snc_energies'
function Box($from,$to,$tex,$front=$null,$top=$null) {
 $faces=[ordered]@{}
 foreach($face in @('down','up','north','south','west','east')) {
  $t=$tex;if($face -eq 'north' -and $front){$t=$front};if($face -eq 'up' -and $top){$t=$top}
  $faces[$face]=@{uv=@(0,0,16,16);texture=$t}
 }
 return @{from=$from;to=$to;faces=$faces}
}
$elements=@(
 (Box @(0,0,1) @(16,1,16) '#iron'),
 (Box @(1,1,2) @(3,3,15) '#brick'),
 (Box @(13,1,2) @(15,3,15) '#brick'),
 (Box @(1,3,2) @(15,10,15) '#brick'),
 (Box @(0,10,0) @(16,11,16) '#iron' $null '#cooktop'),
 (Box @(2.4,4,0.9) @(8.5,9.4,2.1) '#iron'),
 (Box @(3,4.6,0.7) @(7.9,8.8,0.9) '#iron' '#door'),
 (Box @(9,4,1) @(14,9.4,2.1) '#iron' '#oven'),
 (Box @(2.6,3.2,0.6) @(8.3,3.8,2.1) '#iron'),
 (Box @(7.7,6,0.1) @(8.2,7.4,0.7) '#iron'),
 (Box @(9.7,6,0.2) @(13.2,6.5,1) '#iron'),
 (Box @(2.3,5,0.6) @(2.8,5.6,1.4) '#iron'),
 (Box @(2.3,8,0.6) @(2.8,8.6,1.4) '#iron'),
 (Box @(3.5,1.1,1.3) @(5.5,2.8,9) '#wood'),
 (Box @(6,1.1,1.4) @(8,2.8,9) '#wood'),
 (Box @(8.5,1.1,1.2) @(10.5,2.8,9) '#wood'),
 (Box @(11,1.1,1.5) @(12.5,2.8,9) '#wood'),
 (Box @(10,11,11) @(15,11.5,16) '#iron'),
 (Box @(11,11.5,12) @(14,15.2,15) '#iron'),
 (Box @(10.5,15.2,11.5) @(14.5,16,15.5) '#iron')
)
foreach($mode in @('off','on')) {
 $textures=@{particle='snc_energies:block/wood_stove_brick'}
 foreach($kind in @('brick','iron','cooktop','oven','wood')) {$textures[$kind]="snc_energies:block/wood_stove_$kind"}
 $textures['door']="snc_energies:block/wood_stove_door_$mode"
 @{textures=$textures;elements=$elements;display=@{gui=@{rotation=@(30,225,0);scale=@(0.9,0.9,0.9)};fixed=@{rotation=@(0,180,0);scale=@(0.7,0.7,0.7)}}} | ConvertTo-Json -Depth 14 | Set-Content (Join-Path $assets "models/block/wood_stove_$mode.json")
}
# Note: generate_large_wood_stove.ps1 is invoked by generate_textures.ps1 with a pwsh 7 guard.
