# SNC Energies — progressão regional e integração

Direção aprovada pelo usuário em 25/09/2026. Este documento distingue o escopo completo aprovado da primeira versão jogável 0.2.0: os cinco tiers têm processos funcionais; automação avançada e a integração ampliada com Adventures continuam pendentes.

## Contrato entre os mods

- Energies: energia, metalurgia, extração, transporte industrial, arroz e soja.
- Adventures (namespace legado `intoxicantes`): cultivos existentes, bebidas, efeitos, personagens, comércio e suas receitas canônicas.
- Cada mod deve iniciar sozinho. Integrações são opcionais; conteúdos de terceiros não são registrados novamente.
- Máquinas existentes, IDs, inventários, timers e comportamento manual continuam válidos. Nenhuma instalação pode exigir energia retroativamente de uma máquina antiga.
- O primeiro recurso integrado é `intoxicantes:bagaco_de_cana`: tag opcional `snc_energies:biomass_fuel`, 100 ticks a 80 E/t no fogão (8.000 E). Não altera o rendimento da moenda.
- Os demais recursos deste contrato são futuros: portas de automação transacionais, motorização, receitas compartilhadas e iluminação elétrica.
- Antes de automatizar Adventures, separar operações de produção da interação com Player. Expor inserção/extração por lado sem criar jogador falso nem ler campos privados por reflexão. Operações devem simular capacidade e confirmar o lote inteiro no servidor; preservar componentes e recipientes.
- ProcessosBebida continua sendo a fonte das receitas de bebidas. A ponte deve consultar um contrato público e versionado, ativado apenas quando ambos os mods estão presentes. Evitar referência direta às classes opcionais nos inicializadores comuns.
- Mostos são itens atualmente. Automatizar itens primeiro; não converter silenciosamente saves antigos em fluidos. Água e vapor terão armazenamento próprio com conservação de volume.
- UV atual continua funcionando. A variante/melhoria elétrica é uma escolha explícita, com energia e redstone independentes.
- Comércio permanece autoritativo no Adventures; adicionar ofertas por interface própria, mantendo cotas e sem implementar segunda carteira. Não incluir venda automática no primeiro marco.

## Cinco tiers

| Tier | Materiais e acesso | Equipamentos e capacidade nova |
| --- | --- | --- |
| Colonial | Madeira, tijolo, ferro e cobre vanilla | Fogão 3×2 existente; moinho 2×1; prensa 2×1; início das lavouras de arroz e soja. Produção manual e aproveitamento de resíduos. |
| Vapor | Estanho novo + cobre → bronze; peças feitas no tier inicial | Caldeira 2×2×3; motor 2×1; serraria 3×2. Água + biomassa → vapor; tubulações, tanques e processamento mecânico. |
| Eletromecânico | Ferro + carbono → aço; motores e componentes de cobre | Gerador acoplado ao vapor, britador e fornalha elétrica existentes revisados, laminador 2×1. Energia elétrica, cabos, tubos de itens e configuração de lados. |
| Agroindustrial | Aço, óleos vegetais, circuitos e peças de precisão | Secador 3×3×4, extratora 3×2, compactadora 2×1, silos. Processamento em lote, briquetes e combustível vegetal; melhorias elétricas opcionais do Adventures. |
| Voltaico | Voltaite existente refinada com processos dos tiers anteriores | Refinaria 3×3×3 e sintetizador 3×3×3. Produção renovável de minérios mediante amostra não consumida, matriz mineral consumida, reagentes e energia. |

A tabela acima registra o plano completo. A versão 0.2.0 entrega caldeira, serraria, fundição, turbina, laminador, secador, extratora, refinaria e sintetizador; motor separado, compactadora elétrica, silos, tanques externos e tubos de itens ainda não estão implementados. Moinho/prensa medem 2×1×2; fundição/turbina, 2×2×2; demais equipamentos seguem as dimensões indicadas. O fogão existente mantém suas dimensões.

## Regras de gameplay

1. Resíduos têm saídas úteis: serragem/casca/bagaço → combustível ou compostagem. Nenhuma saída secundária pode sumir quando o destino está cheio.
2. Equipamentos têm geometria 3D, colisão coerente, frente reconhecível e estados de atividade. Texturas Energies 128×128; preservar assets Adventures existentes.
3. Tier novo libera processo novo. Máquinas anteriores permanecem úteis; melhorias não multiplicam gratuitamente rendimento, velocidade e eficiência ao mesmo tempo.
4. Um minério novo inicial (estanho), metais vanilla e Voltaite. Bronze/aço são ligas, não minérios. Ferro não vira Voltaite no britador; criar pós correspondentes e receitas corretas antes da progressão elétrica.
5. Interfaces mostram causas de parada e entradas/saídas. Sem explosões destrutivas ou manutenção obrigatória nesta primeira versão.
6. Guia em pt_br/en_us com árvore, receitas e montagem; guia Adventures recebe referências quando suas funcionalidades forem alteradas.
7. Renovabilidade só no tier final. Produção não pode sustentar um ciclo de energia/material gratuito. Receitas finais terão tabela explícita de consumo/rendimento antes de entrar no jogo.
8. Erva-mate é ramo opcional após a cadeia principal; reutilizar secagem/moagem/peneiramento, sem bloquear o tier final.

## Marcos e critérios de entrega

- A: compatibilidade atual, bagaço → energia, testes de ambos os perfis e preservação dos saves.
- B: tier Colonial completo, arroz/soja, resíduos, guia inicial e preview aprovado dos modelos.
- C: vapor, bronze, serraria, água e montagem multiblocos; validar falta de água/combustível e desmontagem sem duplicação.
- D: aço, eletrificação, receitas de minério corrigidas, automação transacional Adventures; testar receitas com duas entradas e todas as saídas.
- E: agroindústria e economia, cotas, melhorias UV opt-in e testes cliente/servidor.
- F: Voltaite, síntese renovável, balanceamento da cadeia completa e ramo opcional de erva-mate.

Para cada marco: testar energia/materiais conservados, saída cheia, restart, chunks descarregados, multiplayer e slots/recipientes. Novos minérios somente em chunks novos, sem retrogen automático. Saves antigos testados em cópias. Compatibilidade é evidência por versão/cenário, não garantia sobre futuras versões desconhecidas.

## Estado atual — 0.6.1

- Onze novos equipamentos multiblocos com modelos nativos 3D, colisão baseada na geometria, controlador único, menus e desmontagem com devolução do inventário: moinho, prensa e nove industriais. Texturas de blocos/itens 128×128.
- Colonial: arroz, soja e erva-mate; processamento manual, óleo, farelo, cascas, briquetes, pão e infusão com retorno da tigela. Serragem também vira briquete.
- Vapor: estanho em chunks novos (Y -16 a 80), bronze, caldeira a combustível com água em baldes e rede independente de vapor. Serraria produz tábuas/serragem; fundição produz aço.
- Eletromecânico: turbina, laminador, chapas, fios e receitas revistas das máquinas elétricas existentes. Britagem de metais produz o pó correspondente.
- Agroindustrial: secagem em lote, extração de óleo e chapas isoladas; circuitos básicos usam quartzo, dando função à exploração do Nether.
- Voltaico: refinamento de Voltaite, circuitos avançados e síntese de cinco minérios. Cada lote consome matriz + 160.000 E e produz duas unidades, conservando a amostra. Matriz custa óleo, pedra e 48.000 E na refinaria.
- Automação atual: funis no controlador, entradas por cima/lados e saídas por baixo; cabos elétricos e tubos de vapor. Saídas cheias pausam sem perder reagentes ou resíduos. Estados internos são persistidos. Desde 0.3.0: tubos de itens com buffer de 1 item por célula (conservação exata, roteamento BFS limitada, sucção das saídas de máquinas), funis automatizam fornalha/britador/gerador via WorldlyContainer e as 11 industriais aceitam válvula de redstone com status dedicado no painel. Desde 0.5.0: cada lado das industriais tem modo próprio (ambos/entrada/saída) configurado pela chave de fenda, com modo de redstone também invertível, e cada célula do tubo aceita whitelist de 9 slots editável ao clicar no tubo.
- Guia pt_br/en_us: livro + semente de trigo. Inclui processos, operação, montagem industrial, cultivo e integração opcional.
- Compatibilidade de servidor testada especificamente com SNC Adventures 1.2.50 e 1.2.57, além do perfil sem Adventures. A instância mantém o Adventures 1.2.57 que já estava instalado. Integração atual: bagaço como combustível no fogão e na caldeira. Não altera receitas, dinheiro, UV ou máquinas do Adventures.
- Veículos no jogo desde a 0.6.1: trator, plantadeira, colheitadeira e Carreta Graneleira com painéis, óleo vegetal, colheita/semeadura reais e engate (ver seção v0.6.1). Pendências do plano completo: bombas/tanques externos e contratos de comércio além do Mercadão. Motores para Adventures e UV elétrico foram entregues na v0.8.0 (caldeirão, dorna, alambique e barril continuam manuais).
- Limites dos testes: mundos isolados e serialização de máquinas; não equivalem a validação de todos os saves antigos ou de sessões multiplayer prolongadas. IDs antigos preservados. Nenhum mundo do usuário foi aberto pelos testes.

### v0.3.0 — Automação (25/09/2026)

- Tubo de Itens: cada célula guarda até 1 item; item estaciona no tubo quando o destino está cheio e retoma sozinho. Rede encontra saída adjacente ou por BFS de até 512 células; um item por passo, commit somente com destino confirmado.
- Sucção automática: tubo vazio puxa produtos prontos de fornalha, britador e controladores industriais (saída por baixo do controlador, preservando o contrato 0.2.0). Funis e tubos não são drenados.
- Máquinas antigas automatizáveis: fornalha elétrica e britador (insumo qualquer lado, produto qualquer lado); gerador e fogão só recebem combustível, nada é extraído.
- Válvula de redstone: alternável no painel do sintetizador, modo persistido no save; desligar deixa terminar o lote em curso e nada novo começa. Status "Redstone inativa" na interface.
- Evidências: 469 verificações standalone e 473 com Adventures 1.2.57 (`verification/0.3.0-*.log`); instalação com backup e hash em `verification/installation-0.3.0.json`.

### v0.5.0 — Configuração de lados e filtragem (26/09/2026)

- Chave de Fenda: clique num lado da industrial cicla ambos → entrada → saída (padrão preserva o contrato 0.2.0); agachar+clique alterna sempre → com redstone → SEM redstone (novo status 8 "Redstone invertida"). Faces mapeadas pelo facing, estáveis sob rotação; modo e faces persistidos.
- Filtro do Tubo de Itens: clique no tubo abre a whitelist da célula (9 slots, botão Limpar). Vazia = tudo passa; vale na entrada (funis e sucção) — item já dentro do tubo nunca é retido, preservando a conservação exata. Persistência com `ItemStack.OPTIONAL_CODEC` (o CODEC comum rejeita pilhas vazias).
- Evidências: 484 verificações standalone e 488 com Adventures 1.2.57 (`verification/0.5.0-*.log`); instalação com backup e hash em `verification/installation-0.5.0.json`.

### v0.8.0 — Motores Adventures + UV elétrico opt-in (30/09/2026)

- **Motor de Bebidas** (`beverage_motor`, bloco único elétrico, 80 E/passo a cada 40 ticks): encostado na frente da Esmagadora de Cana ou Prensa de Uvas, alimenta a fornada exata da receita e coleta caldo/bagaço para o próprio buffer de 2 slots. A máquina dockada é resolvida por instância (método público `tipoMaquina` + campos publicados `idxInsumo/idxOut/idxExtra`) e a leitura de receitas usa só os métodos públicos de `ProcessosBebida` via reflexão — sem jogador falso, sem campos privados, sem classes do companion no classpath. Cada movimento é transacional (capacidade simulada, snapshot + rollback); sem energia o motor pausa sem tocar em nada. Caldeirão/dorna/alambique/barril permanecem manuais neste marco.
- **Lâmpada UV Elétrica** (`electric_uv_lamp`, variante opt-in da lampada_uv): buffer próprio de 4.000 E; compra um ciclo de 1.200 E (600 ticks) e gasta 20 E/t acesa, sem redstone. Emite luz 11 — abaixo do atalho vanilla (12) — para que o único efeito seja a maturação UV feita pela lógica do próprio Adventures (`uv_age` 0..3). A lâmpada original e o caminho de sol continuam intactos.
- **Caderno**: página própria do Silo (operação + receita) e texto de operação da Compactadora na página dela; página 06 (SNC Adventures) reescrita descrevendo motor e UV elétrico; craft dos dois blocos novos gerado a partir da receita real.
- **Prévias no estúdio**: `previews/compactadora.html` (pistão com slider de progresso + fardo de demonstração espremido) e `previews/silo.html` (slider de nível de grão enchendo fileiras de baixo para cima), ambas com importmap local, dados embutidos e variantes `-offline.html`; `industry-models.json` agora inclui o silo e `assets/industry-materials.json` alimenta as prévias industriais.
- Evidências: **640 verificações** standalone e **656 com Adventures 1.2.61** (`verification/0.8.0-*.log`) — inclui a cadeia real cana → moenda → caldo/bagaço coletados pelo motor, conservação total, recusa de máquinas fora do contrato, persistência e apagão da lâmpada. Gametest de cliente cobre os painéis novos e o caderno.

Procedimentos e evidências: `tools/FUNCTIONAL_TESTS.md`, `verification/` e `GUIA-DE-TESTE.md`.

### v0.6.0 — Mercadão (27/09/2026)

- Prateleiras funcionais do Mercadão (`snc_energies:mercadao_shelf`, 4 variantes — bebidas, sementes, frios, balcão forte) + âncora invisível que mantém um Mercajeiro (NPC humanoide) atrás do balcão; ele volta ao posto sozinho se empurrado e renasce pelo âncora se morto. A compra é só pelo balcão (compra, nunca vende): o servidor debita a carteira do Adventures (`PlayerMoney`) e entrega o item real; nada de segunda carteira, sem networking próprio (botão de menu, como o trator).
- Catálogo = preços do Gago/Traficante do Adventures (cerveja R$ 15, vinho 25, hidromel 20, cachaça 35, rum 40; sementes 4× por R$ 4; pão, garrafa, arroz/soja/óleo; balcão forte com baseado, cocaína, heroína, LSD, ópio, extrato de cafeína e pó estelar). Itens resolvidos por Identifier na primeira consulta (após o registro global congelar) — entradas inexistentes somem no perfil sem Adventures; nada é registrado em nome de outro mod.
- Estoque diário por prateleira (reposição a cada dia do mundo; persistido), recusa carteira vazia sem tocar estoque, som e mensagem de compra/recusa, fallback de entrega no chão (contrato Traficante.entregar). Sem alteração nenhuma no mod Adventures.
- Estrutura gerada no mundo: prédio 11×6×9 com as 4 prateleiras, lanternas e o posto do Mercajeiro (`data/snc_energies/structure/mercadao.nbt` + template pool/structure set/tag de bioma), ovo de spawn e receita para o bloco (tábuas de pinheiro + esmeralda + barril).
- Evidências: 503 verificações standalone e 498 com Adventures 1.2.61 (`verification/mercadao-standalone.log`, `verification/mercadao-adventures.log`) — compra debitou exatamente o preço, esgotou e repôs estoque diário; recusa no perfil sem carteira. Pendência externa ao Mercadão: falha pré-existente do teste do trator (implemento acoplado gasta as próprias sementes), fora do escopo desta entrega.

### Hotfix 27/09 — travamento na criação de mundo

- Causa: o jar integrava a colheitadeira (renderer + entidade) sem empacotar os assets dela; `VehicleRig.load` lançava `FileNotFoundException` para `snc_energies:vehicle/harvester-model.json`, o reload de recursos abortava e o cliente ficava preso em "Preparando a criação do mundo…".
- Correção: `tools/export_harvester_game_assets.py` empacota modelo (747 cubos/83 grupos) + 21 texturas 128×128 no resource pack (`verification/harvester-game-assets.json`); blockstate/model do `mercadao_anchor` silencia o aviso de variante ausente. Jar reinstalado com backup prévio e SHA conferidos (`verification/installation-harvester-assets.json`).

### v0.6.1 — Veículos no jogo (28/09/2026)

- Colheitadeira SNC 90 dirigível e funcional: colheita por faixas alinhada com as fileiras da plantadeira (só lavouras maduras; tanque cheio pausa sem destruir nada e repõe o ciclo replantando), óleo vegetal como combustível (1 porção = 80 ticks de trabalho; motor morre em silêncio ao secar e recusa partida sem óleo), marcha lente sem motorista e plataforma de corte elevável. Slot de óleo nos painéis do trator e da colheitadeira corrigido (container dedicado, padrão vanilla).
- Carreta Graneleira SNC 90-C (novo): engata no ponto traseiro da colheitadeira (busca por proximidade, botão no painel dela), acompanha soldada durante o trabalho, recebe a descarga do tanque com conservação exata de pilhas (carreta cheia pausa sem voidar) e desengatada guarda a carga no mundo, com painel próprio de 15 slots para descarregar à mão. Persistida junto da colheitadeira ("AttachedCart"). Receita: 6 aço + barril + 2 ferro + carrinho de mina.
- Painéis dos veículos: botões de ignição, plataforma/capa e engate/descarga da carreta são autoritativos no servidor (data slot dedicado mostra o estado do engate). Ticks reordenados para posar o implemento acoplado antes de plantar/colher, mantendo a solda.
- Bug extra corrigido: `SncMenus.PLANTER` não tinha screen registrado — abrir o painel da plantadeira congelaria o cliente; agora tem tela própria (e a carreta também).
- Infusão de erva-mate e ovo de spawn do Mercador (renomeado de "Mercajeiro"; IDs internos mantidos) redesenhadas com silhueta sólida; geradores idempotentes em `tools/`.
- Mercadão: renderer da prateleira lê o estoque sem mutar a block entity (antes mutava na render thread); o Mercajeiro/Mercador agora serve prateleiras em qualquer direção horizontal do posto, não só ao sul.
- Prévia 3D interativa do Mercadão: `previews/mercadao.html` (modelo real do NBT, 329 cubos, toggles de telhado/toldos/NPC/âncora) e `previews/mercadao-offline.html` autocontida (2 MB).
- Evidências: 559 verificações standalone e 570 com Adventures 1.2.50 (`verification/0.6.1-*.log`) — 19 verificações novas cobrem painel da carreta, engate/desengate pelos dois caminhos (painel e entidade), solda no reboque após passo de direção, conservação exata na descarga (94 trigo + 40 cenouras; carreta empacotada pausa com o restante no tanque), carga preservada ao desengatar e devolução da carga ao remover a carreta. O teste antigo do trator foi atualizado para a semântica soldada (a origem do plantio do implemento acoplado é a posição da engata após o passo de direção, não a posição estacionada). Instalação com backup e SHA em `verification/installation-0.6.1.json`.
- Quirk do harness registrado: no mundo funcional, adições de entidades no chunk (4,0) dentro do mesmo tick do teardown do teste anterior não ficam visíveis às buscas (chunks 0-3 e 40 aceitam); o teste da carreta roda no tick seguinte e usa a zona 44-54. Código do teste não entra no JAR.

### v0.6.2 — Mercadão fechado e vitrines (28/09/2026)

- Prédio agora fecha de verdade (reclamação do dono sobre o salão aberto): fachada sul com porta central 3×2, duas vitrines de vidro simétricas e plinto sólido; janelas de vidro nas laterais; paredes de pinheiro com pilares de tronco; lanternas internas para iluminar o salão fechado. `mercadao.nbt` regenerado e auditado (porta/paredes/vidros/lanternas conferidos célula a célula).
- Balcões-modelo de vitrine: o bloco voltou a renderizar modelo estático (`RenderShape.MODEL` — antes era `INVISIBLE`, só os itens flutuavam). Modelo de elementos com base, tampo, painel de fundo, postes, prateleira interna e vidro frontal cutout; 3 texturas novas por variante (tampo, moldura com listra de acento, vidro estilo vanilla). Colisão assimétrica orientada ao facing (o corredor fica livre).
- Produtos como mini-modelos: o renderer exibe as ofertas de pé sobre a prateleira interna, atrás do vidro, com dois tamanhos alternados para leitura de mercado abastecido; itens saem da vitrine quando o estoque diário zera.
- Prévia 3D regenerada (393 cubos: paredes, porta, vitrines, 12 produtos nas prateleiras) com toggle "Vitrines e produtos"; `mercadao-offline.html` repacoteado; corrida de inicialização do viewer corrigida (retry de frame).
- Evidências: 559 verificações standalone e 570 com Adventures 1.2.50 (`verification/0.6.2-*.log`); instalação com backup do 0.6.1 e SHA em `verification/installation-0.6.2.json`.

### v0.6.3 — Letreiro, vitrine viva e interior (28/09/2026)

- Letreiro "MERCADÃO" pintado sobre a porta: bloco novo `mercadao_sign` (3 blocos left/center/right, estrutura-only, sem item/loot), fonte de pixel própria 3×5 ampliada 2× numa faixa contínua de 48 px (placa palha, moldura escura, grão de madeira); legibilidade conferida renderizando a textura em ASCII. Vira drop ao quebrar (sem loot table: nada), preserva o save.
- Vitrine esvazia de verdade: cada oferta exibida encolhe proporcionalmente ao estoque diário restante (fator mínimo 0,55) e some da prateleira ao zerar — o mercado visibly "vai acabando" durante o dia.
- Interior cenográfico: piso xadrez de pedra/andesito, caixas de barril e sacarias de lã atrás dos balcões (fora do corredor e da faixa do Mercador), lanternas internas agora pousadas sobre as caixas.
- Vidros revisados (queixa do dono): os 16 panes do prédio gravam no NBT a conectividade explícita norte/sul/leste/oeste contra os vizinhos reais — não dependem mais de update de vizinho na geração; nenhuma janela nasce "flutuando" ou mal emendada. Porta corrigida para 2×2 de verdade (a revisão anterior deixou-a 3×1).
- Evidências: 559 verificações standalone e 570 com Adventures 1.2.50 (`verification/0.6.3-*.log`); instalação com backup do 0.6.2 e SHA em `verification/installation-0.6.3.json`. Preview 3D com letreiro/cenário (411 cubos) e offline repacoteado.

### v0.6.4 — Mercadão mais frequente (28/09/2026)

- Após `/locate structure snc_energies:mercadao` sem resultado na instância do dono: espaçamento 44→24 e separação 24→12 (mercados ~3× mais próximos) e a tag de biomas sobe de 11 para 18 (entram taiga, snowy_taiga, taigas antigas, floresta escura, pântano e mangue). Estrutura NBT intacta.
- Diagnóstico registrado: "estrutura não encontrada" com o tipo válido é bioma fora da tag ou região já explorada — mundos antigos não recebem retrogen; procurar em chunks novos. O jar precisa estar ≥ 0.6.0 e o jogo reiniciado.
- Evidências: 559 verificações standalone (`verification/0.6.4-standalone.log`, o boot valida o parse do worldgen regenerado); Adventures não reexecutado — nenhum Java mudou desde a 0.6.3. Backup do 0.6.3 e SHA em `verification/installation-0.6.4.json`.

### v0.6.5 — Correção crítica: o Mercadão nunca existiu no worldgen (28/09/2026)

- O dono reportou `/locate` vazio mesmo com o tipo conhecido. Probe de registries dentro da suíte provou: `structure=false, structure_set=false, template_pool=false` enquanto os controles vanilla eram `true` — os JSONs data-driven do Mercadão estavam **sem o prefixo `worldgen/`** nas pastas do datapack desde a 0.6.0 (`structure/mercadao.json`, `structure_set/…`, `template_pool/…`) e os registries os ignoravam silenciosamente. A estrutura nunca foi registrada nem gerada em mundo algum; só o NBT e a tag de biomas estavam certos.
- Correção: caminhos alinhados ao layout vanilla da 26.3 (JSONs em `worldgen/structure`, `worldgen/structure_set`, `worldgen/template_pool`; NBT permanece em `structure/`), arquivos nas pastas erradas removidos. Frequência 24 e 18 biomas preservados.
- Prova end-to-end na suíte (agora permanente): registries `mercadao=true` ×3 e o próprio `/locate` executado no servidor de testes — `The nearest snc_energies:mercadao is at [-240, ~, 464] (508 blocks away)`, com `minecraft:village_plains` como controle positivo. 559 verificações (`verification/0.6.5-standalone.log`). Backup do 0.6.4 e SHA em `verification/installation-0.6.5.json`.
- Mundo do dono: chunks já explorados não regeneram; procurar em região nova — ou recriar o mundo para ver o mercado nascer perto do spawn.

### v0.6.6 — Sweep dos datapacks: nenhum outro erro de pasta (29/09/2026)

- Varredura completa de `data/snc_energies` (86 JSONs) contra o jar vanilla 26.3, na esteia do bug do Mercadão. **Nenhuma outra pasta errada**: `recipe/` (51 resultados no formato novo `result.id`, sem wrappers `"item"/"tag"` antigos), `loot_table/blocks/` (28 blocos com drop; crops usam `match_block`+age idênticos ao wheat vanilla; anchor/sign são `noLootTable` de propósito), tags (`tags/item`, `tags/block`, `tags/worldgen/biome` — o singular É o layout da 26.3) e o worldgen dos minérios, conferidos campo a campo. Cruzamento de referências: todo `snc_energies:` citado em JSON existe no código (blocos, itens, IndustryKind, template_pool).
- A hipótese de que `worldgen/feature/` (estanho/voltaite) estava errada estava **invertida**: na 26.3 a pasta correta É `worldgen/feature/` — o jar vanilla tem 242 features lá e **nenhuma** em `configured_feature/` (renomeado nesta versão). Formato dos JSONs idêntico ao `ore_gravel_nether` vanilla; `SncBiomeModifications` referencia os placed_features certos. E `verification/missing-mod-chunks-20260926.json` já provava estanho/voltaite gerados no mundo do dono (centenas de blocos em chunks escaneados).
- Hardening permanente na suíte: probe de `PLACED_FEATURE` (`tin_ore`/`voltaite_ore` × `ore_coal_upper` vanilla — uma regressão de pasta nos minérios não passaria mais batido, já que o gerador copia `tin_ore` do `voltaite_ore` em runtime) e probe de `RecipeManager.byKey(tractor)`. 561 verificações, FUNCTIONAL TESTS PASSED (`verification/0.6.6-standalone.log`).
- Instalação com backup do 0.6.5 e SHA em `verification/installation-0.6.6.json`. Nenhuma mudança de comportamento no jogo nesta versão — nenhum arquivo de dados precisou ser movido.

### v0.7.0 — Tier Agroindustrial completo + retrogen do Mercadão (29/09/2026)

- **Compactadora** (11º industrial, `compactor`, 2×1×2, elétrica tier 4 a 120 E/lote): 9 trigos ou 9 arroz → fardo de feno, 9 farinhas de osso → bloco de osso, 6 cascas de arroz ou 6 serragens → 3 briquetes, 8 farelos de soja → chapa isolada, 4 açúcares + 1 ovo → bolo. Mesmo contrato das outras industriais (painel, lados, redstone, funis) — geometria própria com aço, bronze e painéis âmbar.
- **Silo** (`silo`, 2×2×3 sem orientação, 12 células): granel com 16 fileiras de 16.384 itens (262.144 no total), um tipo por fileira — culturas nunca se misturam. Recepção por cima, saída por baixo; buffers de entrada e extração drenam sozinhos a cada tick. Depósito/retirada em massa segurando pilhas grandes. Fileiras persistem como amostra de 1 + contador (contagem >64 viola a validação do ItemStack.CODEC no load — mesma lição do filtro da 0.5.0). Derrubar qualquer célula devolve 1 silo + todo o inventário.
- **Retrogen opcional do Mercadão**: gamerule `snc_mercadao_retrogen` (off por padrão) + fila por chunk load processada a 2 chunks/tick; elegibilidade = controle vanilla resolve, busca num raio de 2 regiões não acha mercadao e a caixa de ar no nível do chão (11×9×6, chão sem fluido) cabe o mercado. Colocação pelo próprio template NBT (âncora incluída), overwrite só de ar. Comando admin `/retrogen mercadao` (raiz: valida gamerule/proximidade), `here|at <pos>` força no chunk. Nota 26.3: gamerules viraram entradas de registry — id obrigatoriamente lowercase.
- Evidências: **629 verificações** standalone, FUNCTIONAL TESTS PASSED (`verification/0.7.0-standalone.log`); 68 novas no `AgroFunctionalTest` (contrato da compactadora, 7 receitas conservativas, anel do silo, granel sem mistura, buffers, persistência, teardown, gamerule do retrogen). Instalação com backup do 0.6.6 e SHA em `verification/installation-0.7.0.json`.

## Veículos — fora do JAR (modelagem, 26/09/2026)

- Biblioteca compartilhada de veículos extraída do golden SNC 75 com prova byte-idêntica (17/17 arquivos, `verification/tractor-library-refactor.json`): `tools/vehicle_library.py` (primitivas/rig/exportadores JSON+BBmodel+GLB), `tools/vehicle_texture_kit.py`, `tools/vehicle_verify_library.py` e o estúdio genérico `previews/vehicle-studio.js` + `vehicle-studio.css`. Regra registrada no AGENTS.md (regras 6 e 7).
- SNC 90 — colheitadeira modelada nesse padrão: 747 cubos, 83 grupos articulados, 21 materiais 128×128. Plataforma de corte de 4 blocos com reel articulado e destacável, rotor axial, peneiras com ventilador, picador, tanque vincado, tubo de descarga que ergue/dobra o bico, motor completo sob capô abertura 55°, cabine fechada com interior. `tools/verify_harvester.py` = passed sem avisos (15 cenários de articulação). Prévia `previews/colheitadeira.html` + offline autocontida; revisada nas 4 vistas com todas as articulações testadas. Assets em `assets/harvester/`; nada registrado no jogo.
- Prévia visual da 0.3.0 criada com modelos/texturas/painel reais do JAR: `previews/v030.html` (linha Fornalha → 3 células de Tubo de Itens → Britador; painel do Sintetizador com botão de redstone funcional) via `tools/generate_v030_preview.py`.
- Status 28/09: as pendências acima foram entregues na v0.6.1 — entidade dirigível com registros centralizados, colheita/semeadura reais, combustível por óleo vegetal e engate com a Carreta Graneleira (seção v0.6.1).
