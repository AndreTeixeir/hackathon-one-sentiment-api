# Arquitetura de Software
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Introdução

### 1.1. Propósito

Este documento descreve a **arquitetura de software** do sistema **Hackathon One Sentiment API**, com foco em:

- como os componentes principais se organizam (Frontend, Backend, ML, Banco);
- como eles se comunicam entre si;
- quais responsabilidades cada parte assume;
- quais decisões arquiteturais foram tomadas e por quê.

Ele complementa os documentos:

- `docs/requisitos.md` (requisitos de software);
- `docs/database.md` (modelo de dados);
- `docs/frontend.md` (frontend web);
- diagramas em `docs/uml/`.

### 1.2. Escopo

A arquitetura aqui descrita cobre:

- a API principal em **Java 17 + Spring Boot**;
- o microserviço de **Machine Learning em Python (FastAPI)**;
- o **Frontend Web** (HTML/CSS/JS);
- o **banco de dados relacional** (PostgreSQL);
- os **notebooks de Data Science** (Jupyter/Colab) que treinam o modelo;
- a integração entre esses módulos.

### 1.3. Público-alvo

- Desenvolvedores (Backend, Frontend, Data Science);
- Equipe de QA / Testes;
- Mentores e avaliadores técnicos;
- Futuras pessoas que forem dar manutenção ou evoluir o projeto.

---

## 2. Visão geral da arquitetura

### 2.1. Resumo em alto nível

O sistema é dividido em quatro blocos principais:

1. **Frontend Web**  
   Interface HTML/CSS/JS que oferece:
    - telas de **login/cadastro**;
    - tela de **Comprador** (listar produtos e enviar comentários);
    - tela de **Vendedor** (cadastrar produtos, ver dashboard, notificações e exportação).

2. **Backend Java (Spring Boot)**  
   API REST responsável por:
    - receber requisições do Frontend;
    - validar e persistir dados no banco;
    - chamar o microserviço de ML para classificar o sentimento;
    - aplicar regras de negócio (ex.: criação de notificações para comentários negativos críticos);
    - montar respostas para o dashboard e exportação.

3. **Microserviço de ML (FastAPI, Python)**  
   Serviço especializado que:
    - carrega o modelo treinado (TF-IDF + Regressão Logística, por exemplo);
    - recebe texto via `POST /predict`;
    - devolve `{ label, probability, model_name, model_version }`.

4. **Banco de Dados (PostgreSQL)**  
   Armazena:
    - clientes (comprador, vendedor, admin);
    - produtos;
    - comentários;
    - resultados de análise de sentimento;
    - notificações;
    - registros de dataset de treino (opcional);
    - logs de eventos da aplicação.

Além disso, há os **notebooks de Data Science**, que ficam fora da aplicação, mas alimentam o ML com o modelo `.pkl` treinado.

### 2.2. Diagrama de arquitetura (System Context / Container)

O diagrama correspondente está em:

- `docs/uml/01-diagrama-de-arquitetura-c4.puml`

Ele mostra:

- atores (Comprador, Vendedor);
- Interface Web;
- API Spring Boot;
- Microserviço ML;
- Banco de Dados;
- notebooks de DS;
- serviço externo de e-mail/SMS (opcional).

A ideia central é:

- Frontend chama **apenas** a API Java;
- API Java chama **apenas** o ML e o banco;
- ML acessa o modelo `.pkl` e não fala direto com o banco;
- notebooks treinam o modelo que o ML consome.

---

## 3. Visão de containers (C4 – nível 2)

### 3.1. Lista de containers

| Container               | Tecnologia                    | Responsabilidade principal                                    |
|-------------------------|-------------------------------|---------------------------------------------------------------|
| Frontend Web            | HTML/CSS/JS                   | UI para Comprador e Vendedor                                 |
| Backend API             | Java 17 + Spring Boot         | Orquestra fluxo, regras de negócio, integração ML/DB         |
| ML Service              | Python 3 + FastAPI            | Servir o modelo de ML via `/predict`                         |
| Banco de Dados          | PostgreSQL                    | Persistência de dados principais                             |
| Notebooks DS            | Jupyter/Colab + scikit-learn  | EDA, treinamento, métricas, export do modelo `.pkl`          |
| Serviço de E-mail/SMS   | Provedor externo (simulado)   | Envio de notificações para o Vendedor (opcional/futuro)      |

### 3.2. Frontend Web

- Local: `/frontend/web/`
- Principais arquivos:
    - `login.html` – escolha de perfil (Comprador/Vendedor);
    - `comprador.html` – vitrine + detalhe de produto + formulário de comentário;
    - `vendedor.html` – painel com:
        - cadastro de produto;
        - dashboard de sentimentos;
        - notificações;
        - botão de exportação JSON.
- Scripts JS:
    - `js/config.js` – centraliza `API_BASE_URL` e constantes (ex.: tipos de cliente);
    - `js/login.js` – fluxo de login/cadastro simples;
    - `js/comprador.js` – chamadas aos endpoints de produtos e comentários;
    - `js/vendedor.js` – chamadas para stats, comments, notifications, export.

O Frontend **não acessa o banco diretamente**. Tudo passa pela API Java.

### 3.3. Backend API (Spring Boot)

- Local: `/backend/`
- Organização (padrão Spring):

```text
  backend/
  ├── src/main/java/com/example/demo/
  │   ├── DemoApplication.java
  │   ├── domain/           # Entidades JPA
  │   ├── domain/enums/     # Enums de domínio
  │   ├── repository/       # Interfaces Spring Data JPA
  │   ├── service/          # Lógica de negócio
  │   └── controller/       # Endpoints REST
  └── src/main/resources/
      ├── application.yml
      ├── application-dev.yml
      └── application-oci.yml
````

* Responsabilidades principais:

  * Validar dados de entrada do Frontend;
  * Gerenciar transações com o banco (via JPA);
  * Integrar com o ML service (`POST /predict`);
  * Criar e atualizar notificações;
  * Expor endpoints para stats, comments, export;
  * Registrar logs de eventos.

Endpoints típicos (prefixo sugerido: `/api/v1`):

* `/clientes`
* `/produtos`
* `/comentarios` (ou `/sentiment` agregando comentário + análise)
* `/stats`
* `/comments`
* `/notificacoes`
* `/notificacoes/{id}/ler`
* `/export`

### 3.4. Microserviço de ML (FastAPI)

* Local: `/ml_service/`

* Estrutura básica:

  ```text
  ml_service/
  ├── app.py            # FastAPI
  ├── model/
  │   └── sentiment_model.pkl
  └── requirements.txt
  ```

* Responsabilidade:

  * Expor `POST /predict` que recebe:

    ```json
    { "text": "comentário do usuário..." }
    ```

  * Retornar:

    ```json
    {
      "label": "NEGATIVE",
      "probability": 0.87,
      "model_name": "sentiment-logreg-tfidf",
      "model_version": "v1"
    }
    ```

* O ML service não fala com o banco; ele só usa o modelo `.pkl`.

### 3.5. Banco de Dados (PostgreSQL)

* DDL principal em: `/ddl/schema-postgres.sql`
* Tabelas principais:

  * `cliente`
  * `produto`
  * `comentario`
  * `resultado_analise`
  * `notificacao`
  * `modelo_ml`
  * `dataset_registro`
  * `log_evento`

Modelo detalhado em:

* `docs/database.md`
* `docs/uml/02-diagrama-de-er-banco.puml`

### 3.6. Notebooks de Data Science

* Local: `/notebooks/` e `/datascience/ml_notebooks/`
* Funções:

  * Explorar e limpar o dataset;
  * Treinar o modelo de sentimento;
  * Calcular métricas (Accuracy, F1, etc.);
  * Serializar o modelo com `joblib` em `ml_service/model/`.

O notebook não faz parte da aplicação em produção, mas é peça fundamental no ciclo de vida do modelo.

---

## 4. Componentes internos do Backend

### 4.1. Camadas lógicas

1. **Controller**

   * Recebe HTTP, valida request de forma básica, chama os serviços.
   * Ex.: `SentimentController`, `ProdutoController`, `DashboardController`, `NotificacaoController`.

2. **Service**

   * Implementa lógica de negócio:

     * fluxo de criação de comentário + chamada ao ML + gravação do resultado;
     * criação de notificações;
     * regras de `eh_critico`;
     * montagem de estatísticas para o dashboard;
     * fluxo de exportação.

3. **Repository**

   * Interfaces Spring Data JPA para operações em:

     * `Cliente`
     * `Produto`
     * `Comentario`
     * `ResultadoAnalise`
     * `Notificacao`
     * `DatasetRegistro`
     * `LogEvento`
     * `ModeloML`

4. **Domain (Entidades + Enums)**

   * Entidades JPA alinhadas ao ER:

     * `Cliente`, `Produto`, `Comentario`, `ResultadoAnalise`, `Notificacao`, `DatasetRegistro`, `LogEvento`, `ModeloML`.
   * Enums:

     * `TipoCliente`, `Sentimento`, `StatusNotificacao`, `CanalNotificacao`, `NivelLog`, `OrigemLog`, `SplitDataset`.

5. **DTOs e Mapeadores**

   * Classes para entrada/saída da API:

     * Requests e Responses para Cliente, Produto, Comentario, etc.
   * Mapeamento entre Entidade ↔ DTO, isolando o modelo interno do formato de exposição.

### 4.2. Principais componentes (exemplos)

* `SentimentService`

  * recebe texto e dados do comentário;
  * persiste `comentario`;
  * chama ML via HTTP;
  * persiste `resultado_analise`;
  * dispara criação de notif (se necessário).

* `NotificacaoService`

  * cria notificações;
  * marca notificações como lidas;
  * consulta notificações por Vendedor.

* `DashboardService`

  * consulta dados agregados no banco;
  * monta objetos de resposta para `/stats`, `/comments`, `/export`.

* `LogService` (ou mecanismo utilitário)

  * centraliza gravação de `log_evento`.

---

## 5. Fluxos principais (vista arquitetural)

Os detalhes de cada fluxo estão nos diagramas de sequência e de fluxo no banco (`docs/uml/03` a `docs/uml/11`). Aqui fica um resumo arquitetural.

### 5.1. Fluxo: Vendedor cadastra produto

1. Vendedor acessa `vendedor.html`.
2. Front chama `GET /api/v1/produtos?vendedorId=...` para listar produtos atuais.
3. Ao cadastrar novo produto:

   * Front faz `POST /api/v1/produtos` com dados e `clienteVendedorId`.
   * `ProdutoController` → `ProdutoService`.
   * Validação → persistência em `produto`.
   * Registro de log em `log_evento`.
   * Resposta com `201 Created`.

### 5.2. Fluxo: Comprador envia comentário

1. Comprador acessa `comprador.html` e escolhe um produto.
2. Preenche nota + texto e envia.
3. Front faz `POST /api/v1/comentarios`:

   * `SentimentController` chama `SentimentService`.
   * Validação de entrada.
   * Persistência do `comentario` no banco.
   * Registro de log (`comentário salvo`).
   * Chamada `POST /predict` no ML service.
   * Mapeamento da resposta em `ResultadoAnalise`.
   * Persistência de `resultado_analise`.
   * Se `NEGATIVO` e crítico:

     * busca `cliente_vendedor_id` do produto;
     * cria `notificacao` para o Vendedor;
     * log de criação de notificação.
   * Resposta ao Comprador é **apenas** “comentário recebido com sucesso”.

### 5.3. Fluxo: Vendedor vê dashboard e notificações

1. Vendedor acessa `vendedor.html`.
2. Front faz:

   * `GET /api/v1/stats?vendedorId=...`;
   * `GET /api/v1/comments?vendedorId=...`;
   * `GET /api/v1/notificacoes?vendedorId=...`.
3. API:

   * Busca `produto` do vendedor.
   * Busca `comentario` desses produtos.
   * Junta com `resultado_analise`.
   * Calcula estatísticas (total, positivos, negativos, neutros, críticos).
   * Busca notificações (`notificacao`) do vendedor.
   * Devolve JSON para o frontend montar cards, gráficos e listas.

### 5.4. Fluxo: Vendedor marca notificação como lida

1. Vendedor clica em “Marcar como lida” em uma notificação.
2. Front chama `PATCH /api/v1/notificacoes/{id}/ler`.
3. API:

   * Atualiza `status = LIDA`, `data_envio` se ainda não estiver setado.
   * Gera `log_evento` informando a ação.
4. Front atualiza visualmente a notificação.

### 5.5. Fluxo: Exportação de dados em JSON

1. Vendedor clica em “Exportar JSON”.
2. Front chama `GET /api/v1/export?vendedorId=...`.
3. API:

   * Faz uma consulta consolidada com `cliente` (Comprador), `produto`, `comentario`, `resultado_analise`.
   * Monta um JSON de exportação.
   * Registra `log_evento` de exportação.
4. Navegador baixa o arquivo `.json`.

---

## 6. Integração entre equipes e módulos

### 6.1. Estrutura de pastas e responsabilidades

A estrutura proposta na raiz do repositório:

```text
/
├── backend/                # API Spring Boot (Java 17)
├── datascience/ ou ml_service/
│   ├── ml_service/         # FastAPI /predict
│   ├── ml_notebooks/       # notebooks de treino de modelo
│   └── data/               # datasets
├── frontend/               # Interface web
│   ├── web/                # HTML/CSS/JS
│   └── streamlit/          # (opcional)
├── docs/                   # Documentação
│   ├── requisitos.md
│   ├── arquitetura.md
│   ├── database.md
│   ├── frontend.md
│   └── uml/                # .puml
├── ddl/                    # schema-postgres.sql
├── docker-compose.yml      # (opcional - integração local)
└── README.md               # visão geral
```

### 6.2. Contratos de integração

Documentados em:

* `docs/requisitos.md`
* `docs/frontend.md`
* `docs/database.md`
* diagramas `09-diagrama-de-integracao-equipes-modulos.puml`.

Os principais contratos são:

1. **Frontend ↔ Backend**

   * JSON para:

     * `/clientes`
     * `/produtos`
     * `/comentarios` / `/sentiment`
     * `/stats`
     * `/comments`
     * `/notificacoes`
     * `/notificacoes/{id}/ler`
     * `/export`
   * Front só precisa conhecer URLs e formato JSON, não o banco nem o ML.

2. **Backend ↔ ML Service**

   * `POST /predict` com:

     * `{ "text": "..." }`
   * Resposta:

     * `{ "label": "...", "probability": ..., "model_name": "...", "model_version": "..." }`.

3. **Backend ↔ Banco**

   * Mapeamento via JPA das tabelas definidas em `schema-postgres.sql`.

4. **Data Science ↔ ML Service**

   * Notebooks produzem o `.pkl`.
   * ML service carrega esse arquivo e mantém o contrato `/predict`.

---

## 7. Decisões arquiteturais principais

Algumas decisões importantes (tipo ADR simplificado):

### 7.1. ML em microserviço separado

* **Decisão:**
  Usar um microserviço em Python (FastAPI) para expor o modelo de ML, em vez de tentar reimplementar o modelo em Java.

* **Motivos:**

  * Aproveitar diretamente o pipeline scikit-learn já treinado.
  * Facilitar o trabalho da equipe de DS (Python é o ambiente natural).
  * Separar preocupações: API de negócio em Java, ML em Python.

* **Consequência:**

  * Comunicação via HTTP e JSON (latência um pouco maior, mas aceitável para MVP).
  * Precisa de cuidado com timeouts, erros de rede e logs.

### 7.2. Banco relacional único (PostgreSQL)

* **Decisão:**
  Manter um único banco relacional central, com todas as tabelas (`cliente`, `produto`, `comentario`, etc.).

* **Motivos:**

  * Simplicidade de desenvolvimento e deploy;
  * Ferramentas conhecidas (SQL, pgAdmin);
  * Fácil integração com Spring Data JPA.

* **Consequência:**

  * Para escalar muito, talvez seja necessário particionamento ou outros ajustes no futuro. Para o escopo do projeto, isso é suficiente.

### 7.3. Separação Conceitual de Perfis (Comprador vs Vendedor)

* **Decisão:**
  O sistema trata Comprador e Vendedor como perfis distintos de Cliente, com acessos de UI diferentes.

* **Motivos:**

  * Requisitos de negócio: sentimento é informação interna da empresa (Vendedor), não algo para o Comprador.
  * Interface mais clara e focada para cada tipo de usuário.

* **Consequência:**

  * Mesmo que autenticação complexa não seja implementada agora, a arquitetura já separa telas e recursos por perfil.

### 7.4. Logging centralizado em tabela `log_evento`

* **Decisão:**
  Registrar eventos importantes em uma tabela específica (`log_evento`), em vez de apenas logs de texto.

* **Motivos:**

  * Permitir consultas no próprio banco para auditoria e suporte.
  * Rastrear problemas por `cliente_id`, `comentario_id`, etc.

* **Consequência:**

  * Pequeno overhead de escrita no banco, mas benefício grande em diagnóstico.

---

## 8. Cross-cutting concerns (segurança, logs, erros)

### 8.1. Segurança básica

* Não versionar segredos (senhas de banco, tokens) no repositório.
* Habilitar CORS adequadamente para o domínio onde o frontend estiver.
* Em produção, usar HTTPS no acesso à API.
* Limitar erro detalhado em produção (sem stacktrace brutão em JSON para o cliente).

### 8.2. Logs

* Níveis: `INFO`, `WARN`, `ERROR`.
* Origem: `API`, `ML_SERVICE`, `FRONTEND`, `DB`.
* Todo fluxo crítico (comentário, análise, notificação, export) deve ter logs rastreáveis.

### 8.3. Tratamento de erros de integração com ML

* Se o ML estiver fora do ar:

  * Registrar um `log_evento` com `ERROR` e `origem = 'ML_SERVICE'`.
  * Retornar erro amigável ao cliente (Comprador ou Vendedor), sem vazar detalhes sensíveis.
  * Para MVP, é aceitável falhar a operação; numa evolução futura, poderia haver fila para reprocessar.

---

## 9. Visão de implantação (Deployment)

### 9.1. Ambiente local

* Banco PostgreSQL em container ou instalação local;
* Backend (Spring Boot) rodando com `mvn spring-boot:run`;
* ML Service rodando com `uvicorn app:app --reload --port 8000`;
* Frontend acessado diretamente via navegador (`frontend/web/login.html`) ou via servidorzinho estático.

### 9.2. Deploy com Docker (futuro próximo)

Possibilidade de `docker-compose.yml` com:

* `db` (PostgreSQL);
* `backend` (API Spring Boot);
* `ml-service` (FastAPI);
* `frontend` (servido por nginx ou outro servidor simples).

### 9.3. OCI (futuro)

* Banco PostgreSQL migrado para serviço gerenciado;
* Backend e ML em instâncias ou containers;
* Frontend servido por storage estático / gateway.

---

## 10. Conclusão

A arquitetura do **Hackathon One Sentiment API** foi pensada para:

* separar claramente responsabilidades (Frontend, API, ML, Banco);
* permitir que equipes trabalhem em paralelo:

  * DS foca no modelo e no `/predict`;
  * Backend foca em regras de negócio e orquestração;
  * Frontend foca na experiência do Comprador e do Vendedor;
* facilitar a evolução futura:

  * novos modelos de ML,
  * múltiplos idiomas,
  * autenticação mais robusta,
  * mais canais de notificação.

Este documento, junto com `requisitos.md`, `database.md`, `frontend.md` e os diagramas em `docs/uml/`, forma o conjunto básico para alguém conseguir:

* entender o sistema sem ver uma linha de código;
* depois, abrir o repositório e navegar com segurança entre as pastas.