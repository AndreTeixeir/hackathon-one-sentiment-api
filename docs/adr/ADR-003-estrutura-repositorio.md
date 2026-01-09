# ADR-003 – Estrutura do repositório monolítico (backend, datascience, frontend, docs, ddl)

**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025
**Status:** Aprovado  
**Escopo:** Organização de diretórios e módulos no repositório Git

---

## 1. Contexto

O projeto envolve várias áreas de conhecimento:

- Back-end (Java + Spring Boot)
- Data Science / Machine Learning (Python, notebooks, FastAPI)
- Front-end (HTML/CSS/JS, opcionalmente Streamlit)
- Banco de dados (DDL, scripts SQL)
- Documentação (requisitos, arquitetura, diagramas, etc.)
- Futuro: DevOps (docker-compose, CI/CD, deploy em OCI)

A equipe é dividida em subgrupos, mas todos colaboram em cima de um repositório Git comum. Por isso, foi necessário definir uma **estrutura de pastas clara e estável**, que:

- facilite o trabalho em paralelo;
- deixe óbvio onde cada artefato deve ser criado;
- evite mistura de responsabilidades (ex.: front dentro do backend sem necessidade);
- seja simples o suficiente para um hackathon, mas organizada o bastante para “parecer empresa”.

---

## 2. Problema

Como organizar os arquivos do projeto no repositório de forma que:

- seja fácil localizar:
    - código de API,
    - código de ML,
    - front-end,
    - documentação,
    - scripts de banco;
- permita que diferentes pessoas e times trabalhem sem pisar demais no espaço dos outros;
- apoie a evolução futura (testes, CI/CD, containers, documentação extra).

Algumas estruturas possíveis:

- tudo misturado sob `/src` (ruim para multi-tecnologia);
- um repositório por componente (backend em um repo, ML em outro, front em outro);
- um único monorepo com pastas bem separadas (backend, datascience, frontend, docs, ddl etc.).

---

## 3. Decisão

Será utilizado **um único repositório (monorepo)** com uma estrutura de pastas organizada por responsabilidade:

```text
/
├── backend/
│   ├── src/main/java/com/example/demo/
│   │   ├── DemoApplication.java
│   │   ├── domain/
│   │   ├── domain/enums/
│   │   ├── repository/
│   │   ├── service/
│   │   └── controller/
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-oci.yml
│
├── datascience/
│   ├── ml_service/
│   │   ├── app.py
│   │   ├── model/
│   │   │   └── sentiment_model.pkl
│   │   └── requirements.txt
│   │
│   ├── ml_notebooks/
│   │   └── sentiment_model.ipynb
│   │
│   ├── notebooks/
│   │   └── Hackathon_One.ipynb
│   │
│   └── data/
│       └── (datasets)
│
├── frontend/
│   ├── web/
│   │   ├── login.html
│   │   ├── comprador.html
│   │   ├── vendedor.html
│   │   ├── css/
│   │   │   └── styles.css
│   │   └── js/
│   │       ├── config.js
│   │       ├── login.js
│   │       ├── comprador.js
│   │       └── vendedor.js
│   └── streamlit/
│       └── app.py   # opcional
│
├── docs/
│   ├── README-docs.md
│   ├── requisitos.md
│   ├── arquitetura.md
│   ├── database.md
│   ├── frontend.md
│   ├── test-strategy.md
│   ├── security.md
│   ├── devops-deploy.md
│   ├── ml-model-card.md
│   └── adr/
│       ├── ADR-001-microservico-ml.md
│       ├── ADR-002-comprador-sem-sentimento.md
│       └── ADR-003-estrutura-repositorio.md
│       # (outros ADRs futuros)
│   └── uml/
│       ├── 01-diagrama-de-arquitetura.puml
│       ├── 02-diagrama-de-er-banco.puml
│       └── (demais diagramas .puml)
│
├── ddl/
│   └── schema-postgres.sql
│
├── docker-compose.yml
├── .gitignore
└── README.md
````

---

## 4. Alternativas consideradas

### 4.1. Repositórios separados por componente (backend, ML, frontend)

**Descrição:**

* Um repositório Git para backend;
* outro para o microserviço de ML;
* outro para o frontend;
* outro para documentação ou scripts SQL.

**Prós:**

* Isolamento máximo entre áreas.
* Parecido com o que muitas empresas grandes fazem em ambientes bem maduros.

**Contras:**

* Para o contexto de hackathon, complica a vida:

    * exige configurar vários repositórios, acessos e pipelines separados;
    * dificulta a visão “fim-a-fim” do projeto em uma apresentação.
* Traz custo de coordenação maior:

    * cada ajuste de contrato Java ↔ Python envolveria PRs em repositórios diferentes.
* Para um MVP, é desnecessariamente pesado.

### 4.2. Tudo dentro de `/backend` (frontend e ML incluídos)

**Descrição:**

* Colocar front-end como `src/main/resources/static` do Spring Boot;
* colocar código de ML como submódulo ou pasta dentro de `/backend`.

**Prós:**

* Aparente simplicidade: “só um projeto Spring Boot com tudo dentro”.
* Facilita o caminho clássico de “projetos de curso” que sobem apenas um JAR.

**Contras:**

* Mistura responsabilidades:

    * backend vira “pasta genérica” para tudo;
    * ML em Python fica deslocado e confuso dentro de um projeto Java.
* Quebra a separação entre equipes:

    * Data Science teria que navegar dentro da árvore de backend para achar seus arquivos.
* Dificulta evolução real:

    * se o ML crescer, fica amarrado dentro da estrutura de um único projeto Java.

### 4.3. Monorepo com pastas claras por responsabilidade (decisão escolhida)

**Descrição:**

* Um único repositório, mas organizado em pastas de “grande área”:

    * `backend`, `datascience`, `frontend`, `docs`, `ddl`, etc.

**Prós:**

* Simples de clonar, rodar e demonstrar.
* Cada área sabe exatamente **onde trabalhar**:

    * DS em `datascience/`,
    * backend em `backend/`,
    * frontend em `frontend/`.
* Facilita integração:

    * contratos podem ser documentados em `docs/` e usados por todos.
* Fica didático para entrevista/avaliação:

    * qualquer pessoa vê rapidamente que o projeto cobre:

        * API,
        * ML,
        * UI,
        * banco,
        * documentação.

**Contras:**

* O repositório tende a crescer em tamanho com o tempo (muitos artefatos diferentes).
* Em um cenário de produção real com times grandes, talvez fizesse sentido separar em repos distintos – mas esse não é o foco agora.

---

## 5. Impactos da decisão

### 5.1. Para o backend (Java)

* O projeto Spring Boot fica isolado e organizado dentro de `/backend`.

* Classes de domínio seguem o padrão:

  ```text
  backend/src/main/java/com/example/demo/domain
  backend/src/main/java/com/example/demo/domain/enums
  backend/src/main/java/com/example/demo/repository
  backend/src/main/java/com/example/demo/service
  backend/src/main/java/com/example/demo/controller
  ```

* Arquivos de configuração (`application.yml`, `application-dev.yml`, `application-oci.yml`) ficam em `src/main/resources`.

### 5.2. Para Data Science / ML

* Notebooks de exploração e treino ficam em `datascience/ml_notebooks` e `datascience/notebooks`.
* Dados brutos ou processados ficam em `datascience/data`.
* O microserviço de ML (FastAPI/Flask) fica em `datascience/ml_service`, com:

    * `app.py`
    * `requirements.txt`
    * `model/sentiment_model.pkl`

### 5.3. Para o Frontend

* Inteface Web “clássica” (HTML/CSS/JS) fica em `frontend/web`.
* Se for usada uma interface em Streamlit, ela fica em `frontend/streamlit/app.py`.

Isso evita misturar código de UI com o Spring Boot, deixando claro que o front conversa com a API via HTTP, como um cliente externo.

### 5.4. Para Documentação

* Toda a documentação de texto, diagramas e decisões de arquitetura fica em `docs/`.
* Os ADRs específicos ficam em `docs/adr/`.
* Diagramas UML/PlantUML ficam em `docs/uml/` com nomes numerados de forma didática, por exemplo:

    * `01-diagrama-de-arquitetura.puml`
    * `02-diagrama-de-er-banco.puml`
    * …

### 5.5. Para Banco de Dados

* O script DDL principal (`schema-postgres.sql`) fica em `ddl/`.
* Isso facilita:

    * usar esse mesmo script em qualquer ambiente (local, docker, OCI);
    * documentar a evolução do banco com versões nesse diretório.

---

## 6. Convenções associadas

A decisão de estrutura traz junto algumas convenções:

* **Caminhos relativos na documentação**:

    * exemplos em `docs/arquitetura.md` e `docs/database.md` usam os mesmos nomes de pastas e arquivos.
* **Uso de `.gitignore`**:

    * cada área respeita o `.gitignore` global para:

        * `target/` (Java),
        * `__pycache__/` (Python),
        * `.ipynb_checkpoints` (notebooks),
        * arquivos `.pkl` (se for decidido não versionar o modelo em produção).
* **Nomes de arquivos e diretórios sempre em inglês ou misto coerente**, evitando abreviações aleatórias.

---

## 7. Consequências (curto e longo prazo)

### 7.1. Benefícios imediatos

* Onboarding de qualquer pessoa nova é muito mais fácil:

    * “API?” → `/backend`
    * “ML?” → `/datascience`
    * “Front?” → `/frontend`
    * “Docs?” → `/docs`
    * “Banco?” → `/ddl`
* A apresentação do projeto (por exemplo, em uma banca ou entrevista) fica clara e organizada.

### 7.2. Preparação para futuro

* Caso o projeto cresça:

    * é possível “extrair” o que hoje está em `/datascience/ml_service` para um repositório próprio,
    * ou o que está em `/frontend` para um outro repo, sem precisar reorganizar tudo do zero.
* A estrutura é compatível com:

    * docker-compose (cada pasta gera uma imagem),
    * pipelines de CI/CD separados por pasta (backend, ML, frontend).

---

## 8. Quando revisitar esta decisão

Essa decisão deve ser revista se:

* o projeto sair do escopo de hackathon e se tornar um produto com times dedicados por componente (backend, ML, front) em escala maior;
* a organização adotar uma política de “um repositório por microserviço” de forma obrigatória;
* a estrutura atual começar a atrapalhar a automação de CI/CD (por exemplo, se builds parciais por pasta não forem suficientes).

Até lá, o monorepo bem organizado com as pastas definidas acima será a estrutura oficial do Hackathon One Sentiment API.
