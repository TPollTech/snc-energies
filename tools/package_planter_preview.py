"""Create a self-contained SNC 75-P preview that opens directly from the filesystem.

Run from the project root: .venv-textures/Scripts/python.exe tools/package_planter_preview.py
The regular previews/plantadeira.html remains the development version served over HTTP.
Follows the package_harvester_preview.py pattern: data-URI import map, embedded
model/materials JSON, embedded downloads and Three.js license.
"""

import base64
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
PREVIEWS = ROOT / "previews"
ASSETS = ROOT / "assets" / "planter"


def data_uri(path, mime):
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:{mime};base64,{encoded}"


def embedded_json(element_id, value):
    # Escape '<' so names or other JSON text can never terminate the script element.
    content = json.dumps(value, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c")
    return f'<script type="application/json" id="{element_id}">{content}</script>'


def build():
    html = (PREVIEWS / "plantadeira.html").read_text(encoding="utf-8")
    import_map = {
        "imports": {
            "three": data_uri(PREVIEWS / "vendor" / "three.module.js", "text/javascript"),
            "three-controls": data_uri(PREVIEWS / "vendor" / "OrbitControls.js", "text/javascript"),
        }
    }
    html, replacements = re.subn(
        r'<script type="importmap">.*?</script>',
        '<script type="importmap">' + json.dumps(import_map, separators=(",", ":")) + "</script>",
        html,
        count=1,
        flags=re.DOTALL,
    )
    if replacements != 1:
        raise ValueError("Expected one import map in previews/plantadeira.html")

    css = (PREVIEWS / "vehicle-studio.css").read_text(encoding="utf-8")
    html, replacements = re.subn(
        r'<link rel="stylesheet" href="./vehicle-studio.css">',
        "<style>\n" + css + "\n</style>",
        html,
        count=1,
    )
    if replacements != 1:
        raise ValueError("Expected one stylesheet link in previews/plantadeira.html")

    model = json.loads((ASSETS / "planter-model.json").read_text(encoding="utf-8"))
    manifest = json.loads((ASSETS / "materials.json").read_text(encoding="utf-8"))
    for material in manifest.get("materials", manifest).values():
        if isinstance(material, dict) and material.get("file"):
            material["file"] = data_uri(ASSETS / material["file"], "image/png")

    for filename, mime in (
        ("snc-75-p-plantadeira.bbmodel", "application/json"),
        ("snc-75-p-plantadeira.glb", "model/gltf-binary"),
    ):
        original = f'href="../assets/planter/{filename}" download'
        if original not in html:
            raise ValueError(f"Missing download link: {filename}")
        html = html.replace(original, f'href="{data_uri(ASSETS / filename, mime)}" download="{filename}"', 1)

    viewer = (PREVIEWS / "planter-viewer.js").read_text(encoding="utf-8")
    studio = data_uri(PREVIEWS / "vehicle-studio.js", "text/javascript")
    viewer = viewer.replace("from './vehicle-studio.js'", f"from '{studio}'", 1)
    viewer = re.sub(r"</script", r"<\\/script", viewer, flags=re.IGNORECASE)
    license_text = (PREVIEWS / "vendor" / "THREE-LICENSE.txt").read_text(encoding="utf-8")
    inline = "\n".join((
        '<script type="text/plain" id="three-license">' + license_text + "</script>",
        embedded_json("planter-model-data", model),
        embedded_json("planter-materials-data", manifest),
        '<script type="module">\n' + viewer + "\n</script>",
    ))
    original = '<script type="module" src="./planter-viewer.js"></script>'
    if original not in html:
        raise ValueError("Missing viewer script in previews/plantadeira.html")
    html = html.replace(original, inline, 1)

    output = PREVIEWS / "plantadeira-offline.html"
    output.write_text(html, encoding="utf-8", newline="\n")
    print(f"Created {output}")
    print(f"{output.stat().st_size:,} bytes ({output.stat().st_size / 1024 / 1024:.2f} MiB)")


if __name__ == "__main__":
    build()
