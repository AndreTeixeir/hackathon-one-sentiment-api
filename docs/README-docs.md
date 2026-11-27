# Documentação do Projeto

Índice da pasta `docs/`.

## Documentação atual

- [ADR-001](adr/ADR-001-microservico-ml.md) (28/12/2025): o modelo de ML fica num microserviço Python, que a API Java chama por HTTP. Parcialmente superado pelo ADR-004 quanto ao enum de sentimento.
- [ADR-002](adr/ADR-002-comprador-sem-sentimento.md) (28/12/2025): o comprador não vê o sentimento previsto pelo modelo na interface.
- [ADR-003](adr/ADR-003-estrutura-repositorio.md) (28/12/2025): um único repositório, com pastas organizadas por responsabilidade (backend, datascience, frontend, docs, ddl).
- [ADR-004](adr/ADR-004-modelo-treinado-e-contrato-binario.md) (19/09/2026): carregamento do modelo treinado, fallback observável pelo `/health` e contrato binário de sentimento, sem a classe Neutro.
- [ADR-005](adr/ADR-005-dominio-e-https-via-caddy-oci-rag.md) (29/09/2026): domínio próprio com HTTPS, terminado pelo Caddy de outra VM da conta (`oci-rag`).
- [ADR-006](adr/ADR-006-cors-restrito-nginx-removido-e-portas-expostas.md) (22/10/2026): CORS restrito às origens da demo e do ambiente local, remoção do nginx antigo e portas do Postgres e do `ds-service` publicadas só no loopback.
- [ml-model-card.md](ml-model-card.md): ficha do modelo em uso (`TfidfVectorizer` + `LogisticRegression`, binário, treinado com avaliações do Hopi Hari), com limitações e modo de degradação.
- [deploy-oci.md](deploy-oci.md): guia de deploy na OCI Free Tier, da criação da VM ao deploy com `scripts/deploy-oci.sh`.
- [revisao-tecnica-2026-09.md](revisao-tecnica-2026-09.md): revisão técnica de 17/09/2026, que comparou documentação e implementação e concluiu que a API não usava o modelo treinado.
- [progresso/parte1.md](progresso/parte1.md): registro do Marco 1, a integração do backend com o serviço de Data Science pelo endpoint `POST /sentiment`.

## Proposta inicial

O desenho inicial do hackathon (requisitos, arquitetura, banco de dados, frontend, testes e diagramas) descreve um sistema diferente do implementado e está em [proposta-inicial/](proposta-inicial/).
