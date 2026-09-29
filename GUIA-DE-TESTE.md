# SNC Energies 0.6.6 — teste no jogo

Instalado em `C:/Users/enzot/curseforge/minecraft/Instances/SNC energies/mods/snc-energies.jar`, junto do SNC Adventures. Reinicie o Minecraft para carregar os JARs novos.

## Começo

Livro + semente de trigo → Caderno da Oficina SNC. Clique com o botão direito para abrir. As setas laterais mudam de página; as setas verticais rolam as páginas industriais. No criativo, todos os equipamentos e materiais estão na aba SNC Energies.

1. Colonial: plante arroz e soja; coloque moinho/prensa em área livre 2×1, com dois blocos de altura. Insira grãos, clique em Girar manivela e mantenha produto/resíduo livres.
2. Vapor: minere estanho em chunks novos (Y -16 a 80), funda e combine 1 estanho com 3 cobres para obter bronze. Caldeira recebe combustível e baldes de água. Conecte tubo de vapor à serraria/fundição; ferro + carvão ou carvão vegetal produz aço.
3. Eletromecânico: caldeira → tubo de vapor → turbina → cabo elétrico → laminador. Aço vira chapas; cobre vira fios. O fogão e gerador também alimentam a rede elétrica.
4. Agroindustrial: fabrique circuitos/engrenagens, secador e extratora. Secador processa arroz/erva-mate em lote e produz chapas isoladas com aço e farelo. Extratora produz mais óleo por soja que a prensa manual.
5. Voltaico: refinaria processa Voltaite com óleo. Prepare circuitos avançados e sintetizador. Matriz no primeiro slot, amostra de minério bruto no segundo; cada lote precisa de 160.000 E e conserva a amostra. Use vários geradores ou energia acumulada para atender 400 E/t.

## Conexões e montagem

O controlador fica na base esquerda ao olhar a frente. Energia elétrica e funis ligam nele; vapor conecta em qualquer parte industrial compatível. Funis acima/lados inserem; abaixo extraem produtos, resíduos e baldes vazios. As outras partes abrem o mesmo menu. Quebrar qualquer parte desmonta e devolve o inventário uma vez.

Caldeira/fundição/turbina ocupam base 2×2; serraria/extratora, 3×2; laminador, 2×1; secador/refinaria/sintetizador, 3×3. Caldeira tem altura 3, secador 4, refinaria/sintetizador 3 e demais 2. A colocação exige espaço livre para o volume inteiro.

## Veículos (desde a 0.6.1)

Todos usam óleo vegetal como combustível: insira no slot dedicado do painel (ou clique com o balde/óleo na máquina). O painel exige estar perto da máquina; os botões são autoritativos no servidor.

1. Trator SNC 75 + Plantadeira SNC 75-P: use o item num bloco livre para posicionar; o item usado sobre a máquina já posicionada abre o painel (óleo + tanques de sementes por fileira). Clique com a mão vazia para montar e dirija com W/A/S/D; avançar em solo arado semeia três fileiras, cada uma consumindo do próprio tanque. Elevação da plantadeira no painel para transportar sem semear. Engate: pare com a plantadeira atrás do trator e use o botão de engate — ela passa a semear dos próprios tanques e acompanha soldada; desengatar preserva as sementes.
2. Colheitadeira SNC 90: painel com óleo e tanque de grãos de 27 slots. Avançando com a plataforma abaixada, corta só lavouras maduras nas três fileiras e guarda no tanque; tanque cheio pausa sem destruir nada. Sem óleo o motor morre em silêncio; sem motorista ela desacelera em marcha lente.
3. Carreta Graneleira SNC 90-C: posicione atrás da colheitadeira e engate pelo painel dela (o painel mostra o estado do engate). O botão de descarga transfere o grão com conservação exata — carreta cheia, o restante fica no tanque. Desengatada, a carreta fica no mundo com a carga; clique nela para abrir os 15 slots e descarregar à mão. Receita: 6 aço + barril + 2 ferro + carrinho de mina.
4. Mercadão: estrutura gerada no mundo — ~3× frequente e em 18 biomas (taigas, floresta escura e pântanos incluídos). **Requer 0.6.5+**: versões anteriores tinham um bug que impedia o registro da estrutura (nunca nascia). Localize em chunks nunca explorados com `/locate structure snc_energies:mercadao` (sem raio: esta versão do comando não aceita). Salão fechado com porta 2×2, letreiro pintado e vitrines de vidro; produtos expostos minguam com o estoque do dia (reposição às 07h). Compre pelo balcão — o servidor debita a carteira do Adventures e entrega o item. O balcão também é craftável (tábuas de pinheiro + esmeralda + barril). Prévia 3D em `previews/mercadao.html` (ou `mercadao-offline.html`).

## Adventures e limites desta versão

Bagaço de cana alimenta o fogão e a caldeira. Bebidas, comércio e UV preservam as regras do Adventures; as compras do Mercadão debitam a carteira do Adventures, sem segunda carteira. Motores para o Adventures e UV elétrico ainda não existem. O ramo de erva-mate é opcional e está explicado no caderno.

Os testes automatizados usam mundos próprios. As evidências ficam em `verification/`; não substituem avaliação de balanceamento no survival.
