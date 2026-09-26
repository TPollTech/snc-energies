# Generate the approved industrial materials and all five-tier resources.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'generate_colonial.ps1')
foreach($name in @('industry_steel','industry_bronze','industry_panel','industry_core','industry_gauge')) {
    Texture $name 'block' { param($g)
        $base=switch($name){'industry_bronze'{'#9c683e'} 'industry_panel'{'#b0ae8e'} 'industry_core'{'#542d77'} default{'#39474e'}}
        Rect $g $base 0 0 128 128
        for($y=2;$y -lt 128;$y+=4){Line $g '#10ffffff' 1 0 $y 128 $y}
        Rect $g '#242d31' 0 0 128 5;Rect $g '#242d31' 0 123 128 5
        foreach($x in @(7,111)){foreach($y in @(8,111)){Oval $g '#20282a' $x $y 10 10;Oval $g '#adb4aa' ($x+2) ($y+1) 5 4}}
        if($name -eq 'industry_panel'){foreach($y in @(24,42,60,78,96)){Rect $g '#4f5b50' 16 $y 96 7;Line $g '#d7d3b3' 1 16 ($y+8) 112 ($y+8)}}
        if($name -eq 'industry_core'){foreach($x in @(27,58,89)){Rect $g '#b46cea' $x 16 12 96;Rect $g '#e4afff' ($x+4) 16 4 96}}
        if($name -eq 'industry_gauge'){
            Oval $g '#151e22' 14 14 100 100;Oval $g '#c8c6af' 23 23 82 82
            for($i=0;$i -lt 10;$i++){$angle=($i*28+140)*[Math]::PI/180;Line $g '#434a40' 3 (64+29*[Math]::Cos($angle)) (64+29*[Math]::Sin($angle)) (64+37*[Math]::Cos($angle)) (64+37*[Math]::Sin($angle))}
            Line $g '#a64d31' 4 64 64 83 37;Oval $g '#323d3b' 58 58 12 12
        }
    }
}
foreach($name in @('tin_ore','deepslate_tin_ore')){
    Texture $name 'block' {param($g)
        Rect $g $(if($name -eq 'tin_ore'){'#7c807c'}else{'#414b4f'}) 0 0 128 128
        $rng=[Random]::new(158)
        for($i=0;$i -lt 500;$i++){Rect $g $(if($i%2){'#656e70'}else{'#969d9a'}) ($rng.Next(128)) ($rng.Next(128)) 3 2}
        foreach($point in @(@(16,21),@(76,13),@(95,78),@(35,88),@(59,52),@(7,59))){
            Rect $g '#3b4c4d' $point[0] $point[1] 21 16;Rect $g '#a8c3bc' ($point[0]+2) ($point[1]+2) 16 10;Rect $g '#e1e8d7' ($point[0]+3) ($point[1]+2) 9 4
        }
    }
}
foreach($name in @('raw_tin','tin_ingot','bronze_ingot','steel_ingot','steel_plate','copper_wire','steel_gear','basic_circuit','insulated_plate','refined_voltaite','advanced_circuit','mineral_matrix','sawdust','iron_dust','copper_dust','gold_dust','tin_dust')){
    Texture $name 'item' {param($g)
        $color=switch -Wildcard($name){'*bronze*'{'#c48a47'} '*copper*'{'#d58b50'} '*gold*'{'#dfc35d'} '*voltaite*'{'#b47ee4'} '*tin*'{'#b8cdc4'} 'sawdust'{'#b0925b'} default{'#9eabb0'}}
        if($name -like '*ingot'){
            Rect $g '#263435' 17 40 93 46;Rect $g $color 21 36 85 43;Rect $g '#d5ded3' 28 37 69 8;Rect $g '#657674' 21 71 85 9
        }elseif($name -like '*plate'){
            Rect $g '#283239' 20 24 88 83;Rect $g $color 25 20 78 80;Line $g '#e8e5c7' 3 28 24 99 24
            if($name -eq 'insulated_plate'){Rect $g '#a1844e' 35 34 58 54;Rect $g '#685333' 42 40 44 42}
        }elseif($name -like '*circuit'){
            Rect $g '#263330' 20 20 88 88;Rect $g $(if($name -eq 'basic_circuit'){'#427761'}else{'#634586'}) 25 25 78 78
            foreach($y in @(36,58,81)){Line $g '#dfc678' 5 26 $y 99 $y}Rect $g '#2d3535' 46 43 34 36
            for($y=44;$y -lt 82;$y+=9){Line $g '#b1b9af' 3 40 $y 84 $y}Rect $g '#44444b' 50 46 26 28
        }elseif($name -eq 'copper_wire'){
            foreach($x in @(26,38,50,62,74)){Oval $g '#763e2c' $x 21 28 80;Oval $g '#e0a063' ($x+2) 23 22 74;Oval $g '#243333' ($x+7) 29 10 62}
        }elseif($name -eq 'steel_gear'){
            foreach($angle in @(0,45,90,135,180,225,270,315)){$a=$angle*[Math]::PI/180;Rect $g '#8d9d9d' (54+38*[Math]::Cos($a)) (54+38*[Math]::Sin($a)) 20 20}
            Oval $g '#9eafb0' 27 27 74 74;Oval $g '#4f6062' 41 41 46 46;Oval $g '#d4dcd4' 51 51 26 26;Oval $g '#273237' 57 57 14 14
        }elseif($name -in @('refined_voltaite','mineral_matrix')){
            Rect $g '#38303e' 22 25 84 80;Rect $g $(if($name -eq 'refined_voltaite'){'#9862c4'}else{'#898874'}) 29 20 70 76
            foreach($x in @(37,56,75)){Line $g '#d4c3e0' 4 $x 31 ($x+10) 83}
        }else{
            $rng=[Random]::new(74)
            for($i=0;$i -lt 28;$i++){$x=$rng.Next(22,95);$y=$rng.Next(37,100);Oval $g '#39423b' ($x-2) ($y-2) 16 12;Oval $g $color $x $y 12 8}
        }
    }
}
python (Join-Path $PSScriptRoot 'generate_industry.py')
if($LASTEXITCODE -ne 0){throw 'Industrial resource generation failed'}
