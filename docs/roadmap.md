# Roadmap · Hackathon One Sentiment API

Este documento descreve, em linhas gerais, a evolução planejada do projeto.  
Ele não é uma agenda rígida, mas um guia para:

- organizar entregas;
- comunicar prioridades;
- evitar que o escopo saia do controle.

---

## 1. Objetivo geral

Entregar um MVP funcional que:

- receba comentários de clientes (compradores);
- analise automaticamente o sentimento via modelo de ML;
- registre resultados em banco;
- permita que o vendedor visualize estatísticas, comentários e notificações críticas.

---

## 2. Fases do projeto

### Fase 0 – Preparação e alinhamento

**Objetivo:** levantar o escopo e preparar o terreno.

- [x] Ler e entender a **Proposta Oficial do Hackathon**.
- [x] Definir o nome do projeto: *Hackathon One Sentiment API*.
- [x] Especificar requisitos (docs/requisitos.md).
- [x] Definir arquitetura de alto nível (docs/arquitetura.md + diagramas UML).
- [x] Modelar o banco de dados (docs/database.md + ddl/schema-postgres.sql).
- [x] Estruturar o repositório em pastas (backend, datascience, frontend, docs, ddl).

**Entregáveis principais:**

- Documentos em `docs/`.
- Estrutura inicial de diretórios.
- Diagrama de arquitetura e ER.

---

### Fase 1 – MVP técnico de ponta a ponta

**Objetivo:** provar o fluxo principal de ponta a ponta, mesmo que de forma simples.

**Escopo mínimo:**

- Data Science:
    - Notebook com EDA básica e treino de um modelo simples (TF-IDF + Regressão Logística).
    - Exportação do modelo em `.pkl`.
- ML Service:
    - Microserviço FastAPI com endpoint `POST /predict` que:
        - receba `{ "text": "..." }`;
        - devolva `{ "label": "...", "probability": ... }`.
- Backend (Spring Boot):
    - Endpoint que receba texto (ex.: `/api/v1/sentiment` ou `/api/v1/comentarios`) e:
        - valide a entrada;
        - chame o ML service;
        - retorne a previsão.
- Frontend:
    - Tela simples (pode ser só uma) que:
        - permita enviar um texto;
        - exiba o sentimento retornado (para fins de teste interno na Fase 1).

> Observação: na fase seguinte, a resposta completa de sentimento passa a ser usada só internamente, sem aparecer para o comprador.

**Critério de “feito” (MVP técnico):**

- É possível:
    - subir DS/ML;
    - subir backend;
    - enviar texto e ver o sentimento de volta, via Postman ou interface simples.

---

### Fase 2 – Domínio completo: Comprador, Vendedor, Banco e Notificações

**Objetivo:** sair do “toy model” e implementar o domínio completo de e-commerce simplificado.

**Principais itens:**

- Banco:
    - Tabelas `cliente`, `produto`, `comentario`, `resultado_analise`, `notificacao`, `log_evento`, `modelo_ml`, `dataset_registro`.
- Backend:
    - Implementar:
        - `POST /api/v1/comentarios` (entrada do comentário);
        - criação de `resultado_analise` após chamada ao ML;
        - criação de `notificacao` em caso de comentários críticos;
        - `GET /api/v1/stats`, `GET /api/v1/comments`, `GET /api/v1/notificacoes`, `PATCH /api/v1/notificacoes/{id}/ler`, `GET /api/v1/export`.
    - Logging básico nos principais fluxos.
- Frontend:
    - `login.html` diferenciando Cliente - Comprador e Cliente - Vendedor;
    - `comprador.html` com:
        - lista de produtos;
        - detalhe de produto;
        - envio de comentário;
    - `vendedor.html` com:
        - cadastro de produtos;
        - dashboard com stats e gráficos básicos;
        - lista de comentários com sentimento;
        - lista de notificações.
- Regras de negócio:
    - Comprador **não vê** o sentimento classificado;
    - Vendedor vê estatísticas e comentários com sentimento;
    - Notificações são criadas apenas para comentários negativos críticos.

**Critério de “feito” (MVP funcional):**

- Fluxo completo:
    1. Vendedor cadastra produto.
    2. Comprador envia comentário.
    3. Sistema classifica sentimento e registra resultado.
    4. Comentário crítico gera notificação para o vendedor.
    5. Vendedor vê dashboard, comentários e notificações.

---

### Fase 3 – Qualidade, Testes e Documentação

**Objetivo:** dar “cara de produção” ao que foi feito.

**Atividades principais:**

- Testes:
    - Implementar testes unitários básicos no backend (ex.: services principais).
    - Implementar ao menos um teste de integração backend ↔ ML.
    - Documentar cenários de teste em `docs/test-strategy.md`.
    - Registrar o que foi de fato testado em `docs/test-report.md`.
- Segurança:
    - Revisar se senhas/tokens não estão no Git.
    - Confirmar que o projeto está utilizando `.gitignore` adequado.
- Documentação:
    - Revisar `docs/` para garantir consistência:
        - requisitos ↔ arquitetura ↔ banco ↔ frontend ↔ testes → tudo batendo;
    - Atualizar `docs/traceability-matrix.md`.

**Critério de “feito”:**

- Existe:
    - uma rota clara para subir o projeto localmente;
    - uma documentação coerente;
    - alguns testes automatizados rodando.

---

### Fase 4 – Extras (opcional / pós-hackathon)

Itens para além do escopo do hackathon, mas que o projeto já está preparado para receber:

- **CI/CD**:
    - GitHub Actions para rodar testes a cada PR;
    - build automático do backend;
    - publicação de imagens Docker.
- **Dockerização completa**:
    - `docker-compose.yml` subindo:
        - banco (PostgreSQL),
        - backend (Spring Boot),
        - ML service (FastAPI),
        - opcionalmente um servidor para o frontend.
- **Melhorias no modelo**:
    - incorporar análises multi-classe (Positivo / Negativo / Neutro);
    - retrain com novos dados reais.
- **Autenticação**:
    - adicionar login real (JWT, sessões, etc.);
    - controle de acesso por perfil (Comprador/Vendedor/Admin).

---

## 3. Priorização

Para efeito de hackathon, a ordem de prioridade pode ser resumida assim:

1. **Fase 1 + Fase 2 (núcleo)**  
   – fluxo ponta a ponta comprador → ML → vendedor.

2. **Fase 3 (qualidade)**  
   – testes básicos + documentação bem amarrada.

3. **Fase 4 (extras)**  
   – apenas se sobrar tempo ou para continuação do projeto depois.

---

## 4. Atualização deste roadmap

Este documento deve ser revisitado quando:

- um novo requisito relevante for adicionado;
- escopo do projeto for alterado;
- alguma fase for considerada fechada.

A ideia é que o roadmap seja um guia vivo, e não só uma foto do momento do hackathon.


---

---

---

# Hackathon One Sentiment API · Roadmap e Cronograma

> Este documento descreve o plano de evolução do projeto **Hackathon One Sentiment API**, com foco em organização por fases, prioridades e entregas.  
> A ideia não é engessar o time, mas dar visão clara do “hoje → MVP → evolução”.

---

## 1. Objetivo do Roadmap

Este roadmap existe para:

- organizar o trabalho das equipes (Backend, Frontend, Data Science, DevOps);
- garantir que o **MVP** (produto mínimo viável) seja entregue dentro do tempo do hackathon;
- registrar **o que já foi feito** e **o que ainda está em aberto**;
- servir de referência para futuras evoluções após o hackathon.

Ele não é um contrato rígido, mas uma **linha-guia**. Pequenas mudanças são esperadas, desde que alinhadas com:

- a **Proposta oficial** do desafio;
- os **diagramas de arquitetura e fluxo**;
- os **documentos de requisitos**.

---

## 2. Visão Geral de Fases

Dividimos o roadmap em três grandes momentos:

1. **Fase 0 – Fundamento e alinhamento**  
   Organização de repositório, arquitetura, base de dados, documentação inicial.

2. **Fase 1 – MVP funcional (Hackathon)**  
   Integração mínima entre **Frontend → API Spring Boot → Microserviço ML → Banco**, com testes básicos.

3. **Fase 2 – Pós-MVP / Evolução**  
   Melhorias, refinamentos e recursos opcionais sugeridos na Proposta (stats avançadas, batch, multi-idioma, etc.).

---

## 3. Fase 0 – Fundamento e Alinhamento

> Status: **Praticamente concluída** (restam apenas ajustes finos e revisão).

### 3.1. Metas principais

- Definir **escopo funcional** (MVP) e requisitos.
- Consolidar o **modelo de dados** e o **DDL** do banco PostgreSQL.
- Definir arquitetura de alto nível (Java + Python + Postgres + Frontend Web).
- Estruturar o repositório Git com pastas por responsabilidade.
- Criar base de documentação em `/docs`.

### 3.2. Entregas previstas

- ✅ `docs/requisitos.md`
- ✅ `docs/arquitetura.md`
- ✅ `docs/database.md`
- ✅ `docs/frontend.md`
- ✅ `docs/test-strategy.md`
- ✅ `docs/security.md`
- ✅ `docs/devops-deploy.md`
- ✅ `docs/ml-model-card.md`
- ✅ `docs/traceability-matrix.md`
- ✅ `docs/dataset.md`
- ✅ `docs/coding-standards.md`
- ✅ ADRs:
    - `docs/adr/ADR-001-microservico-ml.md`
    - `docs/adr/ADR-002-comprador-sem-sentimento.md`
    - `docs/adr/ADR-003-estrutura-repositorio.md`
- ✅ Diagramas PlantUML em `docs/uml/`
    - Arquitetura (C4 – contexto/container)
    - ER do banco
    - Fluxo “Comprador envia comentário”
    - Fluxo “Vendedor recebe notificação e lê”
    - Fluxos de dashboard e export
    - Casos de uso, navegação de telas, integração entre equipes

### 3.3. Critérios de conclusão (Definition of Done)

Fase 0 é considerada concluída quando:

- O time consegue responder **“como o sistema funciona”** apenas olhando documentação + diagramas.
- O DDL (`ddl/schema-postgres.sql`) está consistente com o ER e com as entidades planejadas.
- O repositório está organizado de acordo com `docs/arquitetura.md` e ADR-003.

---

## 4. Fase 1 – MVP Funcional (Hackathon)

> Objetivo: entregar um MVP que atenda à Proposta, com fluxo **fim a fim** funcionando.

### 4.1. Metas principais

- Backend Java Spring Boot:
    - Endpoints principais:
        - `POST /api/v1/comentarios` ou `/sentiment` (entrada do texto)
        - `GET /api/v1/stats`
        - `GET /api/v1/comments`
        - `GET /api/v1/produtos`
        - `GET /api/v1/notificacoes`
        - `PATCH /api/v1/notificacoes/{id}/ler`
        - `GET /api/v1/export`
    - Integração com o **microserviço ML** (`POST /predict`).
    - Persistência em PostgreSQL com o schema definido.
    - Logs relevantes em `log_evento`.

- Microserviço ML (FastAPI/Flask em Python):
    - Carregar modelo `.pkl` (TF-IDF + Logistic Regression).
    - Expor `POST /predict` (contrato documentado).
    - Retornar JSON com `{ label, probability }`.

- Frontend Web:
    - `login.html` com escolha de perfil (Comprador/Vendedor).
    - `comprador.html` listando produtos e permitindo enviar avaliação.
    - `vendedor.html` mostrando:
        - stats simples (totais de positivos/negativos/neutros);
        - lista de comentários;
        - notificações (pendentes/lidas);
        - botão de exportar JSON.

- Banco de Dados:
    - PostgreSQL rodando localmente.
    - Script de criação de schema funcionando (`ddl/schema-postgres.sql`).
    - Testes manuais: inserts básicos + selects.

- Qualidade mínima:
    - Pelo menos alguns testes unitários e de integração:
        - Java: controllers/services principais.
        - Python: função/endpoint de previsão (`/predict`).
    - Testes de fluxo manual com Postman/cURL e via UI.

### 4.2. Backlog de Fase 1 (alto nível)

| ID   | Item                                              | Responsável principal | Dependências                      |
|------|---------------------------------------------------|-----------------------|-----------------------------------|
| F1-1 | Criar entidades JPA e repositórios básicos        | Backend               | `ddl/schema-postgres.sql`         |
| F1-2 | Implementar endpoint `/api/v1/comentarios`        | Backend               | F1-1, contrato com ML             |
| F1-3 | Implementar chamada HTTP para `POST /predict`     | Backend               | ML expondo `/predict`             |
| F1-4 | Implementar persistência de `resultado_analise`   | Backend               | F1-1, F1-2                        |
| F1-5 | Implementar lógica de criação de `notificacao`    | Backend               | F1-4                              |
| F1-6 | Implementar `/api/v1/stats`, `/comments`, `/export` | Backend             | F1-1, F1-4                        |
| F1-7 | Implementar `POST /predict` no microserviço ML    | DS / ML               | Modelo `.pkl` no `ml_service/model` |
| F1-8 | Frontend `login.html` + integração com API        | Frontend              | Endpoints de cliente (ou mock)    |
| F1-9 | Frontend `comprador.html` (listar + avaliar)      | Frontend              | `/produtos`, `/comentarios`       |
| F1-10| Frontend `vendedor.html` (dashboard + notificações + export) | Frontend | `/stats`, `/comments`, `/notificacoes`, `/export` |
| F1-11| Testes unitários/integrados mínimos               | Backend / DS          | Implementações principais         |
| F1-12| Atualizar documentação e exemplos de uso          | Todos                 | Funcionalidade concluída          |

### 4.3. Linha do tempo sugerida (hackathon)

> Os “dias” aqui são **lógicos**, não necessariamente dias reais.  
> Ajustar conforme o calendário do evento.

| Dia / Iteração | Foco principal                                                                 |
|----------------|-------------------------------------------------------------------------------|
| Dia 1          | Ajustes finais de requisitos, arquitetura, banco e contratos JSON            |
| Dia 2          | Implementação backend básico (entidades, repositórios, `/comentarios`)      |
| Dia 3          | Microserviço ML `/predict` + integração backend ↔ ML                        |
| Dia 4          | Frontend (login + comprador + vendedor básico)                              |
| Dia 5          | Notificações, dashboard, export JSON                                        |
| Dia 6          | Testes, correções, polimento, prints, validação com Postman/UI             |
| Dia 7          | Ajustes finais em documentação e preparação da apresentação/demonstração    |

### 4.4. Critérios de “MVP pronto”

Consideramos o MVP pronto quando:

- Um **Comprador** consegue:
    - acessar a UI,
    - escolher um produto,
    - enviar um comentário,
    - receber mensagem de confirmação.
- O comentário chega à API, é salvo no banco, analisado pelo modelo de ML, e um `resultado_analise` é criado.
- Um **Vendedor** consegue:
    - acessar o dashboard,
    - ver estatísticas de sentimentos,
    - ver comentários,
    - ver notificações de comentários negativos críticos,
    - exportar JSON com os feedbacks.
- Os principais fluxos documentados em `docs/uml/*.puml` batem com o comportamento real.
- README principal está atualizado com **como rodar** e **como testar**.

---

## 5. Fase 2 – Pós-MVP / Evolução

> Itens opcionalmente implementáveis dentro ou depois do hackathon, conforme tempo.

### 5.1. Melhorias técnicas

- Autenticação e autorização básicas (ex.: JWT ou sessão simples).
- Logging mais estruturado (JSON, correlação de requisições).
- Melhor tratamento de erros (padrão único de erro em JSON).

### 5.2. Recursos opcionais da Proposta

- Endpoint `GET /stats` mais avançado:
    - filtro por período, produto, perfil de cliente.
- Processamento em lote:
    - endpoint que recebe CSV ou lista de textos e retorna previsões em lote.
- Explicabilidade:
    - retornar palavras mais influentes na previsão (coeficientes da regressão logística).
- Multi-idioma:
    - suporte a PT/ES (português / espanhol).
- Ajuste de limiar de probabilidade para definir “crítico”.

### 5.3. DevOps e Observabilidade

- Dockerização completa (backend, ml_service, db).
- `docker-compose.yml` para subir todo o ambiente local.
- GitHub Actions rodando:
    - build de backend,
    - testes do ML,
    - lint no frontend.
- Saúde e monitoramento:
    - endpoints de health-check,
    - dashboard simples de métricas (pode ser só log + scripts).

---

## 6. Riscos e Dependências

### 6.1. Dependências chave

- Microserviço ML funcionando (arquivo `.pkl` no lugar certo).
- Banco de dados PostgreSQL acessível e com schema carregado.
- Backend configurado corretamente para acessar ML e DB.
- Contratos JSON **claros e estáveis** (descritos em `docs/requisitos.md` e `docs/traceability-matrix.md`).

### 6.2. Principais riscos

- Atraso na entrega do modelo de ML ou do microserviço `/predict`.
- Divergência entre o que o frontend espera e o que o backend devolve (nomes de campos, formatos).
- Problemas de ambiente (porta ocupada, Docker, firewall, etc.) na hora da demo.

### 6.3. Mitigações

- Trabalhar com **mocks temporários** no frontend e no backend enquanto algum serviço não existe ainda.
- Deixar **contratos e exemplos de JSON** bem documentados (já feito nos docs).
- Testar cedo o fluxo ponta-a-ponta em ambiente local, mesmo que “feio”, antes de se preocupar com estética.

---

## 7. Como manter o roadmap vivo

- Atualizar este arquivo (`docs/roadmap.md`) ao final de cada **reunião de alinhamento**.
- Marcar o que foi concluído com:
    - checkboxes (`- [x] feito`) se quiser,
    - ou comentários na própria tabela.
- Manter coerência com:
    - `docs/test-report.md` (o que foi realmente testado),
    - `docs/traceability-matrix.md` (requisitos cobertos),
    - ADRs (decisões tomadas ao longo do caminho).

Assim, o roadmap deixa de ser só um “plano inicial” e vira o **histórico real de evolução** do Hackathon One Sentiment API.
