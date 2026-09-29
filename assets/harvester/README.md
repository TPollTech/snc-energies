# SNC 90 — colheitadeira com plataforma de corte

Modelo original para SNC Energies, criado em 26/09/2026. **Etapa de modelagem:** este pacote ainda não registra uma entidade no Minecraft. Colher, consumir combustível e descarregar grão dependem de implementação posterior.

## Entrega

- `snc-90-colheitadeira.bbmodel`: modelo editável do Blockbench, formato genérico, com texturas embutidas e hierarquia de peças.
- `snc-90-colheitadeira.glb`: glTF 2.0 binário, texturas embutidas, pivôs preservados e animação demonstrativa de rodas, reel, rotor, ventoinhas e picador.
- `harvester-model.json`: geometria canônica usada na prévia e nas exportações.
- `materials.json` e `textures/`: 21 materiais com imagens de 128×128.
- `../../previews/colheitadeira.html`: estúdio 3D local (mesmo padrão do Estúdio do SNC 75) com controles e downloads; `colheitadeira-offline.html` é a versão autocontida.

Modelagem procedural compartilhada: `tools/generate_harvester.py` e `tools/generate_harvester_textures.py` usam as bibliotecas extraídas do golden reference (`tools/vehicle_library.py`, `tools/vehicle_texture_kit.py`, `tools/vehicle_verify_library.py`). Verificação: `tools/verify_harvester.py` → relatório em `verification/harvester-model-validation.json`.

## Conteúdo modelado

Chassi com longarinas, eixo dianteiro recuado sob a cabine, eixo traseiro com reduções finais, contrapeso de engate e caixa de ferramentas; casa de alimentação com ripas, janelas laterais e porta de pedras; rotor axial com barras, hélices e cones, alojamento aberto em placas, côncavos em grade, batedor; peneiras superior/inferior com bandeja e ventilador; picador de palha com facas em três estações e rabichos; tanque de grão com paredes vincadas, decalque de palha/espigas, passarela e escotilha; tubo de descarga dobrável com rosca, colar de dobra e bico com flapa e manivela; motor lateral-direito (bloco, tampa de válvulas, radiador com grade e ventoinha a correia, bateria com cabo, reservatório hidráulico, filtro de ar em concha); escapamento alto à direita com aba de chuva; cabine fechada envidraçada sobre a casa de alimentação com interior completo (banco com suspensão e apoio de cabeça, volante inclinado, console com três alavancas, monitor, acelerador, pedais, rádio); escada e plataforma de entrada à esquerda com corrimãos; plataforma de corte de 4 blocos com barra de corte, dedos, rosca de adução, divisor de leiras, sapatas e engate rápido; reel com três discos, varas, tineiras e motor de acionamento.

Assimetrias reais preservadas: escapamento, filtro e motor no lado direito; tubo de descarga, escada e corrimãos no lado esquerdo; giroflex no canto traseiro-direito do teto.

## Escala e articulação

16 unidades = 1 bloco; Y para cima e frente em −Z. Coordenadas `from`, `to` e pivôs `origin` são absolutas na pose de repouso. Rotação Euler **ZYX**, em graus. O GLB aplica escala 1/16 apenas na raiz. O conjunto ocupa aproximadamente 4,0 × 3,4 × 5,3 blocos (largura × altura × comprimento com a plataforma); dimensões visuais, ainda sem colisão no jogo.

| Grupo | Movimento na prévia |
|---|---|
| `front_*_wheel`, `rear_*_wheel` | Rolagem em X (dianteiras ~1,7× mais rápidas) |
| `reel`, `cooling_fan`, `cleaning_fan`, `straw_chopper` | Rolagem de mecanismos em X |
| `rotor` | Giro em Z (longitudinal), exercido na prévia |
| `steering_wheel` | Giro local (fixo na inclinação de repouso de 48°) |
| `hood` | Abertura de 55° em X |
| `header` | Elevação de +26° em X (transporte) |
| `unloading_auger` | Erguer de −38° em X (transporte → descarga) |
| `spout` | Dobra de +150° em X (recolhe o bico sobre o tubo externo) |
| `header` | Visibilidade independente (desacoplar plataforma) |

`locators` no JSON documenta banco do operador, engate da plataforma, ponta do bico, eixos, baia do motor, pivô do tubo e bico dobrado. A animação do GLB demonstra o rig; não é simulação de colheita.

## Abrir a prévia e regenerar

Na raiz do projeto, com o servidor local (`.venv-textures/Scripts/python.exe -m http.server 8765 --bind 127.0.0.1`): `http://127.0.0.1:8765/previews/colheitadeira.html`. A versão offline abre por duplo clique no arquivo. Regeneração:

```powershell
.venv-textures/Scripts/python.exe tools/generate_harvester_textures.py
.venv-textures/Scripts/python.exe tools/generate_harvester.py
.venv-textures/Scripts/python.exe tools/verify_harvester.py
.venv-textures/Scripts/python.exe tools/package_harvester_preview.py
```

## Integração posterior ao aceite visual

Mesmos termos do SNC 75: entidade dirigível no servidor, renderer/modelo apenas no `client`, registros centralizados, sincronização e persistência próprias, combustível (óleo vegetal como candidato), colheita respeitando terreno e permissões. A plataforma é destacável via grupo `header`. Nenhum registro existente foi alterado, nenhum build foi executado e nenhum JAR foi instalado nesta etapa.

## Verificação

`tools/verify_harvester.py` reporta **passed sem avisos** (6 grupos de verificações; 15 cenários de articulação sem interseções). Conferência visual da prévia é distinta de teste no Minecraft. O arquivo Blockbench foi validado estruturalmente, mas a abertura no editor Blockbench ainda precisa de confirmação.
