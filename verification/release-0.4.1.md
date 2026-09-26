# SNC Energies 0.4.1 — marco do fluxo de releases

## O que mudou
- **Nenhuma mudança de gameplay**: o 0.4.0 passou no teste do dono sem nada a ajustar.
- Marco formal do novo fluxo de publicação (Regra de Ouro 8): release gerada pelo GitHub Actions no push da tag `v0.4.1`, com o jar e o `SHA256SUMS.txt` anexados automaticamente.
- CI: cache de Gradle/Loom compartilhado entre os workflows; novo workflow **Build** na `main` e em PRs (artifact do jar a cada push, comentário automático em PR quebrado, issue de rastreio quando a main quebra e fecha quando volta ao verde).

## Notas
- O jar anexado é buildado pelo CI a partir da tag `v0.4.1`; internamente o `fabric.mod.json` ainda declara 0.4.0 — a próxima release (0.5.0, já em preparo) sai com a versão alinhada ao gradle.properties.
- Integridade: conferir o `SHA256SUMS.txt` anexado.
- Requisitos: Minecraft **26.3** · Fabric Loader **0.19.5** · Fabric API **0.161.0+26.3** · Java 25.
- Instalação: copie o jar para `mods` (remova versões anteriores); `fabric-api-0.161.0+26.3.jar` é obrigatório.
