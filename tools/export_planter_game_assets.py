"""Package the approved SNC 75-P planter assets without altering their geometry.

This copies the complete authored rig and its fifteen 128x128 textures to the
game resource pack. The client bakes the cuboids once when resources load.
Follows tools/export_tractor_game_assets.py.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets/planter"
TARGET = ROOT / "src/main/resources/assets/snc_energies"


def main():
    model = json.loads((SOURCE / "planter-model.json").read_text(encoding="utf-8"))
    materials = json.loads((SOURCE / "materials.json").read_text(encoding="utf-8"))
    assert len(model["cubes"]) == 371 and len(model["groups"]) == 7, (len(model["cubes"]), len(model["groups"]))
    assert model.get("cube_count") == 371 and model.get("group_count") == 7
    assert len(materials) == 15 and model["units_per_block"] == 16
    paths = [(SOURCE / "planter-model.json", TARGET / "vehicle/planter-model.json"),
             (SOURCE / "materials.json", TARGET / "vehicle/planter-materials.json")]
    for name, material in materials.items():
        source = SOURCE / material["file"]
        data = source.read_bytes()
        assert data[:8] == b"\x89PNG\r\n\x1a\n"
        assert struct.unpack(">II", data[16:24]) == (128, 128), name
        paths.append((source, TARGET / "textures/entity/planter" / (name + ".png")))
    hashes = {}
    for source, target in paths:
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        assert source.read_bytes() == target.read_bytes(), str(target)
        hashes[str(target.relative_to(ROOT)).replace("\\", "/")] = hashlib.sha256(target.read_bytes()).hexdigest()
    manifest = {"source": "approved SNC 75-P planter assets", "cubes": len(model["cubes"]),
                "groups": len(model["groups"]), "materials": len(materials),
                "texture_dimensions": [128, 128], "source_bytes_preserved": True,
                "files": hashes}
    (ROOT / "verification/planter-game-assets.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Packaged planter: {len(model['cubes'])} cubes, {len(materials)} textures, {len(hashes)} files hashed.")


if __name__ == "__main__":
    main()
