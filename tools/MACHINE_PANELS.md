# Painéis de máquinas

`generate_machine_panels.py` produz `assets/snc_energies/machine_panels.json`, traduções e `previews/paineis.html`. Também é chamado pelo gerador industrial. O JSON é a fonte única das posições de slots e dos desenhos vetoriais; servidor, cliente e galeria usam os mesmos dados.

Cada equipamento tem um perfil próprio: paleta, material de fundo, esquema de processo, slots identificados e indicadores de energia/vapor/água/trabalho/progresso. Inventário e cabeçalho mantêm alinhamento comum. A área lógica é 256×238; não há nova textura raster de GUI. Retângulos, linhas e anéis são desenhados na escala do jogo. As texturas 3D continuam 128×128.

Os sete índices internos dos equipamentos industriais são preservados, inclusive nos slots sem uso. Slots inativos ficam fora da apresentação e rejeitam inserção; somente os slots relevantes são desenhados. O tipo de menu identifica a máquina desde o pacote de abertura, sem esperar a sincronização dos dados para posicionar slots. Os registros antigos continuam presentes.

Moinho e prensa têm tipos de menu próprios e posições diferentes; ambos preservam a ação autoritativa de manivela no servidor. O cubo de energia tem um painel novo de consulta, sem inventário adicional nem alteração da energia.

Após mudar o gerador, executar `python tools/generate_machine_panels.py`. Após alterar geometria industrial, executar `python tools/generate_industry.py`; o sintetizador tem anéis de contenção, dois suportes e cristal suspenso. A refinaria permanece com o modelo anterior.

Validação: o teste de cliente abre os 16 painéis, aciona o moinho, espera sua produção sincronizada e compara slots industriais com o perfil. Screenshots ficam no diretório do teste e são preservados em `verification/` antes do build limpo. As barras da galeria usam preenchimento ilustrativo; dentro do jogo recebem exclusivamente dados sincronizados da máquina.
