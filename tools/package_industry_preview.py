"""Embed model/materials JSON into the compactor and silo studio previews.

Run from the project root: python tools/package_industry_preview.py
Both previews/compactadora.html and previews/silo.html (and their -offline
variants) get <script type="application/json"> data blocks, so the pages work
served over HTTP and opened directly from the filesystem. The viewer's
loadEmbeddedOrFetch finds the embedded ids first, exactly like mercadao-offline.
"""

import base64
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PREVIEWS = ROOT / "previews"
ASSETS = ROOT / "assets"

PAGES = {
    "compactadora": ("compactor-model-data", "compactor-materials-data", "compactor"),
    "silo": ("silo-model-data", "silo-materials-data", "silo"),
}

# Clean templates: the packer inlines CSS/importmap/data into the served page,
# so the source HTML must stay untouched. Git carries the clean sources.
TEMPLATE_SUFFIX = ".template.html"


def data_uri(path, mime):
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:{mime};base64,{encoded}"


def embedded_json(element_id, value):
    content = json.dumps(value, ensure_ascii=False, separators=(",", ":")).replace("<", "\\u003c")
    return f'<script type="application/json" id="{element_id}">{content}</script>'


def build(page, model_id, materials_id, key, output=None):
    template = (PREVIEWS / f"{page}{TEMPLATE_SUFFIX}").read_text(encoding="utf-8")
    import_map = {
        "imports": {
            "three": data_uri(PREVIEWS / "vendor" / "three.module.js", "text/javascript"),
            "three-controls": data_uri(PREVIEWS / "vendor" / "OrbitControls.js", "text/javascript"),
        }
    }
    html, replacements = re.subn(
        r'<script type="importmap">.*?</script>',
        '<script type="importmap">' + json.dumps(import_map, separators=(",", ":")) + "</script>",
        template,
        count=1,
        flags=re.DOTALL,
    )
    if replacements != 1:
        raise ValueError(f"Expected one import map in {page}.template.html")

    css = (PREVIEWS / "vehicle-studio.css").read_text(encoding="utf-8")
    html, replacements = re.subn(
        r'<link rel="stylesheet" href="./vehicle-studio.css">',
        "<style>\n" + css + "\n</style>",
        html,
        count=1,
    )
    if replacements != 1:
        raise ValueError(f"Expected one stylesheet link in {page}.template.html")

    models = json.loads((PREVIEWS / "industry-models.json").read_text(encoding="utf-8"))
    model = models[key]
    manifest = json.loads((ASSETS / "industry-materials.json").read_text(encoding="utf-8"))

    viewer = (PREVIEWS / f"{page}-viewer.js").read_text(encoding="utf-8")
    studio = data_uri(PREVIEWS / "vehicle-studio.js", "text/javascript")
    viewer = viewer.replace("from './vehicle-studio.js'", f"from '{studio}'", 1)
    viewer = viewer.replace(f"['../previews/industry-models.json']", f"['{model_id}']")
    viewer = viewer.replace("['../assets/industry-materials.json']", f"['{materials_id}']")
    viewer = re.sub(r"</script", r"<\\/script", viewer, flags=re.IGNORECASE)
    license_text = (PREVIEWS / "vendor" / "THREE-LICENSE.txt").read_text(encoding="utf-8")
    data_blocks = (
        embedded_json(model_id, model)
        + "\n"
        + embedded_json(materials_id, manifest)
    )
    # Development page: keep the module src, embed only the JSON data blocks.
    dev_anchor = f'<script type="module" src="./{page}-viewer.js"></script>'
    if dev_anchor not in html:
        raise ValueError(f"Missing viewer script in {page}.template.html")
    html = html.replace(dev_anchor, data_blocks + "\n" + dev_anchor, 1)
    (PREVIEWS / f"{page}.html").write_text(html, encoding="utf-8", newline="\n")

    if output:
        inline = "\n".join((
            '<script type="text/plain" id="three-license">' + license_text + "</script>",
            data_blocks,
            '<script type="module">\n' + viewer + "\n</script>",
        ))
        html = template.replace(dev_anchor, inline, 1)
        output.write_text(html, encoding="utf-8", newline="\n")
        print(f"Created {output.name}: {output.stat().st_size:,} bytes")


if __name__ == "__main__":
    for page, (model_id, materials_id, key) in PAGES.items():
        build(page, model_id, materials_id, key, output=PREVIEWS / f"{page}-offline.html")
