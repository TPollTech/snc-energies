# Canonical 128 px procedural textures for the Colonial workshop.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path (Split-Path -Parent $PSScriptRoot) 'src/main/resources/assets/snc_energies/textures'
function Brush([string]$color) { return [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($color)) }
function Rect($g,$color,$x,$y,$w,$h) { $b=Brush $color; $g.FillRectangle($b,[single]$x,[single]$y,[single]$w,[single]$h); $b.Dispose() }
function Oval($g,$color,$x,$y,$w,$h) { $b=Brush $color; $g.FillEllipse($b,[single]$x,[single]$y,[single]$w,[single]$h); $b.Dispose() }
function Line($g,$color,$width,$x,$y,$xx,$yy) { $p=[System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml($color),[single]$width); $g.DrawLine($p,[single]$x,[single]$y,[single]$xx,[single]$yy); $p.Dispose() }
function Texture($name,$folder,[scriptblock]$draw) {
    $bitmap=[System.Drawing.Bitmap]::new(128,128)
    $graphics=[System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::Transparent)
    & $draw $graphics
    $directory=Join-Path $assetRoot $folder
    [void][System.IO.Directory]::CreateDirectory($directory)
    $bitmap.Save((Join-Path $directory "$name.png"),[System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose(); $bitmap.Dispose()
}
Texture 'colonial_wood' 'block' { param($g)
    Rect $g '#785031' 0 0 128 128
    $rng=[Random]::new(45)
    for($i=0;$i -lt 95;$i++) { $y=$rng.Next(128); Line $g '#926440' 1 ($rng.Next(80)) $y 128 ($y+$rng.Next(-2,3)) }
    foreach($y in @(0,31,63,95,127)) { Line $g '#3b2b22' 2 0 $y 128 $y }
    Oval $g '#593b29' 31 45 25 7; Oval $g '#97643a' 36 47 16 3
    foreach($x in @(7,119)) { foreach($y in @(9,40,73,104)) { Oval $g '#302f2a' $x $y 5 5; Oval $g '#b8afa0' $x $y 2 2 } }
}
Texture 'colonial_iron' 'block' { param($g)
    Rect $g '#51585a' 0 0 128 128
    for($y=0;$y -lt 128;$y+=3) { Line $g '#636b6c' 1 0 $y 128 $y }
    foreach($x in @(7,112)) { foreach($y in @(7,112)) { Oval $g '#282e31' $x $y 10 10; Oval $g '#aab0ae' ($x+2) ($y+1) 5 4 } }
    Line $g '#acb1ab' 2 1 1 127 1
}
Texture 'colonial_stone' 'block' { param($g)
    Rect $g '#89887c' 0 0 128 128; $rng=[Random]::new(96)
    for($i=0;$i -lt 700;$i++) { $x=$rng.Next(128);$y=$rng.Next(128);Rect $g $(if($i%2){'#9b9a8c'}else{'#74766c'}) $x $y 2 2 }
    foreach($y in @(20,45,70,95,120)){Line $g '#5d625c' 2 0 $y 128 ($y-16)}
}
foreach($crop in @('rice','soy','mate')) {
    for($age=0;$age -lt 8;$age++) {
        Texture "${crop}_stage$age" 'block' { param($g)
            $top=110-$age*12
            foreach($x in @(22,45,68,94)) {
                Line $g '#3c5727' 5 $x 127 ($x+4) $top
                Line $g '#799642' 2 ($x+2) 127 ($x+5) $top
                for($y=113;$y -gt $top+8;$y-=19) {
                    Line $g '#597e36' 6 $x $y ($x-13) ($y-15)
                    Line $g '#91ad4b' 4 ($x+3) ($y-8) ($x+14) ($y-20)
                    if($crop -in @('soy','mate')) { Oval $g '#6c973d' ($x-16) ($y-20) 15 9 }
                }
                if($age -ge 5) {
                    if($crop -eq 'rice') {
                        Line $g '#bba65b' 3 ($x+4) ($top+13) ($x+15) ($top+5)
                        foreach($offset in @(0,6,12)) { Oval $g '#dbc779' ($x+$offset) ($top+6+$offset/2) 6 9 }
                    } elseif($crop -eq 'soy') {
                        foreach($offset in @(5,18,31)) { Oval $g $(if($age -eq 7){'#c7ac5b'}else{'#91a848'}) ($x+4) ($top+$offset) 6 14 }
                    }
                }
            }
        }
    }
}
foreach($name in @('mate_seeds','mate_leaf','dried_mate','ground_mate','mate_infusion','rice_seeds','soy_seeds','rice_paddy','rice','soybean','flour','rice_husk','vegetable_oil','soy_meal','biomass_briquette','field_guide')) {
    Texture $name 'item' { param($g)
        switch($name) {
            'mate_infusion' {
                Oval $g '#382c1d' 28 39 72 77;Oval $g '#94643b' 32 35 64 73
                Oval $g '#bda36c' 29 30 71 25;Oval $g '#647d38' 36 34 57 16
                Line $g '#d6ddd2' 6 72 70 88 14;Line $g '#7c8b88' 2 74 71 90 15
            }
            {$_ -in 'mate_leaf','dried_mate','ground_mate','mate_seeds'} {
                $color=if($name -eq 'mate_leaf'){'#76a64c'}else{'#a1a25a'}
                foreach($x in @(24,48,71)){Oval $g '#334c2b' $x 32 29 65;Oval $g $color ($x+3) 30 23 59;Line $g '#c1c786' 2 ($x+13) 87 ($x+13) 35}
                if($name -eq 'ground_mate'){Rect $g '#8e744a' 20 74 88 30;Line $g '#bea378' 4 22 79 106 79}
                if($name -eq 'mate_seeds'){foreach($x in @(28,52,78)){Oval $g '#d2b582' $x 76 19 20}}
            }
            'field_guide' {
                Rect $g '#211b13' 20 12 88 105; Rect $g '#956534' 23 14 82 99
                Rect $g '#ead9ac' 33 17 67 92; Rect $g '#ba8849' 26 14 13 99
                Rect $g '#67754a' 48 38 42 46; Line $g '#e2cb7e' 5 69 72 69 48
                Line $g '#e2cb7e' 4 69 57 59 50;Line $g '#e2cb7e' 4 69 65 79 57
                foreach($y in @(25,49,73,97)){Rect $g '#493723' 20 $y 19 4}
            }
            'biomass_briquette' {
                Rect $g '#302b21' 19 32 88 62; Rect $g '#705c3d' 23 29 80 59
                Rect $g '#927849' 23 29 80 10
                foreach($y in @(48,62,76)){Line $g '#b29a66' 3 28 $y 98 ($y-3)}
                Rect $g '#414436' 35 27 8 65;Rect $g '#414436' 82 27 8 65
            }
            'vegetable_oil' {
                Oval $g '#705222' 23 26 82 83; Oval $g '#c7962d' 29 24 70 78
                Oval $g '#efd362' 37 34 50 56; Oval $g '#fff0a1' 43 42 16 25
            }
            {$_ -in 'flour','soy_meal'} {
                Rect $g '#7e6541' 24 37 80 69; Rect $g '#bba575' 29 42 70 58
                Oval $g $(if($name -eq 'flour'){'#f1e8cb'}else{'#cab373'}) 30 26 68 48
                Rect $g '#7a8c58' 42 70 44 20;Line $g '#5f5134' 3 29 104 98 104
            }
            default {
                $rng=[Random]::new(17)
                for($i=0;$i -lt 13;$i++) {
                    $x=$rng.Next(20,90);$y=$rng.Next(23,91)
                    $color=switch($name){'rice'{'#eee6ca'} 'soybean'{'#d6bd6b'} 'soy_seeds'{'#b4ad62'} 'rice_husk'{'#b58b45'} default{'#d4bc70'}}
                    $w=if($name -like 'soy*'){18}else{8};$h=if($name -like 'soy*'){17}else{23}
                    Oval $g '#675331' ($x-2) ($y-2) ($w+4) ($h+4);Oval $g $color $x $y $w $h
                    Line $g '#f4dfa4' 2 ($x+3) ($y+4) ($x+3) ($y+$h-4)
                }
                if($name -like '*seeds'){Line $g '#465c2c' 5 66 106 67 73;Line $g '#92ae51' 5 67 92 84 76}
            }
        }
    }
}
python (Join-Path $PSScriptRoot 'generate_colonial.py')
if ($LASTEXITCODE -ne 0) { throw 'Colonial resource generation failed' }
Write-Host 'Colonial textures: 128 x 128.'
