#!/usr/bin/env python3
"""Adds the Mercadão lang entries to both SNC Energies lang files."""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "snc_energies", "lang")

PT = {
    "block.snc_energies.mercadao_shelf": "Balcão do Mercadão",
    "block.snc_energies.mercadao_anchor": "Alma do Mercadão",
    "item.snc_energies.mercajeiro_spawn_egg": "Ovo de Mercajeiro",
    "entity.snc_energies.mercajeiro": "Mercajeiro",
    "gui.snc_energies.panel.subtitle.mercadao": "Mercadão do SNC · compra no balcão",
    "gui.snc_energies.mercadao.money": "Carteira: R$ %s",
    "gui.snc_energies.mercadao.restock": "Reposição às 07h",
    "gui.snc_energies.mercadao.shelf.bebidas": "Prateleira de bebidas",
    "gui.snc_energies.mercadao.shelf.sementes": "Prateleira de sementes",
    "gui.snc_energies.mercadao.shelf.frios": "Frios e mantimentos",
    "gui.snc_energies.mercadao.shelf.balcao_forte": "Balcão forte",
    "gui.snc_energies.mercadao.purchased": "Mercadão: %s entregue por R$ %s.",
    "gui.snc_energies.mercadao.no_money": "Dinheiro curto: custa R$ %s.",
    "gui.snc_energies.mercadao.sold_out": "Esgotado por hoje; reposição às 07h.",
    "gui.snc_energies.mercadao.sold_out_short": "%s — esgotado hoje",
    "gui.snc_energies.mercadao.stock": "%s · %s na prateleira",
}

EN = {
    "block.snc_energies.mercadao_shelf": "Mercadão Counter",
    "block.snc_energies.mercadao_anchor": "Mercadão Anchor",
    "item.snc_energies.mercajeiro_spawn_egg": "Shopkeeper Spawn Egg",
    "entity.snc_energies.mercajeiro": "Shopkeeper",
    "gui.snc_energies.panel.subtitle.mercadao": "SNC street market · counter purchase",
    "gui.snc_energies.mercadao.money": "Wallet: R$ %s",
    "gui.snc_energies.mercadao.restock": "Restock at 07:00",
    "gui.snc_energies.mercadao.shelf.bebidas": "Beverage shelf",
    "gui.snc_energies.mercadao.shelf.sementes": "Seed shelf",
    "gui.snc_energies.mercadao.shelf.frios": "Deli & pantry",
    "gui.snc_energies.mercadao.shelf.balcao_forte": "Strong counter",
    "gui.snc_energies.mercadao.purchased": "Mercadão: %s delivered for R$ %s.",
    "gui.snc_energies.mercadao.no_money": "Short on money: costs R$ %s.",
    "gui.snc_energies.mercadao.sold_out": "Sold out today; restock at 07:00.",
    "gui.snc_energies.mercadao.sold_out_short": "%s — sold out today",
    "gui.snc_energies.mercadao.stock": "%s · %s on the shelf",
}


def add(path, entries):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    data.update(entries)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("updated", os.path.relpath(path, ROOT))


add(os.path.join(LANG, "pt_br.json"), PT)
add(os.path.join(LANG, "en_us.json"), EN)
