# SNC Energies 0.7.0

Tier Agroindustrial completo + retrogen opcional do Mercadão.

## Novidades

- **Compactadora** (11ª máquina industrial, 2×1×2, tier 4 elétrico — 120 E por lote): fardos de feno a partir de 9 trigos ou 9 arroz, bloco de osso a partir de 9 farinhas de osso, briquetes de biomassa em lote (6 cascas de arroz ou 6 serragens → 3), chapa isolada a partir de 8 farelos de soja e o clássico bolo (4 açúcares + 1 ovo). Painel próprio no padrão SNC, compatível com chave de fenda e modos de face.
- **Silo** (2×2×3, sem orientação, 12 células): armazenagem a granel com 16 fileiras de 16.384 itens (262.144 no total), um tipo de item por fileira — grãos nunca se misturam. Recepção por cima (funis e tubos), saída por baixo; buffers de entrada e extração drenam sozinhos. Depósito/retirada em massa clicando com pilhas grandes. Receita: chapas de aço + engrenagens.
- **Retrogen opcional do Mercadão**: `/gamerule snc_mercadao_retrogen true` liga a backfill em chunks antigos já carregados (fila processada a 2 chunks/tick, sem travar o servidor); checagem de proximidade evita duplicar um mercado existente. Comando de admin `/retrogen mercadao` (raiz: valida gamerule + proximidade), `/retrogen mercadao here|at <pos>` (força a checagem no chunk).

## Qualidade

- Suíte funcional: **629 verificações PASS** standalone (`verification/0.7.0-standalone.log`), 68 novas cobrindo o contrato multibloco da compactadora, as 7 receitas conservativas, o granel do silo (sem mistura, buffers, persistência, teardown) e o básico do retrogen.
- Instalação na instância com backup prévio e SHA-256 conferidos: `7D5261F830B9C965CC414D8E0F13659D7AFB7E2B9E06E7F0C086DB55D013FAE0` (`verification/installation-0.7.0.json`).

## Notas

- Requer Minecraft 26.3, Fabric Loader 0.19.5+ e Fabric API 0.161.0+26.3 (Java 25).
- Nenhum ID antigo alterado; mundos e máquinas existentes preservados.
- Changelog completo por versão: `PROGRESSAO.md` no repositório.
