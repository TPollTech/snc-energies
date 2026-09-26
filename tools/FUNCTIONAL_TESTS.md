# Validação de SNC Energies 0.2.0

Java 25; executar da raiz do projeto. Perfis têm mundos separados e não abrem saves do usuário.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
.\gradlew.bat --init-script tools/functional-test.init.gradle clean build runServer --console plain
.\gradlew.bat --init-script tools/functional-test.init.gradle '-PadventuresJar=../intoxicantes-mod/build/libs/intoxicantes-1.2.50.jar' runServer runClientGameTest --console plain
```

O primeiro servidor usa `run/functional-standalone`; o segundo, `run/functional-adventures`. Ambos usam 127.0.0.1:25577 e devem rodar sequencialmente. O parâmetro Adventures exige um JAR existente. Configuração inicial vem de `run/functional-test/server.properties` e `eula.txt`, preparados nesta máquina. O servidor encerra ao concluir. A tarefa falha se não encontrar `FUNCTIONAL TESTS PASSED` ou se houver `FUNCTIONAL TESTS FAILED`.

Cobertura de servidor: máquinas antigas, rede elétrica e conservação de energia; multiblocos em quatro orientações, obstrução, controlador único, remoção com um drop e retorno de inventário; receitas manuais e industriais, saída cheia/componentes diferentes, persistência parcial e recursos exatos; água/vapor, baldes, turbina, amostra preservada na síntese; colheitas e erva-mate; presença/ausência e combustível do Adventures.

O cliente roda em `build/run/clientGameTest`, cria seu próprio mundo, interage com o moinho por clique, envia a ação de manivela, espera o processamento no servidor e a saída sincronizada no menu. Abre a caldeira e o guia. Evidências visuais em `screenshots`; êxito identificado por `CLIENT FLOW PASSED`. Código dos testes não entra no JAR de distribuição.

Logs finais e screenshots copiados para `verification/` antes de qualquer próximo clean. A limitação de leitura dos contadores Perflib/OSHI do Windows aparece no relatório de sistema, sem impedir os testes do mod.

Limites: não há validação visual de todos os modelos em todas as orientações, nem teste multiplayer prolongado ou migração de cada save antigo. Automação de bebidas, UV elétrico, comércio e transporte avançado não são funcionalidades implementadas nesta versão.
