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

## Estado atual — 0.2.0

- Onze novos equipamentos multiblocos com modelos nativos 3D, colisão baseada na geometria, controlador único, menus e desmontagem com devolução do inventário: moinho, prensa e nove industriais. Texturas de blocos/itens 128×128.
- Colonial: arroz, soja e erva-mate; processamento manual, óleo, farelo, cascas, briquetes, pão e infusão com retorno da tigela. Serragem também vira briquete.
- Vapor: estanho em chunks novos (Y -16 a 80), bronze, caldeira a combustível com água em baldes e rede independente de vapor. Serraria produz tábuas/serragem; fundição produz aço.
- Eletromecânico: turbina, laminador, chapas, fios e receitas revistas das máquinas elétricas existentes. Britagem de metais produz o pó correspondente.
- Agroindustrial: secagem em lote, extração de óleo e chapas isoladas; circuitos básicos usam quartzo, dando função à exploração do Nether.
- Voltaico: refinamento de Voltaite, circuitos avançados e síntese de cinco minérios. Cada lote consome matriz + 160.000 E e produz duas unidades, conservando a amostra. Matriz custa óleo, pedra e 48.000 E na refinaria.
- Automação atual: funis no controlador, entradas por cima/lados e saídas por baixo; cabos elétricos e tubos de vapor. Saídas cheias pausam sem perder reagentes ou resíduos. Estados internos são persistidos. Desde 0.3.0: tubos de itens com buffer de 1 item por célula (conservação exata, roteamento BFS limitada, sucção das saídas de máquinas), funis automatizam fornalha/britador/gerador via WorldlyContainer e as 11 industriais aceitam válvula de redstone com status dedicado no painel.
- Guia pt_br/en_us: livro + semente de trigo. Inclui processos, operação, montagem industrial, cultivo e integração opcional.
- Compatibilidade de servidor testada especificamente com SNC Adventures 1.2.50 e 1.2.57, além do perfil sem Adventures. A instância mantém o Adventures 1.2.57 que já estava instalado. Integração atual: bagaço como combustível no fogão e na caldeira. Não altera receitas, dinheiro, UV ou máquinas do Adventures.
- Pendências do plano completo: filtros de itens, configuração de lados e redstone por face, bombas/tanques externos, animações de mecanismos, motores para Adventures, UV elétrico e contratos de comércio. Não declarar estas funções como entregues.
- Limites dos testes: mundos isolados e serialização de máquinas; não equivalem a validação de todos os saves antigos ou de sessões multiplayer prolongadas. IDs antigos preservados. Nenhum mundo do usuário foi aberto pelos testes.

### v0.3.0 — Automação (25/09/2026)

- Tubo de Itens: cada célula guarda até 1 item; item estaciona no tubo quando o destino está cheio e retoma sozinho. Rede encontra saída adjacente ou por BFS de até 512 células; um item por passo, commit somente com destino confirmado.
- Sucção automática: tubo vazio puxa produtos prontos de fornalha, britador e controladores industriais (saída por baixo do controlador, preservando o contrato 0.2.0). Funis e tubos não são drenados.
- Máquinas antigas automatizáveis: fornalha elétrica e britador (insumo qualquer lado, produto qualquer lado); gerador e fogão só recebem combustível, nada é extraído.
- Válvula de redstone: alternável no painel do sintetizador, modo persistido no save; desligar deixa terminar o lote em curso e nada novo começa. Status "Redstone inativa" na interface.
- Evidências: 469 verificações standalone e 473 com Adventures 1.2.57 (`verification/0.3.0-*.log`); instalação com backup e hash em `verification/installation-0.3.0.json`.

Procedimentos e evidências: `tools/FUNCTIONAL_TESTS.md`, `verification/` e `GUIA-DE-TESTE.md`.
