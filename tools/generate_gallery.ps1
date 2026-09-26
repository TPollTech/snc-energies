# Generates a self-contained HTML gallery of all SNC Energies textures (base64 embedded).
# Rule from AGENTS.md: show a preview instead of building when reviewing visual changes.
$ErrorActionPreference = "Stop"

$toolsDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projDir = Split-Path -Parent $toolsDir
$textures = Join-Path $projDir "src/main/resources/assets/snc_energies/textures"
$outPath = Join-Path (Split-Path -Parent $projDir) "gallery.html"

function Add-Category($title, $subdir, $size) {
	$dir = Join-Path $textures $subdir
	if (-not (Test-Path $dir)) { return "" }
	$sb = New-Object System.Text.StringBuilder
	[void]$sb.Append("<h2>$title</h2><div class='grid'>")
	Get-ChildItem $dir -Filter *.png | Sort-Object Name | ForEach-Object {
		$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($_.FullName))
		$name = $_.BaseName
		[void]$sb.Append("<figure style='width:${size}px'><img src='data:image/png;base64,$b64' alt='$name'><figcaption>$name</figcaption></figure>")
	}
	[void]$sb.Append("</div>")
	return $sb.ToString()
}

$css = @"
<style>
body{background:#1b1c22;color:#e8e4da;font-family:'Segoe UI',sans-serif;margin:24px}
h1{color:#40dcff}
h2{color:#ffca5c;margin-top:32px;border-bottom:1px solid #33363f;padding-bottom:4px}
.grid{display:flex;flex-wrap:wrap;gap:18px}
figure{margin:0;text-align:center;background:#24262e;padding:12px;border-radius:10px;border:1px solid #33363f}
figcaption{font-size:12px;color:#a0a4ac;margin-top:8px;font-family:Consolas,monospace}
img{width:100%;image-rendering:pixelated;background:#3a3d47;border-radius:4px}
</style>
"@

$sb = New-Object System.Text.StringBuilder
[void]$sb.Append("<!DOCTYPE html><html lang='pt-br'><head><meta charset='utf-8'><title>SNC Energies - Galeria de Texturas</title>$css</head><body>")
[void]$sb.Append("<h1>SNC Energies - Galeria de Texturas (v0.1.0)</h1>")
[void]$sb.Append("<p>Blocos, itens e GUIs em <b>128x128</b> (GUIs em 176x166, excecao documentada no AGENTS.md).</p>")
[void]$sb.Append((Add-Category "Texturas de blocos" "block" 128))
[void]$sb.Append((Add-Category "Itens" "item" 128))
[void]$sb.Append((Add-Category "GUIs das maquinas" "gui" 352))
[void]$sb.Append("</body></html>")

[IO.File]::WriteAllText($outPath, $sb.ToString(), [Text.Encoding]::UTF8)
Write-Host "Gallery written to $outPath"
