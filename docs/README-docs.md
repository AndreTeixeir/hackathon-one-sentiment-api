# Documentação do Projeto

Esta pasta reúne toda a documentação deste projeto: desde a proposta original do hackathon até detalhes de arquitetura, banco de dados, frontend, modelo de ML, testes e decisões técnicas.

A ideia é que qualquer pessoa nova no time (ou avaliador) consiga entender **o que o sistema faz, por que foi feito desse jeito e como ele é mantido** apenas lendo o conteúdo daqui.

---

## 1. Como a documentação está organizada

A estrutura do diretório `docs/`:

```text
docs/
├── README-docs.md           # Este arquivo
├── proposta-oficial.md      # Proposta do hackathon / visão de negócio
├── requisitos.md            # Requisitos funcionais e não funcionais (SRS)
├── arquitetura.md           # Arquitetura de alto nível e decisões técnicas
├── frontend.md              # Especificação da interface Web (login/comprador/vendedor)
├── database.md              # Modelo de dados, tabelas e regras de negócio do banco
├── ml-model-card.md         # “Ficha” do modelo de Machine Learning
├── test-strategy.md         # Estratégia e tipos de testes (API, ML, front, DB)
├── security.md              # Boas práticas de segurança, logs e privacidade
├── devops-deploy.md         # Execução local, containers e ideias de deploy
├── adr/                     # Architecture Decision Records (decisões arquiteturais)
│   ├── ADR-001-microservico-ml.md
│   ├── ADR-002-comprador-sem-sentimento.md
│   └── ADR-003-estrutura-repositorio.md
└── uml/                     # Diagramas em PlantUML
    ├── 01-diagrama-de-arquitetura-c4.puml
    ├── 02-diagrama-de-er-banco.puml
    ├── 03-diagrama-de-sequencia-comprador-envia-comentario.puml
    ├── 04-diagrama-de-sequencia-vendedor-recebe-notificacao.puml
    ├── 05-diagrama-de-sequencia-vendedor-publica-produto-e-comprador-avalia.puml
    ├── 06-diagrama-de-sequencia-vendedor-visualiza-dashboard.puml
    ├── 07-diagrama-de-navegacao-de-telas.puml
    ├── 08-diagrama-de-casos-de-uso.puml
    ├── 09-diagrama-de-integracao-equipes-modulos.puml
    ├── 10-diagrama-de-fluxo-banco-comentario-analise-notificacao.puml
    └── 11-diagrama-de-fluxo-banco-dashboard-notificacoes-export.puml
````

---

## 2. Por onde começar a leitura

Se você acabou de chegar no projeto e quer entender o todo, a ordem recomendada é:

1. **`proposta-oficial.md`**
   Aqui está a descrição original do desafio: contexto de negócio, necessidades do cliente, escopo do MVP e entregas esperadas (modelagem em Python, API em Java, integração, etc.).
   É o documento que responde “*por que esse sistema existe?*”.

2. **`requisitos.md`**
   Documento de requisitos (SRS) com:

    * requisitos funcionais (endpoints, fluxos de comprador e vendedor, notificações, exportação, etc.);
    * requisitos não funcionais (tecnologias obrigatórias, formato de comunicação, simplicidade de execução);
    * casos de uso principais.

3. **`arquitetura.md`** + **diagramas em `uml/`**
   Explica como o sistema foi dividido:

    * Interface Web (comprador/vendedor),
    * API em Spring Boot,
    * microserviço de ML em FastAPI,
    * banco PostgreSQL,
    * notebooks de Data Science.
      Os diagramas `.puml` mostram a arquitetura em níveis diferentes: contexto, containers, sequência, integrações e fluxo no banco.

Depois disso, o caminho se divide dependendo do interesse pessoal:

* quer entender o front? → `frontend.md`
* quer o modelo de dados? → `database.md`
* quer detalhes de ML? → `ml-model-card.md`
* quer ver como testamos? → `test-strategy.md`
* quer olhar segurança e deploy? → `security.md` + `devops-deploy.md`

---

## 3. Visão rápida dos principais documentos

### 3.1. proposta-oficial.md

Cópia fiel da proposta do hackathon.
Mostra:

* setor alvo (atendimento ao cliente / marketing),
* problema de negócio (muitos comentários, pouco tempo para ler),
* objetivo do sistema (classificar sentimento automaticamente),
* expectativas de entrega (notebook, API em Java, integração com ML, documentação e demo).

É o documento de referência quando surge dúvida de escopo.

---

### 3.2. requisitos.md

Documento de Requisitos de Software (ERS/SRS):

* define os **requisitos funcionais**:

    * receber texto e retornar sentimento + probabilidade;
    * salvar comentários e previsões no banco;
    * notificar vendedor em caso de comentários negativos críticos;
    * exibir dashboard com estatísticas;
    * exportar dados em JSON, etc.
* descreve **requisitos não funcionais**:

    * tecnologias (Java 17, Spring Boot, Python, PostgreSQL),
    * uso de JSON,
    * qualidade mínima do modelo,
    * simplicidade de execução.
* traz **casos de uso** (UC01, UC02, …) que conversam com os diagramas de casos de uso e sequência.

---

### 3.3. arquitetura.md

Documento que explica a arquitetura do sistema:

* visão por componentes (Frontend, Backend, ML, DB);
* como cada parte se comunica (endpoints, contratos JSON);
* decisões importantes (por exemplo, usar um microserviço Python em vez de carregar o modelo direto no Java);
* referências aos diagramas em `docs/uml/`:

    * arquitetura C4,
    * sequências de comprador/vendedor,
    * fluxos de banco,
    * integrações entre equipes.

---

### 3.4. frontend.md

Descrição da interface Web:

* telas previstas:

    * `login.html` (escolha de perfil comprador/vendedor),
    * `comprador.html` (vitrine de produtos + envio de comentários),
    * `vendedor.html` (cadastro de produtos, dashboard, notificações, export).
* como o front conversa com a API:

    * uso de `API_BASE_URL` em `config.js`,
    * endpoints consumidos,
    * formatos JSON esperados.
* regras importantes:

    * o **Comprador não vê o sentimento** previsto, apenas envia o comentário;
    * o **Vendedor** é quem visualiza insights, comentários críticos e exporta os dados.

---

### 3.5. database.md

Documento do modelo de dados:

* explicação das principais tabelas:

    * `cliente`, `produto`, `comentario`, `resultado_analise`, `notificacao`,
      `modelo_ml`, `dataset_registro`, `log_evento`;
* relação delas com o domínio em Java (entidades JPA);
* principais regras de negócio:

    * tipos de cliente,
    * notas válidas,
    * valores permitidos em `sentimento`, `status`, `canal`, `split`, `nivel`.
* referência ao diagrama ER em `uml/02-diagrama-de-er-banco.puml`;
* referência ao DDL final em `ddl/schema-postgres.sql`.

---

### 3.6. ml-model-card.md

“Ficha técnica” do modelo de Machine Learning:

* tarefa: análise de sentimentos em português;
* algoritmo: TF-IDF + Regressão Logística (ou outro definido pela equipe de DS);
* dataset usado, limpeza aplicada e limitações;
* métricas de desempenho;
* como o modelo é consumido pela API (via `ml_service/app.py` e endpoint `/predict`).

---

### 3.7. test-strategy.md

Estratégia de testes:

* o que é testado em cada camada:

    * backend (endpoints, validações, integração com ML),
    * microserviço de ML (respostas para textos positivos/negativos/neutros),
    * banco (restrições / consistência de dados),
    * frontend (fluxo de telas e chamadas básicas à API);
* tipos de teste:

    * unitário, integração, teste de contrato, alguns testes manuais;
* ligação com os requisitos (rastreabilidade básica).

---

### 3.8. security.md

Boas práticas de segurança e privacidade:

* não versionar segredos (senhas, tokens, `.env`, etc.);
* cuidado com logs (não espalhar dados sensíveis em `detalhe_json`);
* uso de HTTPS em produção (quando houver);
* no futuro, autenticação mínima para a área do vendedor.

---

### 3.9. devops-deploy.md

Guia de execução e deploy:

* como rodar o sistema em ambiente local:

    * subir banco (PostgreSQL),
    * backend (Spring Boot),
    * `ml_service` em Python,
    * frontend (HTML/JS ou outro servidor estático);
* ideias para uso de Docker / docker-compose;
* apontamentos iniciais para um futuro deploy na OCI.

---

### 3.10. adr/ (Architecture Decision Records)

Cada arquivo em `docs/adr/` registra uma decisão arquitetural importante, por exemplo:

* uso de um microserviço de ML em Python;
* decisão de não mostrar o sentimento para o comprador;
* escolha da estrutura final do repositório.

Cada ADR costuma ter:

* contexto,
* decisão,
* alternativas consideradas,
* consequências.

Isso ajuda a entender o “porquê” das escolhas mesmo muito tempo depois.

---

### 3.11. uml/ (diagramas)

Pasta com todos os diagramas em PlantUML (`.puml`).
Eles cobrem:

1. Arquitetura de alto nível (C4).
2. Modelo ER do banco.
3. Sequência – comprador envia comentário.
4. Sequência – vendedor recebe notificação de comentário negativo.
5. Sequência – vendedor publica produto e comprador avalia.
6. Sequência – vendedor visualiza o dashboard.
7. Navegação de telas (login/comprador/vendedor).
8. Casos de uso.
9. Integração entre equipes/módulos.
10. Fluxo no banco – novo comentário, análise e notificação.
11. Fluxo no banco – dashboard do vendedor, leitura de notificações e export.

Eles podem ser renderizados com plugins de PlantUML no IntelliJ, VS Code ou via ferramentas online.

---

## 4. Como contribuir com a documentação

Algumas combinações que funcionam bem:

* Ajustou um endpoint?
  → atualizar `requisitos.md`, `arquitetura.md` e, se for endpoint de front, também `frontend.md`.

* Mudou o modelo de dados?
  → atualizar `database.md`, `ddl/schema-postgres.sql` e o diagrama ER em `uml/`.

* Trocou algo importante na arquitetura ou integração?
  → registrar um novo arquivo em `docs/adr/`.

A ideia não é escrever livros, e sim manter o mínimo necessário para que qualquer pessoa consiga:

* entender o sistema,
* decidir se algo está coerente com o objetivo original,
* e continuar o trabalho sem depender de “memória oral” de quem estava no time.

Se algum documento ainda não existir, vale criar pelo menos o esqueleto com alguns tópicos para ir preenchendo aos poucos durante o desenvolvimento.

