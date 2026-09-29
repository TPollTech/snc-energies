"""Pre-launch safety check for the SNC energies instance mods folder.

Validates every jar in the instance `mods/` folder BEFORE the game is opened:
  1. The file is a readable ZIP archive with intact CRCs (full decompression test).
  2. `fabric.mod.json` exists and parses (mod id + version reported).
  3. For snc-energies: SHA-256 is matched against the installation manifests in
     `snc_energies/verification/installation-*.json` (and release notes). A jar
     whose hash is unknown to every manifest is flagged as an error.
  4. Exactly one snc-energies jar must be present (golden rule: single jar).
  5. Warns if a Java process is still running (never touch mods with the game open).

Run from the repo root:  python tools/verify_mods_install.py
Exit code 0 = safe to open the game. Exit code 1 = fix the errors above first.

This script is stdlib-only on purpose: it must run on a clean machine before
launching Minecraft.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]           # snc_energies repo
DEFAULT_INSTANCE = ROOT.parents[1] / "SNC energies"  # Instances/SNC energies
VERIFY_DIR = ROOT / "verification"
REPORT = VERIFY_DIR / "mods-install-check.json"

KNOWN_FILE_HINTS = {
    "snc-energies": "SNC Energies (marco próprio)",
    "intoxicantes": "SNC Adventures (terceiro — integridade zip apenas)",
    "fabric-api": "Fabric API (dependência — integridade zip apenas)",
}


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def known_hashes() -> tuple[dict[str, dict], str | None]:
    """Collect every SHA-256 recorded in verification manifests and release notes.

    Returns (hash -> info map, sha expected by the most recent manifest or None).
    """
    found: dict[str, dict] = {}
    staged: list[dict] = []
    for manifest in sorted(VERIFY_DIR.glob("installation-*.json")):
        try:
            data = json.loads(manifest.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        installed = data.get("installed") or ""
        version = data.get("version") or manifest.stem
        for key in ("sha256", "sha256_local_install"):
            value = data.get(key)
            if isinstance(value, str) and re.fullmatch(r"[A-Fa-f0-9]{64}", value.strip()):
                staged.append({
                    "sha": value.strip().upper(),
                    "version": str(version),
                    "source": manifest.name,
                    "installed": installed,
                })
    for release in sorted(VERIFY_DIR.glob("release-*.md")):
        try:
            text = release.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for line in text.splitlines():
            if "SHA-256" not in line:
                continue
            match = re.search(r"[A-Fa-f0-9]{64}", line)
            if match:
                version = release.stem.replace("release-", "")
                staged.append({
                    "sha": match.group(0).upper(),
                    "version": version + " (build CI)",
                    "source": release.name,
                    "installed": "",
                })
    for entry in staged:
        found.setdefault(entry["sha"], entry)
    expected = None
    with_time = [e for e in staged if e["installed"]]
    if with_time:
        expected = max(with_time, key=lambda e: e["installed"])["sha"]
    return found, expected


def fabric_meta(jar: Path) -> tuple[str | None, str | None, str | None]:
    """Return (mod id, version, error) from fabric.mod.json inside the jar."""
    try:
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
            if "fabric.mod.json" not in names:
                return None, None, "sem fabric.mod.json dentro do jar"
            raw = archive.read("fabric.mod.json")
        data = json.loads(raw.decode("utf-8", errors="replace"))
        return data.get("id"), data.get("version"), None
    except (zipfile.BadZipFile, json.JSONDecodeError) as error:
        return None, None, f"fabric.mod.json ilegível: {error}"


def zip_integrity(jar: Path) -> tuple[bool, str | None]:
    """Full CRC check of every entry (reads and decompresses the whole file)."""
    if jar.stat().st_size < 1024:
        return False, "arquivo menor que 1 KB (download/cópia truncada?)"
    try:
        with zipfile.ZipFile(jar) as archive:
            bad = archive.testzip()
            if bad is not None:
                return False, f"CRC inválido na entrada '{bad}'"
        return True, None
    except zipfile.BadZipFile as error:
        return False, f"ZIP inválido: {error}"
    except (OSError, EOFError) as error:
        return False, f"leitura falhou: {error}"


def game_running() -> list[str]:
    """Names of running Java processes (Windows); empty list elsewhere/unknown."""
    processes: list[str] = []
    for image in ("javaw.exe", "java.exe"):
        try:
            result = subprocess.run(
                ["tasklist", "/FI", f"IMAGENAME eq {image}", "/NH"],
                capture_output=True, text=True, timeout=15, check=False,
            )
        except (OSError, subprocess.SubprocessError):
            return processes
        output = result.stdout or ""
        if image.lower() in output.lower():
            processes.append(image)
    return processes


def classify(name: str) -> str:
    lowered = name.lower()
    for hint in KNOWN_FILE_HINTS:
        if lowered.startswith(hint):
            return hint
    return "other"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--instance", type=Path, default=DEFAULT_INSTANCE,
                        help="pasta da instância (default: Instances/SNC energies)")
    args = parser.parse_args()
    mods_dir = args.instance / "mods"
    instance_name = args.instance.name

    print(f"=== SNC Energies — verificação pré-abertura da pasta mods ===")
    print(f"Instância: {args.instance}")
    if not mods_dir.is_dir():
        print(f"ERRO: pasta mods não encontrada: {mods_dir}")
        return 1

    known, expected_sha = known_hashes()
    expected_version = known.get(expected_sha, {}).get("version") if expected_sha else None

    errors: list[str] = []
    warnings: list[str] = []
    entries: list[dict] = []

    jars = sorted(path for path in mods_dir.iterdir() if path.suffix.lower() == ".jar")
    if not jars:
        errors.append("nenhum .jar encontrado na pasta mods (instância vazia?)")

    snc_jars = [jar for jar in jars if classify(jar.name) == "snc-energies"]
    if len(snc_jars) > 1:
        errors.append(f"múltiplos jars SNC Energies em mods/ (regra: apenas um): {[j.name for j in snc_jars]}")

    running = game_running()
    if running:
        warnings.append(
            "processo Java ativo (" + ", ".join(running) + ") — NÃO instale/substitua/remova jars "
            "com o jogo aberto; feche o Minecraft antes de qualquer operação em mods/"
        )

    for jar in jars:
        kind = classify(jar.name)
        entry: dict = {"jar": jar.name, "kind": kind, "size_bytes": jar.stat().st_size}
        ok, reason = zip_integrity(jar)
        entry["zip_integrity"] = ok
        if not ok:
            errors.append(f"{jar.name}: {reason}")
            entries.append(entry)
            continue

        mod_id, mod_version, meta_error = fabric_meta(jar)
        entry["mod_id"] = mod_id
        entry["mod_version"] = mod_version
        if meta_error:
            errors.append(f"{jar.name}: {meta_error}")
        digest = sha256_of(jar)
        entry["sha256"] = digest

        if kind == "snc-energies":
            if expected_sha and digest == expected_sha:
                entry["manifest_status"] = "ok"
                print(f"  [OK]      {jar.name}: SHA-256 confere com o manifesto mais recente "
                      f"({expected_version})")
            elif digest in known:
                info = known[digest]
                entry["manifest_status"] = "stale"
                entry["manifest_version"] = info["version"]
                warnings.append(
                    f"{jar.name}: é o jar '{info['version']}' ({info['source']}), mas o manifesto "
                    f"mais recente registra outro jar — se a intenção era instalar o marco atual, "
                    f"reinstale e registre o novo hash"
                )
                print(f"  [ATENÇÃO] {jar.name}: jar anterior conhecido '{info['version']}' — "
                      f"manifesto mais recente espera {expected_version}")
            else:
                entry["manifest_status"] = "unknown"
                errors.append(
                    f"{jar.name}: SHA-256 {digest} não consta em nenhum manifesto de "
                    f"verification/installation-*.json — origem/versão não verificável"
                )
                print(f"  [ERRO]    {jar.name}: hash fora dos manifestos")
        else:
            label = KNOWN_FILE_HINTS.get(kind, "jar de origem desconhecida")
            print(f"  [OK]      {jar.name}: zip íntegro (id={mod_id}, versão={mod_version}) — {label}")
            if kind == "other":
                warnings.append(f"{jar.name}: jar não identificado no ambiente travado do projeto")
        entries.append(entry)

    print()
    for warning in warnings:
        print(f"ATENÇÃO: {warning}")
    for error in errors:
        print(f"ERRO: {error}")

    report = {
        "checked_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "instance": str(args.instance),
        "expected_manifest": {"sha256": expected_sha, "version": expected_version},
        "game_running": running,
        "jars": entries,
        "warnings": warnings,
        "errors": errors,
        "verdict": "safe-to-open" if not errors else "do-not-open",
    }
    try:
        REPORT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"\nRelatório: {REPORT.relative_to(ROOT)}")
    except OSError as error:
        print(f"\n(aviso: relatório não gravado: {error})")

    if errors:
        print("\n>>> NÃO ABRIR O JOGO — corrija os erros acima (regra 9 do AGENTS.md).")
        return 1
    print("\n>>> SEGURO ABRIR O JOGO — pasta mods íntegra e conferida contra os manifestos.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
