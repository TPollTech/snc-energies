"""Create a self-contained tractor preview that opens directly from the filesystem.

Run from the project root: python tools/package_tractor_preview.py
The regular previews/trator.html remains the development version served over HTTP.
"""

import base64
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
PREVIEWS = ROOT / "previews"
ASSETS = ROOT / "assets" / "tractor"


def data_uri(path, mime):
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:{mime};base64,{encoded}"


def embedded_json(element_id, value):
    # Escape '<' so names or other JSON text can never terminate the script element.
    content = json.dumps(value, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c")
    return f'<script type="application/json" id="{element_id}">{content}</script>'


def build():
    html = (PREVIEWS / "trator.html").read_text(encoding="utf-8")
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
        raise ValueError("Expected one import map in previews/trator.html")

    model = json.loads((ASSETS / "tractor-model.json").read_text(encoding="utf-8"))
    manifest = json.loads((ASSETS / "materials.json").read_text(encoding="utf-8"))
    for material in manifest.get("materials", manifest).values():
        if isinstance(material, dict) and material.get("file"):
            material["file"] = data_uri(ASSETS / material["file"], "image/png")

    for filename, mime in (
        ("snc-75-trator.bbmodel", "application/json"),
        ("snc-75-trator.glb", "model/gltf-binary"),
    ):
        original = f'href="../assets/tractor/{filename}" download'
        if original not in html:
            raise ValueError(f"Missing download link: {filename}")
        html = html.replace(original, f'href="{data_uri(ASSETS / filename, mime)}" download="{filename}"', 1)

    viewer = (PREVIEWS / "tractor-viewer.js").read_text(encoding="utf-8")
    viewer = re.sub(r"</script", r"<\\/script", viewer, flags=re.IGNORECASE)
    license_text = (PREVIEWS / "vendor" / "THREE-LICENSE.txt").read_text(encoding="utf-8")
    inline = "\n".join((
        '<script type="text/plain" id="three-license">' + license_text + "</script>",
        embedded_json("tractor-model-data", model),
        embedded_json("tractor-materials-data", manifest),
        '<script type="module">\n' + viewer + "\n</script>",
    ))
    original = '<script type="module" src="./tractor-viewer.js"></script>'
    if original not in html:
        raise ValueError("Missing viewer script in previews/trator.html")
    html = html.replace(original, inline, 1)

    output = PREVIEWS / "trator-offline.html"
    output.write_text(html, encoding="utf-8", newline="\n")
    print(f"Created {output}")
    print(f"{output.stat().st_size:,} bytes ({output.stat().st_size / 1024 / 1024:.2f} MiB)")


if __name__ == "__main__":
    build()
