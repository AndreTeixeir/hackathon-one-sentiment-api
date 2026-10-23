# DevOps & Deploy · Hackathon One Sentiment API
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Objetivo deste documento

Este arquivo explica **como colocar o projeto para rodar** em diferentes contextos:

- ambiente local (sem Docker);
- ambiente local com Docker (planejado);
- visão de futuro para deploy em nuvem (ex.: OCI);
- boas práticas de configuração, logs, CI/CD e versionamento.

A ideia é que qualquer pessoa que chegue no projeto consiga entender:

- **o que precisa estar instalado**;
- **como configurar variáveis de ambiente**;
- **como subir backend, ML service e banco**;
- **como rodar testes básicos depois do deploy**.

---

## 2. Visão geral de componentes e ambientes

### 2.1. Componentes principais

Dentro do repositório do **Hackathon One Sentiment API**, os componentes relevantes para DevOps/Deploy são:

```text
/
├── backend/              # API Java Spring Boot
├── datascience/
│   ├── ml_service/       # Microserviço ML (FastAPI/Flask)
│   ├── ml_notebooks/     # Notebooks de treino do modelo
│   └── data/             # Dados (se usados localmente)
├── frontend/             # Interface Web (HTML/CSS/JS ou Streamlit)
├── ddl/                  # Arquivos .sql (schema do banco)
├── docs/                 # Documentação (inclui este arquivo)
└── docker-compose.yml    # (planejado para integração local)
````

### 2.2. Ambientes de execução

Pensamos em 3 “modos” de uso:

| Ambiente               | Objetivo                                | Situação atual |
| ---------------------- | --------------------------------------- | -------------- |
| **Local – sem Docker** | Desenvolvimento, estudo, testes manuais | **Usado hoje** |
| **Local – com Docker** | Subir tudo com um comando só            | **Planejado**  |
| **Cloud (ex.: OCI)**   | MVP rodando em ambiente de nuvem        | **Planejado**  |

---

## 3. Estrutura técnica por componente

### 3.1. Backend (Spring Boot)

* Linguagem: **Java 17**
* Build: **Maven**
* Port padrão: `8080`
* Perfis Spring:

    * `dev` – desenvolvimento local (`application-dev.yml`)
    * `oci` – futuro deploy em nuvem (`application-oci.yml`)

### 3.2. Microserviço de ML (FastAPI/Flask)

* Linguagem: **Python 3.x**
* Framework: **FastAPI** (preferido) ou Flask
* Port padrão: `8000`
* Responsabilidade:

    * carregar modelo `.pkl`;
    * expor `POST /predict`.

### 3.3. Banco de Dados (PostgreSQL)

* Banco sugerido: **PostgreSQL**
* Database sugerido: `sentimentdb`
* Usuário e senha: definidos localmente (não versionados)
* Script de criação de schema:

    * `ddl/schema-postgres.sql`

### 3.4. Frontend Web

* Páginas principais:

    * `frontend/web/login.html`
    * `frontend/web/comprador.html`
    * `frontend/web/vendedor.html`
* Config central:

    * `frontend/web/js/config.js` com `API_BASE_URL`

---

## 4. Configuração de ambientes e variáveis

### 4.1. Spring Boot – Perfis e configs

Arquivos de configuração:

```text
backend/src/main/resources/
├── application.yml        # config base (comum)
├── application-dev.yml    # ambiente local
└── application-oci.yml    # futuro deploy em nuvem
```

Exemplo simples de `application.yml`:

```yaml
spring:
  application:
    name: hackathon-one-sentiment-api

# Configurações comuns a todos os ambientes
server:
  port: 8080
```

Exemplo de `application-dev.yml` (local):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/sentimentdb
    username: SENTIMENT_USER
    password: SENTIMENT_PASS
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

app:
  ml:
    base-url: http://localhost:8000
    predict-path: /predict
```

> **Importante:** em vez de fixar `username` e `password` aqui, o ideal é ler de variáveis de ambiente (abaixo).

### 4.2. Variáveis de ambiente recomendadas

Para o backend:

* `SPRING_PROFILES_ACTIVE` – qual perfil usar (`dev`, `oci`, etc.)
* `SPRING_DATASOURCE_URL`
* `SPRING_DATASOURCE_USERNAME`
* `SPRING_DATASOURCE_PASSWORD`

Para o ML service:

* `ML_MODEL_PATH` – caminho do arquivo `.pkl`
* `ML_HOST` – host onde roda o serviço (ex.: `0.0.0.0`)
* `ML_PORT` – porta (ex.: `8000`)

Para o frontend (não é env, mas uma constante):

* `API_BASE_URL` dentro de `frontend/web/js/config.js`:

```js
// Exemplo para ambiente local
const API_BASE_URL = "http://localhost:8080/api/v1";
```

---

## 5. Fluxo de desenvolvimento local (sem Docker)

Este é o modo **principal hoje**, já que Docker nem sempre está disponível.

### 5.1. Pré-requisitos

* **Java 17** instalado (`java -version`)
* **Maven** (`mvn -v`)
* **Python 3.x** (`python --version` ou `python3 --version`)
* **PostgreSQL** instalado localmente
* (Opcional) um editor/IDE:

    * IntelliJ (para backend),
    * VS Code (para Python/Frontend).

### 5.2. Passo 1 – Clonar o repositório

```bash
git clone https://github.com/<organização>/<repositorio>.git
cd <repositorio>
```

### 5.3. Passo 2 – Criar o banco e o schema

1. Criar o database `sentimentdb` no PostgreSQL (via pgAdmin ou terminal):

   ```sql
   CREATE DATABASE sentimentdb;
   ```

2. Rodar o script `ddl/schema-postgres.sql` apontando para esse banco.
   Exemplo via terminal:

   ```bash
   psql -U postgres -d sentimentdb -f ddl/schema-postgres.sql
   ```

   Ajuste o usuário (`-U`) conforme sua instalação.

### 5.4. Passo 3 – Configurar o backend

No sistema operacional, defina as variáveis de ambiente, por exemplo:

**Windows (PowerShell):**

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/sentimentdb"
$env:SPRING_DATASOURCE_USERNAME="SENTIMENT_USER"
$env:SPRING_DATASOURCE_PASSWORD="SENTIMENT_PASS"
```

**Linux/macOS (bash):**

```bash
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sentimentdb
export SPRING_DATASOURCE_USERNAME=SENTIMENT_USER
export SPRING_DATASOURCE_PASSWORD=SENTIMENT_PASS
```

> **Obs.:** crie um usuário no Postgres com esse nome/senha ou ajuste para o que você tiver.

### 5.5. Passo 4 – Subir o microserviço de ML

Entre na pasta do serviço de ML:

```bash
cd datascience/ml_service
```

Crie um ambiente virtual (opcional, mas recomendado):

```bash
python -m venv .venv
# ativar:
# Windows:
.\.venv\Scripts\activate
# Linux/macOS:
source .venv/bin/activate
```

Instale as dependências:

```bash
pip install -r requirements.txt
```

Garanta que o modelo `.pkl` está na pasta `model/` conforme combinado.
Depois, rode o servidor (exemplo com FastAPI + uvicorn):

```bash
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

O serviço de ML deve estar acessível em algo como:

* `http://localhost:8000/docs` (documentação automática do FastAPI, se configurada)

### 5.6. Passo 5 – Subir o backend (API Java)

Em outro terminal, volte para a raiz e entre em `/backend`:

```bash
cd backend
mvn spring-boot:run
```

Se estiver usando o wrapper do Maven:

```bash
./mvnw spring-boot:run
```

A API deve subir em:

* `http://localhost:8080`

### 5.7. Passo 6 – Subir o frontend

**Modo simples (HTML estático):**

* Abra `frontend/web/login.html` diretamente no navegador **ou**
* Use um servidor estático simples (por exemplo, usando Python):

  ```bash
  cd frontend/web
  python -m http.server 5500
  ```

  e acesse:

    * `http://localhost:5500/login.html`

Certifique-se de que `API_BASE_URL` em `js/config.js` está apontando para:

```js
const API_BASE_URL = "http://localhost:8080/api/v1";
```

---

## 6. Desenvolvimento local com Docker (planejado)

> Esta seção descreve como **poderia** funcionar com Docker.
> Ela pode ser usada como base para implementar o `docker-compose.yml` no futuro.

### 6.1. Ideia de `docker-compose.yml`

Serviços previstos:

* `db` – Postgres
* `ml-service` – FastAPI
* `backend` – Spring Boot
* (Opcional) `frontend` – nginx ou outro servidor estático

Exemplo (resumido):

```yaml
version: "3.9"

services:
  db:
    image: postgres:16
    container_name: sentiment-db
    environment:
      POSTGRES_DB: sentimentdb
      POSTGRES_USER: sentiment_user
      POSTGRES_PASSWORD: sentiment_pass
    ports:
      - "5432:5432"
    volumes:
      - ./ddl/schema-postgres.sql:/docker-entrypoint-initdb.d/01-schema.sql

  ml-service:
    build: ./datascience/ml_service
    container_name: sentiment-ml
    environment:
      ML_MODEL_PATH: /app/model/sentiment_model.pkl
    ports:
      - "8000:8000"
    depends_on:
      - db

  backend:
    build: ./backend
    container_name: sentiment-backend
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/sentimentdb
      SPRING_DATASOURCE_USERNAME: sentiment_user
      SPRING_DATASOURCE_PASSWORD: sentiment_pass
      APP_ML_BASE_URL: http://ml-service:8000
    ports:
      - "8080:8080"
    depends_on:
      - db
      - ml-service
```

> No futuro, é só completar esse arquivo e ajustar o Dockerfile de cada serviço.

---

## 7. Deploy em nuvem (visão de futuro – ex.: OCI)

Sem entrar em detalhes específicos de serviços da nuvem, o desenho geral seria:

* **Banco de dados**:

    * serviço gerenciado (PostgreSQL ou DB compatível).
* **Backend (Spring Boot)**:

    * empacotado em container ou rodando em uma VM;
    * usando o perfil `oci` (`application-oci.yml`).
* **ML Service (FastAPI)**:

    * também empacotado em container;
    * exposto apenas para a rede interna do backend.
* **Frontend**:

    * servido como site estático (por exemplo, em bucket ou servidor web simples);
    * `API_BASE_URL` apontando para o domínio do backend (HTTPS).

Coisas a configurar nesse cenário:

* Variáveis de ambiente via painel da nuvem (nunca no código).
* HTTPS (via certificado/gateway).
* Regras de rede/firewall:

    * backend acessível ao público via HTTPS;
    * ML service e DB acessíveis só internamente.

---

## 8. Logs, monitoramento e saúde da aplicação

### 8.1. Logs do backend

* Framework: logging padrão do Spring Boot (`logback` por padrão).
* Logs de aplicação podem ser enviados para:

    * console (durante desenvolvimento);
    * arquivo (produção);
    * e registrados também na tabela `log_evento`.

Boas práticas:

* Usar correlação simples (ex.: `correlacao_id`) quando fizer múltiplas chamadas em cadeia.
* Em caso de erro de integração com o ML:

    * logar `nivel = ERROR`, `origem = API` ou `ML_SERVICE`.

### 8.2. Logs do ML service

* Usar logging do Python (`logging`).
* Logar:

    * inicialização do modelo (carregado com sucesso ou não);
    * erros de inferência;
    * chamadas anormais (payload vazio, por exemplo).

### 8.3. Health checks (simples)

Possíveis endpoints:

* Backend:

    * `GET /actuator/health` (se Spring Boot Actuator estiver configurado).
* ML service:

    * `GET /health` ou `GET /` retornando algo como `{status: "ok"}`.

Esses endpoints podem ser usados por:

* scripts simples de “smoke test” pós-deploy;
* futuras ferramentas de monitoramento.

---

## 9. CI/CD (GitHub Actions – visão inicial)

> Não é obrigatório ter CI/CD completo para o hackathon, mas vale deixar o caminho desenhado.

### 9.1. Pipeline básico sugerido

Para o backend:

* Disparar pipeline:

    * a cada `push` em `main` ou em PR.
* Etapas:

    1. Checar código (checkout).
    2. Configurar Java.
    3. Rodar testes (`mvn test`).
    4. Rodar build (`mvn package`).

Exemplo simplificado de workflow (`.github/workflows/backend-ci.yml`):

```yaml
name: Backend CI

on:
  push:
    paths:
      - "backend/**"
  pull_request:
    paths:
      - "backend/**"

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repo
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Build & Test
        working-directory: ./backend
        run: mvn -B clean verify
```

Para o ML service (futuro):

* Workflow separado rodando:

    * `pip install -r requirements.txt`
    * `pytest` (se houver testes).

---

## 10. Checklists de deploy

### 10.1. Pré-deploy (local ou nuvem)

* [ ] Database criado (`sentimentdb`) e com schema aplicado.
* [ ] Variáveis de ambiente definidas:

    * backend (Spring),
    * ML service (Python).
* [ ] Modelo `.pkl` disponível para o ML service.
* [ ] Configuração do `API_BASE_URL` correta no frontend.

### 10.2. Deploy (ordem recomendada)

1. Subir **banco**.
2. Subir **ML service** (FastAPI).
3. Subir **backend** (Spring Boot).
4. Servir o **frontend**.

### 10.3. Pós-deploy (smoke tests)

* Backend:

    * [ ] `GET /actuator/health` (ou endpoint equivalente).
* ML service:

    * [ ] `POST /predict` com texto de teste.
* Fluxo completo:

    * [ ] Acessar `login.html`, entrar como comprador, enviar comentário.
    * [ ] Ver se o comentário aparece no banco (`comentario`).
    * [ ] Ver se `resultado_analise` foi gerado.
    * [ ] Ver se `notificacao` foi criada (em caso de comentário negativo crítico).
    * [ ] Ver dashboard do vendedor carregando stats.

---

## 11. Versionamento e releases

Sugestão simples:

* Usar **tags Git**:

    * `v0.1.0` – primeira integração backend + ML;
    * `v0.2.0` – frontend integrado;
    * `v1.0.0` – MVP apresentado no hackathon.
* Criar um `CHANGELOG.md` na raiz, com:

    * versão;
    * data;
    * principais mudanças.

---

## 12. Conclusão

Este documento descreve como **subir, configurar e evoluir** o **Hackathon One Sentiment API** do ponto de vista de DevOps e Deploy:

* hoje: foco em ambiente local sem Docker, com passos claros;
* amanhã: espaço aberto para Docker, CI/CD, e deploy em nuvem (ex.: OCI);
* sempre: sem segredos no código, com perfis de ambiente bem definidos e logs minimamente organizados.

Ele deve ser lido junto com:

* `docs/requisitos.md`
* `docs/arquitetura.md`
* `docs/database.md`
* `docs/frontend.md`
* `docs/test-strategy.md`
* `docs/security.md`

Assim, o projeto não é só “um código que roda”, mas um sistema com **caminho de operação bem definido**, pronto para crescer.
