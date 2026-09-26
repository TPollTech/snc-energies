# SNC 75 — trator aberto com plantadeira

Modelo original para SNC Energies, criado em 26/09/2026. **Etapa de modelagem:** este pacote ainda não registra uma entidade no Minecraft. Dirigir, consumir combustível e plantar no mundo dependem de implementação posterior.

## Entrega

- `snc-75-trator.bbmodel`: modelo editável do Blockbench, formato genérico, com texturas embutidas e hierarquia de peças.
- `snc-75-trator.glb`: glTF 2.0 binário, texturas embutidas, pivôs preservados e animação demonstrativa de rotação das rodas.
- `tractor-model.json`: geometria canônica usada na prévia e nas exportações.
- `materials.json` e `textures/`: 14 materiais com imagens de 128×128.
- `../../previews/trator.html`: estúdio 3D local com controles e downloads.

Cabine aberta com banco e capota, conforme escolha do dono. Inclui chassis, eixos, contrapesos, motor sob capô articulado, pneus com cravos em V, paralamas, faróis, lanternas, giroflex, escapamento, filtro de ar, degraus, pedais, painel, volante, câmbio, comandos hidráulicos, retrovisores e estrutura traseira. A plantadeira destacável tem três reservatórios, dosadores, tubos, discos sulcadores, rodas de apoio e rodas compactadoras.

## Escala e articulação

16 unidades = 1 bloco; Y para cima e frente em −Z. Coordenadas `from`, `to` e pivôs `origin` são absolutas na pose de repouso. Rotação Euler **ZYX**, em graus. O GLB aplica escala 1/16 apenas na raiz. O conjunto ocupa aproximadamente 3,5 × 3,1 × 5,8 blocos (largura × altura × comprimento); essas dimensões são visuais e ainda não definem colisão no jogo.

| Grupo | Movimento na prévia |
|---|---|
| `front_left_steering`, `front_right_steering` | Direção em Y, até ±28° |
| `front_*_wheel`, `rear_*_wheel` | Rolagem em X |
| `steering_wheel` | Giro local em Z, com inclinação de repouso |
| `hood` | Abertura de 55° em X |
| `canopy` | Visibilidade independente |
| `planter` | Engate e elevação de −23° em X |
| `planter_*_wheel`, `row_*_press_wheel` | Rolagem em X |

`locators` no JSON documenta banco do motorista, engate, eixos e três pontos de plantio. As linhas ficam a um bloco de distância entre si. A animação do GLB demonstra o rig; não é uma simulação de deslocamento. A prévia representa o levantamento como rotação do implemento e não como uma simulação de todos os elos hidráulicos.

## Abrir a prévia e regenerar

Na raiz do projeto, PowerShell:

```powershell
.venv-textures/Scripts/python.exe tools/generate_tractor_textures.py
.venv-textures/Scripts/python.exe tools/generate_tractor.py
.venv-textures/Scripts/python.exe tools/verify_tractor.py
.venv-textures/Scripts/python.exe -m http.server 8765 --bind 127.0.0.1
```

Abra `http://127.0.0.1:8765/previews/trator.html`. Arraste para orbitar, use a roda do mouse para zoom e os controles para inspecionar as peças. A prévia usa Three.js 0.170.0 com licença MIT em `previews/vendor/THREE-LICENSE.txt`, armazenado localmente; não depende de CDN em execução.

## Integração posterior ao aceite visual

Criar uma entidade dirigível no servidor e renderer/modelo apenas no source set `client`, mantendo registros centralizados. Movimento, combustível, inventário de sementes, engate e plantio devem ser sincronizados e persistidos. O óleo vegetal existente é candidato a combustível, ainda sem decisão de balanceamento. O plantio deve respeitar terreno arado, espaço livre, permissões de edição e consumo de uma semente por planta realmente colocada; arroz, soja e mate já possuem sementes/culturas no projeto.

A futura colheitadeira pode compartilhar escala, materiais, padrão de pivôs e pontos de montagem. Seu modelo e sua lógica de colheita ainda não fazem parte desta entrega. Nenhum registro existente foi alterado, nenhum build foi executado e nenhum JAR foi instalado nesta etapa.

## Verificação

O relatório estrutural fica em `verification/tractor-model-validation.json`; capturas do navegador em `verification/tractor/`. Conferência visual da prévia é distinta de teste no Minecraft. O arquivo Blockbench foi validado estruturalmente, mas a abertura no editor Blockbench ainda precisa de confirmação.
