# SNC Energies - Procedural HD texture generator (128x128 rule from AGENTS.md)
# Requires Windows PowerShell 5.1 + .NET System.Drawing
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$txBlock = Join-Path $root 'src\main\resources\assets\snc_energies\textures\block'
$txItem  = Join-Path $root 'src\main\resources\assets\snc_energies\textures\item'
$txGui   = Join-Path $root 'src\main\resources\assets\snc_energies\textures\gui'
$txRoot  = Join-Path $root 'src\main\resources\assets\snc_energies'
New-Item -ItemType Directory -Force -Path $txBlock, $txItem, $txGui | Out-Null

$rand = [System.Random]::new(1337)

# ---- SNC industrial palette ----
$steelDark   = [System.Drawing.Color]::FromArgb(255, 40, 44, 50)
$steelBase   = [System.Drawing.Color]::FromArgb(255, 58, 62, 70)
$steelLight  = [System.Drawing.Color]::FromArgb(255, 82, 88, 98)
$steelHi     = [System.Drawing.Color]::FromArgb(255, 118, 124, 134)
$frame       = [System.Drawing.Color]::FromArgb(255, 24, 26, 30)
$orange      = [System.Drawing.Color]::FromArgb(255, 255, 140, 26)
$orangeHot   = [System.Drawing.Color]::FromArgb(255, 255, 208, 92)
$ember       = [System.Drawing.Color]::FromArgb(255, 214, 72, 18)
$copper      = [System.Drawing.Color]::FromArgb(255, 198, 110, 58)
$copperLight = [System.Drawing.Color]::FromArgb(255, 240, 172, 104)
$cyan        = [System.Drawing.Color]::FromArgb(255, 64, 220, 255)
$cyanGlow    = [System.Drawing.Color]::FromArgb(255, 168, 246, 255)
$cyanCell    = [System.Drawing.Color]::FromArgb(255, 16, 56, 76)
$yellow      = [System.Drawing.Color]::FromArgb(255, 255, 214, 64)
$yellowCore  = [System.Drawing.Color]::FromArgb(255, 255, 248, 196)
$vioCore     = [System.Drawing.Color]::FromArgb(255, 186, 86, 255)
$vioMid      = [System.Drawing.Color]::FromArgb(255, 140, 54, 214)
$vioDark     = [System.Drawing.Color]::FromArgb(255, 84, 26, 138)
$vioGlow     = [System.Drawing.Color]::FromArgb(255, 236, 190, 255)
$stoneBase   = [System.Drawing.Color]::FromArgb(255, 126, 126, 126)
$deepBase    = [System.Drawing.Color]::FromArgb(255, 78, 80, 88)

function New-Tex {
	param([int]$size = 128)
	$bmp = New-Object System.Drawing.Bitmap($size, $size)
	return @{ B = $bmp; G = [System.Drawing.Graphics]::FromImage($bmp); S = $size }
}

function Save-Tex($t, [string]$name, [string]$dir) {
	$g = $t.G
	$g.Dispose()
	$path = Join-Path $dir "$name.png"
	$t.B.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
	$t.B.Dispose()
	Write-Host "  -> $name.png"
}

# fill a rectangle with color
function Rect($g, [int]$x, [int]$y, [int]$w, [int]$h, [System.Drawing.Color]$c) {
	$b = New-Object System.Drawing.SolidBrush($c)
	$g.FillRectangle($b, $x, $y, $w, $h)
	$b.Dispose()
}

# pixel
function Px($g, [int]$x, [int]$y, [System.Drawing.Color]$c) {
	$b = New-Object System.Drawing.SolidBrush($c)
	$g.FillRectangle($b, $x, $y, 1, 1)
	$b.Dispose()
}

# soft radial glow (alpha falloff), n = radius
function Glow($g, [single]$cx, [single]$cy, [single]$n, [System.Drawing.Color]$c) {
	for ($dy = -$n; $dy -le $n; $dy++) {
		for ($dx = -$n; $dx -le $n; $dx++) {
			$d = [Math]::Sqrt($dx*$dx + $dy*$dy) / $n
			if ($d -gt 1) { continue }
			$a = [int](220 * (1 - $d) * (1 - $d))
			if ($a -le 1) { continue }
			$col = [System.Drawing.Color]::FromArgb($a, $c.R, $c.G, $c.B)
			$b = New-Object System.Drawing.SolidBrush($col)
			$g.FillRectangle($b, [int]($cx+$dx), [int]($cy+$dy), 1, 1)
			$b.Dispose()
		}
	}
}

# brushed steel background with horizontal streaks
function BrushedSteel($g, [int]$size, [System.Drawing.Color]$base) {
	Rect $g 0 0 $size $size $base
	for ($i = 0; $i -lt 900; $i++) {
		$x = $rand.Next(0, $size); $y = $rand.Next(0, $size)
		$len = $rand.Next(6, 40)
		$v = $rand.NextDouble()
		if ($v -lt 0.45) { $c = $steelLight } elseif ($v -lt 0.8) { $steelDark | Out-Null; $c = $steelDark } else { $c = [System.Drawing.Color]::FromArgb(255, [Math]::Min(255,$base.R+18), [Math]::Min(255,$base.G+18), [Math]::Min(255,$base.B+18)) }
		$a = $rand.Next(30, 120)
		$col = [System.Drawing.Color]::FromArgb($a, $c.R, $c.G, $c.B)
		$b = New-Object System.Drawing.SolidBrush($col)
		$g.FillRectangle($b, $x, $y, $len, 1)
		$b.Dispose()
	}
}

# outer industrial frame with bevel highlight
function MachineFrame($g, [int]$size) {
	$w = [Math]::Max(4, [int]($size * 0.05))
	Rect $g 0 0 $size $w $frame
	Rect $g 0 ($size-$w) $size $w $frame
	Rect $g 0 0 $w $size $frame
	Rect $g ($size-$w) 0 $w $size $frame
	Rect $g $w $w ($size-2*$w) 2 $steelHi
}

# hex bolt with highlight
function Bolt($g, [int]$cx, [int]$cy, [int]$r) {
	$b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 96, 102, 112))
	$g.FillEllipse($b, $cx-$r, $cy-$r, 2*$r, 2*$r); $b.Dispose()
	$b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 140, 146, 156))
	$g.FillEllipse($b, $cx-$r+1, $cy-$r+1, $r, $r); $b.Dispose()
	$b = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 34, 37, 42))
	$g.FillEllipse($b, $cx-1, $cy-1, 2, 2); $b.Dispose()
}

# vertical vent grille in rect
function VentGrille($g, [int]$x, [int]$y, [int]$w, [int]$h) {
	Rect $g $x $y $w $h ([System.Drawing.Color]::FromArgb(255, 22, 24, 28))
	$step = [Math]::Max(4, [int]($w/9))
	for ($yy = $y+2; $yy -lt $y+$h-3; $yy += $step) {
		Rect $g ($x+3) $yy ($w-6) ([Math]::Max(1,[int]($step/3))) ([System.Drawing.Color]::FromArgb(255, 66, 72, 82))
	}
}

# lightning bolt polygon
function Lightning($g, [int]$x, [int]$y, [int]$h, [System.Drawing.Color]$c, [System.Drawing.Color]$core) {
	$w = [int]($h*0.62)
	$pts = @(
		[System.Drawing.Point]::new($x+[int]($w*0.55), $y),
		[System.Drawing.Point]::new($x+[int]($w*0.10), $y+[int]($h*0.48)),
		[System.Drawing.Point]::new($x+[int]($w*0.42), $y+[int]($h*0.48)),
		[System.Drawing.Point]::new($x+[int]($w*0.22), $y+$h),
		[System.Drawing.Point]::new($x+[int]($w*0.90), $y+[int]($h*0.42)),
		[System.Drawing.Point]::new($x+[int]($w*0.55), $y+[int]($h*0.42))
	)
	$b = New-Object System.Drawing.SolidBrush($c)
	$g.FillPolygon($b, $pts); $b.Dispose()
	$cx = $x + [int]($w*0.5); $cy = $y + [int]($h*0.45)
	$b = New-Object System.Drawing.SolidBrush($core)
	$g.FillPolygon($b, @(
		[System.Drawing.Point]::new($cx-2, $y+[int]($h*0.12)),
		[System.Drawing.Point]::new($cx+2, $y+[int]($h*0.40)),
		[System.Drawing.Point]::new($cx-1, $y+[int]($h*0.75)),
		[System.Drawing.Point]::new($cx+2, $y+[int]($h*0.40)),
		[System.Drawing.Point]::new($cx-2, $y+[int]($h*0.40))
	)); $b.Dispose()
}

# ===== Shared machine faces =====
function Tex-MachineSide {
	$t = New-Tex 128
	BrushedSteel $t.G 128 $steelBase
	MachineFrame $t.G 128
	Bolt $t.G 16 16 6; Bolt $t.G 112 16 6; Bolt $t.G 16 112 6; Bolt $t.G 112 112 6
	Rect $t.G 12 62 104 3 ([System.Drawing.Color]::FromArgb(90, 20, 22, 26))
	return $t
}
function Tex-MachineTop {
	$t = New-Tex 128
	BrushedSteel $t.G 128 $steelBase
	MachineFrame $t.G 128
	VentGrille $t.G 24 24 80 80
	Bolt $t.G 12 12 5; Bolt $t.G 116 12 5; Bolt $t.G 12 116 5; Bolt $t.G 116 116 5
	return $t
}
function Tex-MachineBottom {
	$t = New-Tex 128
	BrushedSteel $t.G 128 $steelBase
	MachineFrame $t.G 128
	Bolt $t.G 16 16 6; Bolt $t.G 112 16 6; Bolt $t.G 16 112 6; Bolt $t.G 112 112 6
	Rect $t.G 44 44 40 40 ([System.Drawing.Color]::FromArgb(255, 34, 37, 42))
	return $t
}

# ===== Coal generator =====
$side = Tex-MachineSide
Save-Tex $side 'machine_side' $txBlock

$top = Tex-MachineTop
Save-Tex $top 'machine_top' $txBlock

$bot = Tex-MachineBottom
Save-Tex $bot 'machine_bottom' $txBlock

# generator front (off): vent + ember glow + small bolt warning
$gf = New-Tex 128
BrushedSteel $gf.G 128 $steelBase
MachineFrame $gf.G 128
VentGrille $gf.G 20 20 88 52
Glow $gf.G 64 96 26 $ember
Glow $gf.G 64 96 14 $orange
Rect $gf.G 44 86 40 26 ([System.Drawing.Color]::FromArgb(255, 26, 18, 12))
Glow $gf.G 64 99 12 $orangeHot
Bolt $gf.G 16 16 6; Bolt $gf.G 112 16 6; Bolt $gf.G 16 112 6; Bolt $gf.G 112 112 6
Save-Tex $gf 'coal_generator_front_off' $txBlock

# generator front (on): bright flame glow + lightning decal
$gf2 = New-Tex 128
BrushedSteel $gf2.G 128 $steelBase
MachineFrame $gf2.G 128
VentGrille $gf2.G 20 20 88 52
Lightning $gf2.G 40 24 46 $yellow $yellowCore
Glow $gf2.G 64 96 30 $ember
Glow $gf2.G 64 96 18 $orange
Rect $gf2.G 44 86 40 26 ([System.Drawing.Color]::FromArgb(255, 34, 22, 12))
Glow $gf2.G 64 99 16 $orangeHot
Bolt $gf2.G 16 16 6; Bolt $gf2.G 112 16 6; Bolt $gf2.G 16 112 6; Bolt $gf2.G 112 112 6
Save-Tex $gf2 'coal_generator_front_on' $txBlock

# generator side: machine side + big lightning decal
$gs = Tex-MachineSide
Lightning $gs.G 42 26 66 $yellow $yellowCore
Glow $gs.G 58 60 20 ([System.Drawing.Color]::FromArgb(255, 180, 150, 40))
Save-Tex $gs 'coal_generator_side' $txBlock

# ===== Electric furnace =====
# side with copper power port
$fs = Tex-MachineSide
Rect $fs.G 96 48 20 32 ([System.Drawing.Color]::FromArgb(255, 26, 28, 32))
Rect $fs.G 100 54 12 20 $copper
Rect $fs.G 100 54 12 4 $copperLight
Save-Tex $fs 'electric_furnace_side' $txBlock

function FurnaceFront([bool]$lit) {
	$t = New-Tex 128
	BrushedSteel $t.G 128 $steelBase
	MachineFrame $t.G 128
	# viewing window
	Rect $t.G 30 28 68 50 ([System.Drawing.Color]::FromArgb(255, 18, 20, 24))
	Rect $t.G 34 32 60 42 ([System.Drawing.Color]::FromArgb(255, 26, 28, 34))
	if ($lit) {
		Glow $t.G 64 58 24 $ember
		Glow $t.G 64 62 16 $orange
		Glow $t.G 64 66 9 $orangeHot
		for ($i = 0; $i -lt 26; $i++) {
			$xx = 38 + $rand.Next(0, 52); $yy = 40 + $rand.Next(0, 30)
			Px $t.G $xx $yy $orangeHot
		}
	} else {
		for ($i = 0; $i -lt 5; $i++) {
			Rect $t.G 38 (36+$i*8) 52 1 ([System.Drawing.Color]::FromArgb(60, 70, 76, 86))
		}
	}
	# slot frame below
	Rect $t.G 46 88 36 24 ([System.Drawing.Color]::FromArgb(255, 24, 26, 30))
	Rect $t.G 50 92 28 16 ([System.Drawing.Color]::FromArgb(255, 40, 44, 52))
	Bolt $t.G 16 16 6; Bolt $t.G 112 16 6; Bolt $t.G 16 112 6; Bolt $t.G 112 112 6
	return $t
}
Save-Tex (FurnaceFront $false) 'electric_furnace_front_off' $txBlock
Save-Tex (FurnaceFront $true)  'electric_furnace_front_on'  $txBlock

# ===== Crusher =====
# side with hazard stripes
$cs = New-Tex 128
BrushedSteel $cs.G 128 $steelBase
MachineFrame $cs.G 128
for ($i = -4; $i -lt 12; $i++) {
	for ($p = 0; $p -lt 128; $p++) {
		$band = [Math]::Floor(($p + $i*16) / 16) % 2 -eq 0
		if ($band) { Px $cs.G $p ($i*12+8) ([System.Drawing.Color]::FromArgb(255, 224, 178, 40)) }
		else      { Px $cs.G $p ($i*12+8) ([System.Drawing.Color]::FromArgb(255, 30, 30, 34)) }
	}
}
Rect $cs.G 0 4 128 4 $frame
Bolt $cs.G 16 108 6; Bolt $cs.G 112 108 6; Bolt $cs.G 16 40 6; Bolt $cs.G 112 40 6
Save-Tex $cs 'crusher_side' $txBlock

function CrusherFront([bool]$lit) {
	$t = New-Tex 128
	BrushedSteel $t.G 128 $steelBase
	MachineFrame $t.G 128
	# jaw chamber
	Rect $t.G 24 20 80 60 ([System.Drawing.Color]::FromArgb(255, 20, 22, 26))
	# teeth top and bottom
	for ($i = 0; $i -lt 5; $i++) {
		$xx = 26 + $i*16
		$ptsTop = @([System.Drawing.Point]::new($xx, 24), [System.Drawing.Point]::new($xx+14, 24), [System.Drawing.Point]::new($xx+7, 52))
		$ptsBot = @([System.Drawing.Point]::new($xx, 78), [System.Drawing.Point]::new($xx+14, 78), [System.Drawing.Point]::new($xx+7, 50))
		$col = [System.Drawing.Color]::FromArgb(255, 118, 124, 134)
		if ($lit) { $col = [System.Drawing.Color]::FromArgb(255, 255, 170, 70) }
		$b = New-Object System.Drawing.SolidBrush($col)
		$t.G.FillPolygon($b, $ptsTop); $t.G.FillPolygon($b, $ptsBot); $b.Dispose()
	}
	if ($lit) {
		Glow $t.G 64 52 26 ([System.Drawing.Color]::FromArgb(255, 200, 110, 30))
		for ($i = 0; $i -lt 40; $i++) { Px $t.G (30+$rand.Next(0,68)) (30+$rand.Next(0,44)) ([System.Drawing.Color]::FromArgb(255, 120, 110, 100)) }
	}
	Rect $t.G 46 92 36 22 ([System.Drawing.Color]::FromArgb(255, 24, 26, 30))
	Rect $t.G 50 96 28 14 ([System.Drawing.Color]::FromArgb(255, 40, 44, 52))
	Bolt $t.G 12 12 5; Bolt $t.G 116 12 5; Bolt $t.G 12 116 5; Bolt $t.G 116 116 5
	return $t
}
Save-Tex (CrusherFront $false) 'crusher_front_off' $txBlock
Save-Tex (CrusherFront $true)  'crusher_front_on'  $txBlock

# crusher top: hopper opening
$ct = Tex-MachineTop
Rect $ct.G 34 34 60 60 ([System.Drawing.Color]::FromArgb(255, 20, 22, 26))
Rect $ct.G 40 40 48 48 ([System.Drawing.Color]::FromArgb(255, 30, 32, 38))
Rect $ct.G 34 34 60 4 ([System.Drawing.Color]::FromArgb(255, 16, 17, 20))
Save-Tex $ct 'crusher_top' $txBlock

# ===== Energy cube =====
$ec = New-Tex 128
BrushedSteel $ec.G 128 $steelBase
MachineFrame $ec.G 128
Rect $ec.G 18 18 92 92 ([System.Drawing.Color]::FromArgb(255, 26, 30, 36))
foreach ($cell in @(@(24,24), @(68,24), @(24,68), @(68,68))) {
	$cx = $cell[0]; $cy = $cell[1]
	Rect $ec.G $cx $cy 36 36 $cyanCell
	Glow $ec.G ($cx+18) ($cy+18) 15 $cyan
	Rect $ec.G ($cx+4) ($cy+4) 28 28 ([System.Drawing.Color]::FromArgb(40, 10, 30, 40))
	Glow $ec.G ($cx+18) ($cy+18) 10 $cyan
	Rect $ec.G ($cx+12) ($cy+12) 12 12 ([System.Drawing.Color]::FromArgb(255, 220, 250, 255))
}
Bolt $ec.G 10 10 4; Bolt $ec.G 118 10 4; Bolt $ec.G 10 118 4; Bolt $ec.G 118 118 4
Save-Tex $ec 'energy_cube' $txBlock

$ect = New-Tex 128
BrushedSteel $ect.G 128 $steelBase
MachineFrame $ect.G 128
Rect $ect.G 30 30 68 68 ([System.Drawing.Color]::FromArgb(255, 26, 30, 36))
Glow $ect.G 64 64 26 $cyan
Rect $ect.G 52 52 24 24 ([System.Drawing.Color]::FromArgb(255, 220, 250, 255))
Bolt $ect.G 10 10 4; Bolt $ect.G 118 10 4; Bolt $ect.G 10 118 4; Bolt $ect.G 118 118 4
Save-Tex $ect 'energy_cube_top' $txBlock

# ===== Cable =====
$cc = New-Tex 128
Rect $cc.G 0 0 128 128 ([System.Drawing.Color]::FromArgb(255, 36, 39, 45))
Glow $cc.G 64 64 34 $copper
Rect $cc.G 40 40 48 48 $copper
Rect $cc.G 52 52 24 24 $copperLight
Save-Tex $cc 'cable_core' $txBlock

$ca = New-Tex 128
Rect $ca.G 0 0 128 128 ([System.Drawing.Color]::FromArgb(255, 36, 39, 45))
Rect $ca.G 32 32 64 64 $copper
Rect $ca.G 44 44 40 40 $copperLight
Save-Tex $ca 'cable_arm' $txBlock

# ===== Voltaite ore =====
function OreTex([System.Drawing.Color]$base) {
	$t = New-Tex 128
	Rect $t.G 0 0 128 128 $base
	for ($i = 0; $i -lt 2600; $i++) {
		$x = $rand.Next(0,128); $y = $rand.Next(0,128)
		$d = $rand.Next(-14, 15)
		$col = [System.Drawing.Color]::FromArgb(255, [Math]::Max(0,[Math]::Min(255,$base.R+$d)), [Math]::Max(0,[Math]::Min(255,$base.G+$d)), [Math]::Max(0,[Math]::Min(255,$base.B+$d)))
		Px $t.G $x $y $col
	}
	# crystal clusters
	$clusters = @(@(30,34), @(84,28), @(56,72), @(100,88), @(24,96), @(70,110))
	foreach ($cl in $clusters) {
		$cx = $cl[0]; $cy = $cl[1]
		Glow $t.G ($cx+8) ($cy+8) 12 $vioDark
		for ($s = 0; $s -lt 4; $s++) {
			$sx = $cx + $rand.Next(0, 12); $sy = $cy + $rand.Next(0, 12)
			$sh = 6 + $rand.Next(0, 10)
			$pts = @(
				[System.Drawing.Point]::new($sx+3, $sy),
				[System.Drawing.Point]::new($sx+7, $sy+$sh),
				[System.Drawing.Point]::new($sx+3, $sy+[int]($sh*0.7)),
				[System.Drawing.Point]::new($sx, $sy+$sh)
			)
			$b = New-Object System.Drawing.SolidBrush($vioMid)
			$t.G.FillPolygon($b, $pts); $b.Dispose()
			$b = New-Object System.Drawing.SolidBrush($vioCore)
			$t.G.FillPolygon($b, @(
				[System.Drawing.Point]::new($sx+3, $sy+1),
				[System.Drawing.Point]::new($sx+5, $sy+$sh),
				[System.Drawing.Point]::new($sx+3, $sy+[int]($sh*0.6))
			)); $b.Dispose()
			Px $t.G ($sx+3) $sy $vioGlow
		}
	}
	return $t
}
Save-Tex (OreTex $stoneBase) 'voltaite_ore' $txBlock
Save-Tex (OreTex $deepBase)  'deepslate_voltaite_ore' $txBlock

# ===== Items =====
# raw voltaite chunk
$raw = New-Tex 128
Glow $raw.G 64 68 40 $vioDark
$pts = @()
foreach ($ang in @(0, 55, 120, 200, 260, 310)) {
	$rr = 34 + $rand.Next(-8, 8)
	$pts += [System.Drawing.Point]::new([int](64 + $rr*[Math]::Cos($ang*[Math]::PI/180)), [int](66 + $rr*[Math]::Sin($ang*[Math]::PI/180)*0.85))
}
$b = New-Object System.Drawing.SolidBrush($vioDark)
$raw.G.FillPolygon($b, $pts); $b.Dispose()
$b = New-Object System.Drawing.SolidBrush($vioMid)
$raw.G.FillPolygon($b, @(
	[System.Drawing.Point]::new(40, 52), [System.Drawing.Point]::new(74, 40),
	[System.Drawing.Point]::new(94, 68), [System.Drawing.Point]::new(70, 96),
	[System.Drawing.Point]::new(38, 88)
)); $b.Dispose()
$b = New-Object System.Drawing.SolidBrush($vioCore)
$raw.G.FillPolygon($b, @(
	[System.Drawing.Point]::new(48, 56), [System.Drawing.Point]::new(72, 48),
	[System.Drawing.Point]::new(82, 66), [System.Drawing.Point]::new(62, 84)
)); $b.Dispose()
Glow $raw.G 62 62 12 $vioGlow
Save-Tex $raw 'raw_voltaite' $txItem

# voltaite dust pile
$du = New-Tex 128
for ($i = 0; $i -lt 1300; $i++) {
	$x = 20 + $rand.Next(0, 88)
	$edge = [Math]::Abs($x - 64) / 44
	if ($edge -gt 1) { continue }
	$maxY = 96 - [int](30 * (1 - $edge))
	$y = $rand.Next([int]($maxY - 34), $maxY + 1)
	if ($y -gt 100) { continue }
	$v = $rand.NextDouble()
	if ($v -lt 0.25) { $c = $vioCore } elseif ($v -lt 0.6) { $c = $vioMid } else { $c = $vioDark }
	Px $du.G $x $y $c
}
Glow $du.G 64 88 16 $vioDark
Save-Tex $du 'voltaite_dust' $txItem

# voltaite ingot
$in = New-Tex 128
function Ingot($g, [int]$ox, [int]$oy, [System.Drawing.Color]$main, [System.Drawing.Color]$light, [System.Drawing.Color]$dark) {
	# isometric-ish bar
	$b = New-Object System.Drawing.SolidBrush($dark)
	$g.FillPolygon($b, @(
		[System.Drawing.Point]::new($ox+8, $oy+40), [System.Drawing.Point]::new($ox+56, $oy+16),
		[System.Drawing.Point]::new($ox+120, $oy+40), [System.Drawing.Point]::new($ox+120, $oy+62),
		[System.Drawing.Point]::new($ox+72, $oy+90), [System.Drawing.Point]::new($ox+8, $oy+62)
	)); $b.Dispose()
	$b = New-Object System.Drawing.SolidBrush($main)
	$g.FillPolygon($b, @(
		[System.Drawing.Point]::new($ox+8, $oy+40), [System.Drawing.Point]::new($ox+56, $oy+16),
		[System.Drawing.Point]::new($ox+120, $oy+40), [System.Drawing.Point]::new($ox+72, $oy+64)
	)); $b.Dispose()
	$b = New-Object System.Drawing.SolidBrush($light)
	$g.FillPolygon($b, @(
		[System.Drawing.Point]::new($ox+20, $oy+40), [System.Drawing.Point]::new($ox+56, $oy+22),
		[System.Drawing.Point]::new($ox+92, $oy+40), [System.Drawing.Point]::new($ox+56, $oy+56)
	)); $b.Dispose()
}
Ingot $in.G 0 8 $vioMid $vioGlow $vioDark
Ingot $in.G 0 40 $vioMid $vioCore $vioDark
Glow $in.G 56 76 10 $vioGlow
Save-Tex $in 'voltaite_ingot' $txItem

# ===== Mod icon (128) =====
$ic = New-Tex 128
BrushedSteel $ic.G 128 $steelBase
MachineFrame $ic.G 128
Glow $ic.G 64 64 34 ([System.Drawing.Color]::FromArgb(255, 200, 130, 20))
Lightning $ic.G 34 26 76 $yellow $yellowCore
Save-Tex $ic 'icon_128' $txRoot

# ===== GUI backgrounds =====
# NOTE (AGENTS.md exception): GUI backgrounds are 176x166 because vanilla slot/item
# rendering is hard-locked to a 16px grid; blocks/items remain 128x128.
$GUIW = 176; $GUIH = 166
function New-Gui {
	$bmp = New-Object System.Drawing.Bitmap($GUIW, $GUIH)
	return @{ B = $bmp; G = [System.Drawing.Graphics]::FromImage($bmp) }
}
function GuiPanel($t) {
	$g = $t.G
	Rect $g 0 0 $GUIW $GUIH ([System.Drawing.Color]::FromArgb(255, 42, 44, 50))
	Rect $g 0 0 $GUIW 4 ([System.Drawing.Color]::FromArgb(255, 22, 24, 28))
	Rect $g 0 ($GUIH-4) $GUIW 4 ([System.Drawing.Color]::FromArgb(255, 22, 24, 28))
	Rect $g 0 0 4 $GUIH ([System.Drawing.Color]::FromArgb(255, 22, 24, 28))
	Rect $g ($GUIW-4) 0 4 $GUIH ([System.Drawing.Color]::FromArgb(255, 22, 24, 28))
	Rect $g 4 4 ($GUIW-8) 2 ([System.Drawing.Color]::FromArgb(255, 96, 102, 112))
	# title strip
	Rect $g 8 10 ($GUIW-16) 22 ([System.Drawing.Color]::FromArgb(255, 30, 32, 37))
}
function GuiSlot($g, [int]$x, [int]$y) {
	# vanilla-style 18x18 slot at 1x
	Rect $g ($x-1) ($y-1) 18 18 ([System.Drawing.Color]::FromArgb(255, 26, 28, 33))
	Rect $g $x $y 16 16 ([System.Drawing.Color]::FromArgb(255, 18, 20, 24))
	Rect $g $x $y 16 1 ([System.Drawing.Color]::FromArgb(255, 12, 13, 16))
	Rect $g $x $y 1 16 ([System.Drawing.Color]::FromArgb(255, 12, 13, 16))
	Rect $g $x ($y+15) 16 1 ([System.Drawing.Color]::FromArgb(255, 70, 76, 86))
	Rect $g ($x+15) $y 1 16 ([System.Drawing.Color]::FromArgb(255, 70, 76, 86))
}
function GuiEnergyBar($g, [int]$x, [int]$y) {
	# 14x64 frame; fill drawn in screen code (always crisp)
	Rect $g ($x-1) ($y-1) 16 66 ([System.Drawing.Color]::FromArgb(255, 26, 28, 33))
	Rect $g $x $y 14 64 ([System.Drawing.Color]::FromArgb(255, 14, 16, 19))
	Rect $g $x $y 14 2 ([System.Drawing.Color]::FromArgb(255, 10, 11, 13))
	Rect $g $x ($y+62) 14 2 ([System.Drawing.Color]::FromArgb(255, 10, 11, 13))
}
function GuiPlayerInv($g) {
	# main inventory 3 rows + hotbar (1x coords baked at 2x)
	for ($r = 0; $r -lt 3; $r++) { for ($c = 0; $c -lt 9; $c++) { GuiSlot $g (8 + $c*18) (84 + $r*18) } }
	for ($c = 0; $c -lt 9; $c++) { GuiSlot $g (8 + $c*18) (142) }
}

# Coal generator GUI: fuel slot (80,33), energy bar right (150,10), flame drawn in code
$g1 = New-Gui
GuiPanel $g1
GuiSlot $g1.G 80 33
GuiEnergyBar $g1.G 150 10
GuiPlayerInv $g1.G
Save-Tex $g1 'gui_coal_generator' $txGui

# Electric furnace GUI: in (56,26), out (116,26), energy left (8,10)
$g2 = New-Gui
GuiPanel $g2
GuiSlot $g2.G 56 26
GuiSlot $g2.G 116 26
GuiEnergyBar $g2.G 8 10
GuiPlayerInv $g2.G
Save-Tex $g2 'gui_electric_furnace' $txGui

# Crusher GUI: in (56,26), out (116,26), energy left (8,10)
$g3 = New-Gui
GuiPanel $g3
GuiSlot $g3.G 56 26
GuiSlot $g3.G 116 26
GuiEnergyBar $g3.G 8 10
GuiPlayerInv $g3.G
Save-Tex $g3 'gui_crusher' $txGui

Write-Host 'All textures generated.'

# Regenerate the wood stove textures and matching native models.
& (Join-Path $PSScriptRoot 'generate_wood_stove.ps1')
& (Join-Path $PSScriptRoot 'generate_industry.ps1')
