# Proposta inicial do hackathon

Esta pasta guarda o desenho inicial do hackathon, de dezembro de 2025: proposta, requisitos, arquitetura, banco de dados, frontend, testes, segurança, deploy e os diagramas em `uml/` e `diagrams/`. O sistema implementado é diferente e menor do que o descrito aqui.

A descrição do sistema real está no [README da raiz](../../README.md), nos [ADRs](../adr/) e no [model card](../ml-model-card.md).

## Caminhos citados nos documentos

Os caminhos citados dentro destes documentos são os da época, como `docs/uml/02-diagrama-de-er-banco.puml`. Hoje esses arquivos ficam em `docs/proposta-inicial/`, como `docs/proposta-inicial/uml/02-diagrama-de-er-banco.puml`.

## Ajustes posteriores

`database.md` e `requisitos.md` receberam ajustes em setembro de 2026 para o contrato binário de sentimento (`POSITIVO` e `NEGATIVO`, sem `NEUTRO`), registrados no ADR-004 (seção 5.4). Os dois continuam descrevendo o desenho inicial.

## Diferenças entre o desenho e o código

| Tema | Desenho inicial | Código |
| :--- | :--- | :--- |
| Banco de dados | Oito tabelas em `database.md`: `cliente`, `produto`, `comentario`, `modelo_ml`, `resultado_analise`, `notificacao`, `dataset_registro` e `log_evento`. | Uma entidade, `AnaliseResultado`, gravada na tabela `analise_resultado`. |
| Endpoints | `POST /api/v1/comentarios` para o envio de comentários (`requisitos.md`, RF06). | A rota não existe na implementação (ADR-004, seção 6.3). A API expõe `POST /api/v1/sentiment`, `POST /api/v1/sentiment/batch`, `GET /api/v1/stats` e `GET /api/v1/health`. |
| Usuários e telas | Comprador e vendedor, com tela de login, cadastro de produtos, notificações, dashboard do vendedor e exportação em JSON (`requisitos.md` e `frontend.md`). | Uma página, `backend/sentiment-backend/src/main/resources/static/index.html`, servida pelo Spring Boot, para analisar um texto e ver as estatísticas. |
