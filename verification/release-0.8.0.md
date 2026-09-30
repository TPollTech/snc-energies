# SNC Energies 0.8.0 — Motores Adventures + UV elétrico opt-in

Data: 30/09/2026 · Minecraft 26.3 · Fabric Loader 0.19.5

## Destaques

- **Motor de Bebidas** (opt-in, requer SNC Adventures): encoste na Esmagadora de Cana ou Prensa de Uvas, carregue a fornada e ligue energia SNC. Ele entrega a fornada exata, coleta caldo/bagaço e pausa sem perdas quando falta energia. Automação transacional: sem jogador falso, sem leitura de campos privados, lote confirmado antes de aplicar — o contrato entre os mods é respeitado à risca. Caldeirão, dorna, alambique e barril continuam manuais.
- **Lâmpada UV Elétrica** (opt-in): variante elétrica da lâmpada UV do Adventures. Energia própria (ciclo de 1.200 E = 600 ticks, 20 E/t acesa), redstone independente, luz nível 11 para afetar apenas a maturação UV feita pela lógica original do Adventures. A lâmpada original segue intacta.
- **Caderno da Oficina**: página nova do **Silo** (operação + receita, linguagem simples), texto de operação da **Compactadora** na página dela, e a página de Adventures atualizada explicando motor e UV elétrico.
- **Prévias 3D no estúdio**: `previews/compactadora.html` (controle do pistão: desce e espreme o fardo de demonstração) e `previews/silo.html` (controle do nível de grão: fileiras enchendo de baixo para cima). Geometria real do jogo, estúdio do SNC 75, variantes offline autocontidas.

## Sem Adventures

O jogo funciona igual às versões anteriores: o motor idela com máquinas desconhecidas (não toca em nada) e a lâmpada só acende com energia. Nada é registrado em nome do outro mod.

## Qualidade

- Suíte funcional standalone: **640 verificações PASS** (`verification/0.8.0-standalone.log`).
- Suíte funcional com SNC Adventures 1.2.61: **656 verificações PASS** (`verification/0.8.0-adventures.log`), incluindo a cadeia real cana → moenda → caldo/bagaço coletados pelo motor com conservação total.
- Gametest de cliente: painéis do Motor e do Silo renderizam com layout sincronizado; caderno exibe as páginas novas (`verification/panel-beverage_motor.png`, `verification/panel-silo.png`, `verification/progression-guide-0.8.0.png`).

## Uso

Substitua o `snc-energies.jar` na pasta mods. Receitas: Motor = 4 chapas de aço + 2 circuitos + engrenagem + redstone; Lâmpada UV Elétrica = 6 vidros + 2 fios de cobre + quartzo. Detalhes de operação no `GUIA-DE-TESTE.md` e no Caderno da Oficina no jogo.
