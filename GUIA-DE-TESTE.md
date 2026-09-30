# SNC Energies 0.8.0 — teste no jogo

Integrado ao SNC Adventures: Motor de Bebidas e Lâmpada UV Elétrica. As prévias 3D da Compactadora e do Silo entram no padrão do estúdio de veículos, com controles de demonstração. Reinicie o Minecraft após substituir o JAR.

## Começo

Livro + semente de trigo → Caderno da Oficina SNC. A versão 0.8.0 traz páginas novas: **Compactadora** (texto de operação junto das receitas) e **Silo** (página própria, depois das industriais). A página 06 / SNC Adventures explica o Motor de Bebidas e a Lâmpada UV Elétrica em linguagem simples.

## Prévias 3D novas (fora do jogo)

- `previews/compactadora.html` (+ `-offline.html`): estúdio com controle de **progresso da prensagem** — o pistão desce e aperta o fardo de demonstração.
- `previews/silo.html` (+ `-offline.html`): estúdio com controle de **nível de grão** — as fileiras enchem de baixo para cima.
- As duas usam a geometria real do jogo (`previews/industry-models.json` agora inclui o silo) e o estúdio do SNC 75 (`vehicle-studio.js`).

## Motor de Bebidas (opt-in, só com Adventures)

1. Fabrique: 4 chapas de aço + 2 circuitos + engrenagem + redstone (PCP / GRG / PCP).
2. Encoste o motor **na frente** da Esmagadora de Cana ou da Prensa de Uvas (a frente do motor aponta para a máquina; ele gira conforme a sua posição ao colocar).
3. Coloque cana (4 por fornada) ou uvas no slot **Carga da máquina** e ligue energia SNC no motor (80 E por passo, 10.000 E de reserva).
4. O motor entrega a fornada, espera a máquina terminar sozinha (regras dela: água, tempo, fases) e guarda caldo/bagaço no slot **Coleta**.
5. Funis podem alimentar o motor (só aceita o que alguma máquina motorizada consome) e retirar da Coleta.
6. Sem energia o motor pausa sem tocar em nada; lote em curso na máquina termina normalmente. Nada se duplica nem desaparece — cada movimento é confirmado antes de aplicar.

A Caldeirão de Cerveja, Dorna, Alambique e Barril continuam manuais (contrato deste marco).

## Lâmpada UV Elétrica (opt-in, só com Adventures)

1. Fabrique: 6 vidros + 2 fios de cobre + quartzo (GWG / GQG / GGG).
2. Coloque sobre a plantação de uvas (ou qualquer planta do Adventures que amadureça com UV).
3. Conecte energia: ela compra um ciclo de 1.200 E (600 ticks) e gasta 20 E/t enquanto acesa.
4. Ela acende com luz nível 11 — abaixo do atalho de crescimento vanilla (12) — então o **único efeito** é o amadurecimento UV da planta, feito pela lógica do próprio Adventures.
5. Sem energia ela apaga sozinha; nada de redstone obrigatório. A lâmpada original do Adventures segue funcionando igual.

## O que não mudou

- Receitas, tempos, dinheiro, lâmpadas originais e máquinas do Adventures: intocados.
- Nenhum dos mods depende do outro: sem Adventures, o motor idela e a lâmpada funciona como luminária cara.
- Todas as máquinas e regras das versões anteriores permanecem.

## Evidências

- Suíte standalone: **640 verificações PASS** (`verification/0.8.0-standalone.log`).
- Suíte com Adventures 1.2.61: **656 verificações PASS** (`verification/0.8.0-adventures.log`) — inclui a cadeia real cana → moenda → caldo/bagaço coletado pelo motor, sem perdas.
- Gametest de cliente: painéis do motor e do silo renderizam e abrem; caderno mostra as páginas novas (`verification/panel-*.png`, `progression-guide-0.8.0.png`).
