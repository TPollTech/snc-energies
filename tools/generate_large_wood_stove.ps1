# Split a 3x2 stove and three-block chimney into native per-cell models.
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
$assets=Join-Path $root 'src/main/resources/assets/snc_energies'
$cells=@(@(0,0,0),@(1,0,0),@(2,0,0),@(0,0,1),@(1,0,1),@(2,0,1),@(2,1,1),@(2,2,1))
$combined=@{}
foreach($mode in @('off','on')) {
 $source=Get-Content "$assets/models/block/wood_stove_$mode.json" -Raw | ConvertFrom-Json -AsHashtable
 $expanded=@()
 for($index=0;$index -lt $source.elements.Count;$index++) {
  $e=$source.elements[$index] | ConvertTo-Json -Depth 15 | ConvertFrom-Json -AsHashtable
  foreach($edge in @('from','to')) {
   $v=$e[$edge]
   if($index -ge 17) { $e[$edge]=@((34+($v[0]-10)*2.4),(16+($v[1]-11)*6.4),(18+($v[2]-11)*2.4)) }
   else { $e[$edge]=@(($v[0]*3),($v[1]*16/11),($v[2]*2)) }
  }
  $expanded+=,$e
 }
 $combined[$mode]=@{textures=$source.textures;elements=$expanded}
 for($part=0;$part -lt $cells.Count;$part++) {
  $offset=@(($cells[$part][0]*16),($cells[$part][1]*16),($cells[$part][2]*16))
  $pieces=@()
  foreach($e in $expanded) {
   $lo=@();$hi=@();for($axis=0;$axis -lt 3;$axis++){$lo+=[Math]::Max($e.from[$axis],$offset[$axis]);$hi+=[Math]::Min($e.to[$axis],($offset[$axis]+16))}
   if($hi[0] -le $lo[0] -or $hi[1] -le $lo[1] -or $hi[2] -le $lo[2]){continue}
   $faces=@{}
   foreach($name in $e.faces.Keys) {
    $axis=switch($name){'west'{0}'east'{0}'down'{1}'up'{1}default{2}}
    $lowFace=$name -in @('west','down','north')
    if(($lowFace -and $lo[$axis] -gt $e.from[$axis]+0.0001) -or (!$lowFace -and $hi[$axis] -lt $e.to[$axis]-0.0001)){continue}
    # Preserve UV continuity where a textured face crosses cell boundaries.
    $uAxis=if($name -in @('east','west')){2}else{0}
    $vAxis=if($name -in @('up','down')){2}else{1}
    $u0=16*($lo[$uAxis]-$e.from[$uAxis])/($e.to[$uAxis]-$e.from[$uAxis]);$u1=16*($hi[$uAxis]-$e.from[$uAxis])/($e.to[$uAxis]-$e.from[$uAxis])
    $v0=16*($lo[$vAxis]-$e.from[$vAxis])/($e.to[$vAxis]-$e.from[$vAxis]);$v1=16*($hi[$vAxis]-$e.from[$vAxis])/($e.to[$vAxis]-$e.from[$vAxis])
    if($name -in @('north','east')){$temp=$u0;$u0=16-$u1;$u1=16-$temp}
    if($name -ne 'up'){$temp=$v0;$v0=16-$v1;$v1=16-$temp}
    $faces[$name]=@{texture=$e.faces[$name].texture;uv=@($u0,$v0,$u1,$v1)}
   }
   if($faces.Count){$pieces+=@{from=@(($lo[0]-$offset[0]),($lo[1]-$offset[1]),($lo[2]-$offset[2]));to=@(($hi[0]-$offset[0]),($hi[1]-$offset[1]),($hi[2]-$offset[2]));faces=$faces}}
  }
  @{textures=$source.textures;elements=$pieces} | ConvertTo-Json -Depth 15 | Set-Content "$assets/models/block/wood_stove_large_${part}_$mode.json"
 }
}
$variants=@{};$partVariants=@{}
foreach($facing in @('north','east','south','west')) {
 $rotation=@{north=0;east=90;south=180;west=270}[$facing]
 foreach($lit in @('false','true')) {
  $mode=if($lit -eq 'true'){'on'}else{'off'}
  $variants["facing=$facing,lit=$lit,assembled=false"]=@{model="snc_energies:block/wood_stove_$mode";y=$rotation}
  $variants["facing=$facing,lit=$lit,assembled=true"]=@{model="snc_energies:block/wood_stove_large_0_$mode";y=$rotation}
  for($part=1;$part -lt 8;$part++) {$partVariants["facing=$facing,lit=$lit,part=$part"]=@{model="snc_energies:block/wood_stove_large_${part}_$mode";y=$rotation}}
 }
}
@{variants=$variants} | ConvertTo-Json -Depth 6 | Set-Content "$assets/blockstates/wood_stove.json"
@{variants=$partVariants} | ConvertTo-Json -Depth 6 | Set-Content "$assets/blockstates/wood_stove_part.json"
$inventory=$combined.off | ConvertTo-Json -Depth 15 | ConvertFrom-Json -AsHashtable
foreach($element in $inventory.elements){foreach($edge in @('from','to')){$v=$element[$edge];$element[$edge]=@(($v[0]/3),($v[1]/3),($v[2]/3+8/3))}}
$inventory.display=@{gui=@{rotation=@(25,225,0);scale=@(0.9,0.9,0.9)}}
$inventory | ConvertTo-Json -Depth 15 | Set-Content "$assets/models/item/wood_stove.json"
$combined | ConvertTo-Json -Depth 15 -Compress | Set-Content (Join-Path $root 'previews/wood-stove-large-model.json')
