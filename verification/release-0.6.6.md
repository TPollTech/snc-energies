# SNC Energies 0.6.6

Marco acumulado 0.5.0 → 0.6.6 (o primeiro release desde a 0.4.1).

## Destaques

- **Veículos SNC no jogo (0.6.1)**: trator SNC 75 dirigível, plantadeira SNC 75-P engatável, colheitadeira SNC 90 e Carreta Graneleira SNC 90-C — plantio/colheita reais por faixas, combustível a óleo vegetal, engate com transferência conservativa de carga, painéis próprios.
- **Mercadão completo (0.6.0–0.6.4)**: prédio fechado com porta, letreiro MERCADÃO pintado, vitrines que esvaziam durante o dia, interior cenográfico, Mercajeiro que debita a carteira do SNC Adventures pelo balcão, estoque diário persistido, estrutura gerada no mundo (spacing 24, 18 biomas).
- **Correção crítica de worldgen (0.6.5)**: os JSONs de estrutura estavam sem o prefixo `worldgen/` desde a 0.6.0 e os registries os ignoravam em silêncio — o Mercadão nunca foi gerado. Corrigido contra o layout real da 26.3; `/locate structure snc_energies:mercadao` agora responde.
- **Sweep defensivo dos datapacks (0.6.6)**: nenhum outro erro de pasta. Na 26.3 `worldgen/feature/` é o nome correto de configured features (`configured_feature/` não existe mais nesta versão) — estanho e voltaite sempre geraram.
- **Automação (0.5.0)**: modo de I/O por lado nas 11 industriais (chave de fenda), redstone invertível e filtro de 9 slots por célula do Tubo de Itens.

## Qualidade

- Suíte funcional: **561 verificações PASS** standalone (`verification/0.6.6-standalone.log`), incluindo probes permanentes de worldgen (structure/structure_set/template_pool/placed_feature com controles vanilla), `/locate` end-to-end no servidor de testes e `RecipeManager.byKey`.
- Instalação na instância com backup prévio e SHA-256 conferidos: `8F813C9E8987FD96387B5AC6D932F1F1C5DC480F6B7B419B79C3454FF8E648D9` (`verification/installation-0.6.6.json`).

## Notas

- Requer Minecraft 26.3, Fabric Loader 0.19.5+ e Fabric API 0.161.0+26.3 (Java 25).
- Chunks já explorados não recebem estruturas/minérios retroativamente (sem retrogen): procure em região nova.
- Changelog completo por versão: `PROGRESSAO.md` no repositório.
