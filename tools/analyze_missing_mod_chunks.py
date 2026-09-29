"""Audit snc_energies blocks inside an Anvil save, with loss detection against a reference save.

Parses region files (standard Anvil format: sector table + zlib/gzip chunk NBT)
with the Python standard library only — no game, no third-party packages, safe
to run against live saves because it never writes anything.

For every chunk it extracts:
  - last-save timestamp (from the MCA header, epoch seconds -> local time)
  - every palette entry whose id starts with `snc_energies:` plus per-block
    counts (BlockStates bit-unpacking, post-1.16 packing)
  - block entities with ids starting with `snc_energies:`

With `--reference <save folder>` it also diffs against that save and classifies:
  - LOST IN WINDOW       chunk had mod blocks in the reference, has none now and
                         was last saved inside the no-jar session window
  - PARTIAL LOSS         still has mod blocks but fewer than the reference
  - LOST OUTSIDE WINDOW  mod content disappeared but the chunk was NOT written
                         inside the window (needs owner memory to attribute)
  - REWRITTEN IN WINDOW  chunk saved during the no-jar session (at-risk audit,
                         regardless of current mod content)
  - ADDED SINCE REF      chunk gained mod blocks (normal building progress)

Defaults target the 2026-09-26 incident: save "New World (4)" and window
18:15-18:30 local (-03:00). Save (4) was CREATED on 25/09 ~15:58 (not forked
from any other save), so there is no valid in-family reference save: chunk
byte-comparisons against (3)/(2) match 0 frozen chunks. The window-based
classification below is the primary evidence; pass --reference only when the
two saves genuinely share history.

Run:  python tools/analyze_missing_mod_chunks.py [--save "New World (4)"]
                                                  [--reference ""]
                                                  [--window "2026-09-26T18:15:00-03:00,2026-09-26T18:30:00-03:00"]
Exit code 0 always (audit tool); the JSON report is the deliverable.
"""
from __future__ import annotations

import argparse
import gzip
import json
import struct
import sys
import zlib
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]           # snc_energies repo
DEFAULT_INSTANCE = ROOT.parents[1] / "SNC energies"  # Instances/SNC energies
REPORT = ROOT / "verification" / "missing-mod-chunks-20260926.json"
MOD_PREFIX = "snc_energies:"
DEFAULT_WINDOW = "2026-09-26T18:15:00-03:00,2026-09-26T18:30:00-03:00"

# ---------------------------------------------------------------- NBT reader


class NbtReader:
    """Minimal big-endian NBT parser returning plain Python structures."""

    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def _u1(self) -> int:
        value = self.data[self.pos]
        self.pos += 1
        return value

    def _i2(self) -> int:
        value = struct.unpack_from(">h", self.data, self.pos)[0]
        self.pos += 2
        return value

    def _i4(self) -> int:
        value = struct.unpack_from(">i", self.data, self.pos)[0]
        self.pos += 4
        return value

    def _i8(self) -> int:
        value = struct.unpack_from(">q", self.data, self.pos)[0]
        self.pos += 8
        return value

    def _f8(self) -> float:
        value = struct.unpack_from(">d", self.data, self.pos)[0]
        self.pos += 8
        return value

    def _string(self) -> str:
        length = struct.unpack_from(">H", self.data, self.pos)[0]
        self.pos += 2
        raw = self.data[self.pos:self.pos + length]
        self.pos += length
        return raw.decode("utf-8", errors="replace")

    def payload(self, tag: int):
        if tag == 1:
            value = struct.unpack_from(">b", self.data, self.pos)[0]
            self.pos += 1
            return value
        if tag == 2:
            return self._i2()
        if tag == 3:
            return self._i4()
        if tag == 4:
            return self._i8()
        if tag == 5:
            value = struct.unpack_from(">f", self.data, self.pos)[0]
            self.pos += 4
            return value
        if tag == 6:
            return self._f8()
        if tag == 7:
            length = self._i4()
            value = self.data[self.pos:self.pos + length]
            self.pos += length
            return value
        if tag == 8:
            return self._string()
        if tag == 9:
            inner = self._u1()
            length = self._i4()
            return [self.payload(inner) for _ in range(length)]
        if tag == 10:
            compound = {}
            while True:
                child = self._u1()
                if child == 0:
                    return compound
                # key first: in `d[f()] = g()` Python evaluates the VALUE (g)
                # before the KEY (f), which desynchronizes the stream
                key = self._string()
                compound[key] = self.payload(child)
        if tag == 11:
            length = self._i4()
            value = list(struct.unpack_from(f">{length}i", self.data, self.pos))
            self.pos += 4 * length
            return value
        if tag == 12:
            length = self._i4()
            value = list(struct.unpack_from(f">{length}q", self.data, self.pos))
            self.pos += 8 * length
            return value
        raise ValueError(f"unsupported NBT tag {tag}")

    def parse_root(self) -> dict:
        tag = self._u1()
        if tag != 10:
            raise ValueError("root is not a compound")
        self._string()  # root name (empty in region files)
        return self.payload(10)


# ------------------------------------------------------------- region format


def parse_region(path: Path):
    """Yield (cx, cz, timestamp, root_nbt) for every stored chunk."""
    data = path.read_bytes()
    if len(data) < 8192:
        return
    for index in range(1024):
        entry = struct.unpack_from(">I", data, index * 4)[0]
        sectors = entry & 0xFF
        if entry == 0 or sectors == 0:
            continue
        offset = (entry >> 8) * 4096
        timestamp = struct.unpack_from(">I", data, 4096 + index * 4)[0]
        if offset + 5 > len(data):
            continue
        try:
            length = struct.unpack_from(">I", data, offset)[0]
            if length <= 1:
                continue
            compression = data[offset + 4]
            payload = data[offset + 5:offset + 4 + length]
            if compression == 1:
                payload = gzip.decompress(payload)
            elif compression == 2:
                payload = zlib.decompress(payload)
            elif compression != 3:
                raise ValueError(f"unknown compression {compression}")
            root = NbtReader(payload).parse_root()
        except Exception as error:  # keep auditing even if one chunk is corrupt
            raise RuntimeError(f"chunk #{index} em {path.name}: {error}") from error
        cx = index % 32
        cz = index // 32
        yield cx, cz, timestamp, root


# ------------------------------------------------------------ chunk analysis


def unpack_counts(longs: list[int], palette_size: int) -> list[int]:
    """Count palette indices in a post-1.16 BlockStates long array."""
    if palette_size <= 1:
        return [4096]
    bits = max(4, (palette_size - 1).bit_length())
    per_long = 64 // bits
    mask = (1 << bits) - 1
    counts = [0] * palette_size
    for value in longs:
        value &= (1 << 64) - 1
        for slot in range(per_long):
            index = (value >> (slot * bits)) & mask
            if index < palette_size:
                counts[index] += 1
    return counts


def palette_id(entry) -> str:
    """Block id from a palette entry.

    Minecraft 26.3 writes palette entries either as plain strings or as a
    compound whose single field has an EMPTY name carrying the id
    ({'': 'minecraft:stone'}); older-style compounds use the 'Name' key.
    """
    if isinstance(entry, str):
        return entry
    if isinstance(entry, dict):
        if isinstance(entry.get("Name"), str):
            return entry["Name"]
        anonymous = entry.get("")
        if isinstance(anonymous, str):
            return anonymous
    return ""


def chunk_mod_content(root: dict) -> tuple[dict[str, int], dict[str, int]]:
    """(block id -> count, block entity id -> count) for snc_energies content."""
    blocks: dict[str, int] = {}
    for section in root.get("sections") or []:
        states = section.get("block_states") if isinstance(section, dict) else None
        palette = (states or {}).get("palette") or []
        names = [palette_id(entry) for entry in palette]
        hits = [i for i, name in enumerate(names) if name.startswith(MOD_PREFIX)]
        if not hits:
            continue
        if "data" in (states or {}):
            counts = unpack_counts(states["data"], len(palette))
            for i in hits:
                if counts[i]:
                    blocks[names[i]] = blocks.get(names[i], 0) + counts[i]
        else:
            # no data array: the whole section is palette[0]
            if 0 in hits:
                blocks[names[0]] = blocks.get(names[0], 0) + 4096
    entities: dict[str, int] = {}
    for entity in root.get("block_entities") or []:
        identifier = entity.get("id", "") if isinstance(entity, dict) else ""
        if identifier.startswith(MOD_PREFIX):
            entities[identifier] = entities.get(identifier, 0) + 1
    return blocks, entities


def scan_save(save_dir: Path) -> tuple[dict, list[str]]:
    """Scan every region file of the save; returns {chunk key: info}."""
    chunks: dict[str, dict] = {}
    errors: list[str] = []
    region_dirs = sorted(set(save_dir.glob("dimensions/*/region")) |
                         set(save_dir.glob("dimensions/*/*/region")) |
                         {save_dir / "region"} if (save_dir / "region").is_dir() else
                         set(save_dir.glob("dimensions/*/region")) |
                         set(save_dir.glob("dimensions/*/*/region")))
    for region_dir in region_dirs:
        for region_file in sorted(region_dir.glob("r.*.*.mca")):
            for cx, cz, timestamp, root in parse_region(region_file):
                try:
                    blocks, entities = chunk_mod_content(root)
                except Exception as error:
                    errors.append(f"{region_file.name} chunk {cx},{cz}: {error}")
                    continue
                key = f"{root.get('xPos', cx)},{root.get('zPos', cz)}"
                chunks[key] = {
                    "blocks": blocks,
                    "block_entities": entities,
                    "last_saved": timestamp,
                    "region": region_file.name,
                    "inhabited_time": root.get("InhabitedTime", 0),
                }
    return chunks, errors


# ----------------------------------------------------------------- reporting


def to_local(epoch: int) -> str:
    return datetime.fromtimestamp(epoch).astimezone().isoformat(timespec="seconds")


def in_window(epoch: int, window) -> bool:
    if window is None:
        return False
    return window[0].timestamp() <= epoch <= window[1].timestamp()


def total_blocks(chunks: dict) -> int:
    return sum(sum(c["blocks"].values()) for c in chunks.values())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--instance", type=Path, default=DEFAULT_INSTANCE)
    parser.add_argument("--save", default="New World (4)")
    parser.add_argument("--reference", default="",
                        help="save de referência (apenas se compartilhar histórico com --save)")
    parser.add_argument("--window", default=DEFAULT_WINDOW,
                        help="ISO local times 'start,end' of the no-jar session; empty string disables")
    args = parser.parse_args()

    save_dir = args.instance / "saves" / args.save
    if not save_dir.is_dir():
        print(f"ERRO: save não encontrado: {save_dir}")
        return 1
    window = None
    if args.window:
        start_s, end_s = args.window.split(",")
        window = (datetime.fromisoformat(start_s), datetime.fromisoformat(end_s))

    print(f"=== Auditoria de blocos snc_energies — save '{args.save}' ===")
    print(f"Lendo region files de {save_dir} ...")
    current, errors = scan_save(save_dir)
    print(f"  {len(current)} chunks lidos, {total_blocks(current)} blocos do mod no momento, "
          f"{len(errors)} erros de leitura")

    reference, ref_errors = {}, []
    ref_dir = None
    if args.reference:
        ref_dir = args.instance / "saves" / args.reference
        if ref_dir.is_dir():
            print(f"Referência: save '{args.reference}' ({ref_dir})")
            reference, ref_errors = scan_save(ref_dir)
            print(f"  {len(reference)} chunks lidos, {total_blocks(reference)} blocos do mod na referência, "
                  f"{len(ref_errors)} erros de leitura")
        else:
            print(f"AVISO: referência não encontrada, análise sem comparação: {ref_dir}")

    lost_in_window, partial, lost_outside, rewritten, added = [], [], [], [], []
    for key, info in current.items():
        had = reference.get(key)
        written_in_window = in_window(info["last_saved"], window)
        if written_in_window:
            rewritten.append(key)
        if had is None:
            if info["blocks"] or info["block_entities"]:
                added.append(key)
            continue
        ref_blocks = sum(had["blocks"].values())
        now_blocks = sum(info["blocks"].values())
        if ref_blocks and not now_blocks:
            lost_ids = sorted(had["blocks"])
            entry = {"chunk": key, "region": info["region"],
                     "last_saved": to_local(info["last_saved"]),
                     "blocks_in_reference": had["blocks"],
                     "block_entities_in_reference": had["block_entities"],
                     "reference_saved": to_local(had["last_saved"])}
            if written_in_window:
                lost_in_window.append(entry)
            else:
                lost_outside.append(entry)
        elif ref_blocks and now_blocks < ref_blocks:
            partial.append({
                "chunk": key, "region": info["region"],
                "last_saved": to_local(info["last_saved"]),
                "blocks_now": info["blocks"],
                "blocks_in_reference": had["blocks"],
                "removed": sorted(set(had["blocks"]) - set(info["blocks"])),
                "written_in_window": written_in_window,
            })

    print()
    print(f"Blocos do mod hoje: {total_blocks(current)} em "
          f"{sum(1 for c in current.values() if c['blocks'])} chunks")
    if reference:
        print(f"Blocos do mod na referência: {total_blocks(reference)} em "
              f"{sum(1 for c in reference.values() if c['blocks'])} chunks")
    print()
    print(f"PERDA NA SESSÃO SEM JAR (janela): {len(lost_in_window)} chunks")
    for entry in lost_in_window:
        machines = ", ".join(f"{k}×{v}" for k, v in sorted(entry["block_entities_in_reference"].items()))
        blocks = ", ".join(f"{k}×{v}" for k, v in sorted(entry["blocks_in_reference"].items()))
        print(f"  chunk {entry['chunk']} ({entry['region']}) salvo {entry['last_saved']}")
        print(f"    tinha: {blocks or '(só block entities)'}{'; BE: ' + machines if machines else ''}")
    print(f"PERDA FORA DA JANELA (atribuição manual): {len(lost_outside)} chunks")
    for entry in lost_outside:
        print(f"  chunk {entry['chunk']} ({entry['region']}) salvo {entry['last_saved']} "
              f"tinha {entry['blocks_in_reference']}")
    print(f"PERDA PARCIAL: {len(partial)} chunks")
    for entry in partial:
        print(f"  chunk {entry['chunk']} ({entry['region']}) removidos: {entry['removed']}")
    print(f"RESSALVOS REESCRITAS NA JANELA (auditoria): {len(rewritten)} chunks "
          f"({', '.join(sorted(rewritten)) or 'nenhuma'})")
    print(f"CHUNKS NOVOS COM CONTEÚDO DO MOD (pós-referência): {len(added)}")

    report = {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "save": args.save,
        "reference": args.reference if ref_dir and ref_dir.is_dir() else None,
        "window": {"start": window[0].isoformat(), "end": window[1].isoformat()} if window else None,
        "summary": {
            "chunks_scanned": len(current),
            "mod_blocks_now": total_blocks(current),
            "mod_blocks_reference": total_blocks(reference),
            "chunks_with_mod_blocks_now": sum(1 for c in current.values() if c["blocks"]),
            "chunks_with_mod_block_entities_now": sum(1 for c in current.values() if c["block_entities"]),
            "lost_in_window": len(lost_in_window),
            "lost_outside_window": len(lost_outside),
            "partial_loss": len(partial),
            "rewritten_in_window": sorted(rewritten),
            "added_since_reference": len(added),
        },
        "lost_in_window": lost_in_window,
        "lost_outside_window": lost_outside,
        "partial_loss": partial,
        "chunks_now": {k: {"blocks": c["blocks"], "block_entities": c["block_entities"],
                           "last_saved": to_local(c["last_saved"]), "region": c["region"]}
                       for k, c in sorted(current.items()) if c["blocks"] or c["block_entities"]},
        "errors": errors + ref_errors,
    }
    REPORT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"\nRelatório: {REPORT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
