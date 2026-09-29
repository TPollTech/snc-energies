# SNC Energies 0.4.0 — Caderno legível com busca e índice lateral

## Caderno da Oficina (FieldGuideScreen)
- **Legibilidade corrigida**: fundo/pergaminho desenhados na camada de fundo (o desenho antes do `super.extractRenderState` escurecia tudo) e tintas quentes sobre pergaminho claro — corpo `#4d3a26`, títulos `#7a3a20`, apagado `#8a6a48`, erro `#a04430`. Fim do texto quase preto ilegível.
- **Busca ao digitar**: campo no topo do caderno; matching sem acento (ex.: "minerio" encontra "minério"); todas as palavras devem casar com título + corpo + receitas da página. `<`/`>` ciclam só entre resultados; dica "Página X de Y" e mensagem própria quando nada é encontrado.
- **Índice lateral clicável** (novo): coluna à esquerda com uma linha por página (títulos sem o prefixo "NN /", máquinas pelo nome do bloco). Clique pula direto para a página com som de virar página; página atual destacada; entradas fora do filtro da busca ficam apagadas; clicar numa apagada limpa a busca e pula. Roda do mouse rola o índice ou o texto, conforme o lado.

## Requisitos
- Minecraft **26.3** · Fabric Loader **0.19.5** · Fabric API **0.161.0+26.3** · Java 25

## Instalação
1. Copie `snc-energies.jar` para a pasta `mods` da instância (remova versões anteriores).
2. Obrigatório: `fabric-api-0.161.0+26.3.jar` na mesma pasta.

## Integridade
- SHA-256 do `snc-energies.jar` anexado (build do CI): `B96B1D9BD88584CF16230C9E28861359788D00A12748203C20298A90A74A14F8`
- O jar instalado em `Instances/SNC energies/mods/` (SHA-256 `B58B4B3D…1501`) foi buildado localmente das mesmas fontes da tag `v0.4.0` — manifesto em `verification/installation-0.4.0.json`.

## Também neste marco
- v0.3.0 (automação): tubos de itens com conservação exata, sucção nas saídas das máquinas, válvula de redstone nas 11 industriais.
- Texturas 128×128 geradas por código (motor fbm/bevel) — 99 texturas regeneradas de forma determinística.

## Notas
- Traduções pt_br (prioritário) e en_us; strings vindas dos geradores, nunca hardcoded.
- Validação visual no jogo ainda pendente de teste do dono.
