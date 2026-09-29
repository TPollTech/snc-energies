"""Create a self-contained Mercadão preview that opens directly from the filesystem.

Run from the project root: .venv-textures/Scripts/python.exe tools/package_mercadao_preview.py
The regular previews/mercadao.html remains the development version served over HTTP.
Follows the package_harvester_preview.py pattern: data-URI import map, embedded
model/materials JSON and the Three.js license.
"""

import base64
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PREVIEWS = ROOT / "previews"
ASSETS = ROOT / "assets"


def data_uri(path, mime):
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:{mime};base64,{encoded}"


def embedded_json(element_id, value):
    # Escape '<' so names or other JSON text can never terminate the script element.
    content = json.dumps(value, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c")
    return f'<script type="application/json" id="{element_id}">{content}</script>'


def build():
    html = (PREVIEWS / "mercadao.html").read_text(encoding="utf-8")
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
        raise ValueError("Expected one import map in previews/mercadao.html")

    css = (PREVIEWS / "vehicle-studio.css").read_text(encoding="utf-8")
    html, replacements = re.subn(
        r'<link rel="stylesheet" href="./vehicle-studio.css">',
        "<style>\n" + css + "\n</style>",
        html,
        count=1,
    )
    if replacements != 1:
        raise ValueError("Expected one stylesheet link in previews/mercadao.html")

    model = json.loads((PREVIEWS / "mercadao-model.json").read_text(encoding="utf-8"))
    manifest = json.loads((ASSETS / "mercadao-materials.json").read_text(encoding="utf-8"))
    for material in manifest.values():
        if isinstance(material, dict) and material.get("file"):
            material["file"] = data_uri(ASSETS / material["file"], "image/png")

    viewer = (PREVIEWS / "mercadao-viewer.js").read_text(encoding="utf-8")
    studio = data_uri(PREVIEWS / "vehicle-studio.js", "text/javascript")
    viewer = viewer.replace("from './vehicle-studio.js'", f"from '{studio}'", 1)
    viewer = viewer.replace("['../previews/mercadao-model.json']", "['mercadao-model-data']")
    viewer = viewer.replace("['../assets/mercadao-materials.json']", "['mercadao-materials-data']")
    viewer = re.sub(r"</script", r"<\/script", viewer, flags=re.IGNORECASE)
    license_text = (PREVIEWS / "vendor" / "THREE-LICENSE.txt").read_text(encoding="utf-8")
    inline = "\n".join((
        '<script type="text/plain" id="three-license">' + license_text + "</script>",
        embedded_json("mercadao-model-data", model),
        embedded_json("mercadao-materials-data", manifest),
        '<script type="module">\n' + viewer + "\n</script>",
    ))
    original = '<script type="module" src="./mercadao-viewer.js"></script>'
    if original not in html:
        raise ValueError("Missing viewer script in previews/mercadao.html")
    html = html.replace(original, inline, 1)

    output = PREVIEWS / "mercadao-offline.html"
    output.write_text(html, encoding="utf-8", newline="\n")
    print(f"Created {output}")
    print(f"{output.stat().st_size:,} bytes ({output.stat().st_size / 1024 / 1024:.2f} MiB)")


if __name__ == "__main__":
    build()
