"""Package the approved SNC 90 harvester assets without altering their geometry.

This copies the complete authored rig (747 cubes / 83 groups) and its
twenty-one 128x128 textures to the game resource pack. VehicleRig loads
`vehicle/harvester-model.json` from the resource pack and resolves every
material under `textures/entity/harvester/<material>.png`; missing files
break resource reload and freeze world creation. Follows
tools/export_tractor_game_assets.py / export_planter_game_assets.py.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "assets/harvester"
TARGET = ROOT / "src/main/resources/assets/snc_energies"


def main():
    model = json.loads((SOURCE / "harvester-model.json").read_text(encoding="utf-8"))
    materials = json.loads((SOURCE / "materials.json").read_text(encoding="utf-8"))
    assert len(model["cubes"]) == 747 and len(model["groups"]) == 83, (len(model["cubes"]), len(model["groups"]))
    assert model["units_per_block"] == 16
    assert len(materials) == 21
    used = {cube["material"] for cube in model["cubes"]}
    assert used == set(materials), (used ^ set(materials))
    paths = [(SOURCE / "harvester-model.json", TARGET / "vehicle/harvester-model.json"),
             (SOURCE / "materials.json", TARGET / "vehicle/harvester-materials.json")]
    for name, material in materials.items():
        source = SOURCE / material["file"]
        data = source.read_bytes()
        assert data[:8] == b"\x89PNG\r\n\x1a\n", name
        assert struct.unpack(">II", data[16:24]) == (128, 128), name
        paths.append((source, TARGET / "textures/entity/harvester" / (name + ".png")))
    hashes = {}
    for source, target in paths:
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        assert source.read_bytes() == target.read_bytes(), str(target)
        hashes[str(target.relative_to(ROOT)).replace("\\", "/")] = hashlib.sha256(target.read_bytes()).hexdigest()
    manifest = {"source": "approved SNC 90 harvester assets", "cubes": len(model["cubes"]),
                "groups": len(model["groups"]), "materials": len(materials),
                "texture_dimensions": [128, 128], "source_bytes_preserved": True,
                "files": hashes}
    (ROOT / "verification/harvester-game-assets.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Packaged harvester: {len(model['cubes'])} cubes, {len(materials)} textures, {len(hashes)} files hashed.")


if __name__ == "__main__":
    main()
