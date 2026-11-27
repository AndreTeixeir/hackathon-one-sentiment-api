# Matriz de Rastreabilidade · Hackathon One Sentiment API

Este documento amarra, em um único lugar:

- requisitos funcionais (RF) e não funcionais (RNF);
- casos de uso (UC);
- endpoints/componentes principais;
- cenários de teste planejados.

A ideia é facilitar:

- ver se tudo o que foi pedido tem **um lugar na arquitetura**;
- ver se tudo o que foi implementado está **coberto por testes**.

---

## 1. Convenções

- **RFxx** – Requisito Funcional.
- **RNFxx** – Requisito Não Funcional.
- **UCxx** – Caso de Uso (ver docs/requisitos.md e diagramas de caso de uso).
- **T-API-xxx** – Teste de API/Backend.
- **T-ML-xxx** – Teste do microserviço de ML.
- **T-FE-xxx** – Teste de frontend (fluxo de tela).
- **T-DB-xxx** – Teste relacionado a banco de dados.
- **T-SEC-xxx** – Teste de segurança / robustez.

---

## 2. Mapa de Requisitos x Casos de Uso x Endpoints x Testes

### 2.1. Requisitos funcionais

> Lista baseada em `docs/requisitos.md` e nos diagramas de sequência.

#### RF01 – Receber comentário de comprador

- **Descrição:** A API deve aceitar o envio de comentários (texto + nota) de um Cliente - Comprador associado a um produto.
- **Caso(s) de uso:**
    - UC01 – Enviar comentário sobre produto.
- **Endpoint(s):**
    - `POST /api/v1/comentarios`
- **Componentes envolvidos:**
    - Frontend: `comprador.html`
    - Backend: `ComentarioController`, `ComentarioService`
    - DB: tabelas `comentario`, `cliente`, `produto`
- **Testes relacionados:**
    - T-FE-001 – Fluxo comprador envia comentário válido.
    - T-API-001 – POST /api/v1/comentarios com payload válido.
    - T-DB-001 – Inserção correta em `comentario`.

---

#### RF02 – Validar entrada do comentário

- **Descrição:** Antes de aceitar um comentário, o backend deve validar campos obrigatórios e regras (texto obrigatório, tamanho mínimo, nota 1–5, produto existente, etc.).
- **Caso(s) de uso:**
    - UC01 – Enviar comentário sobre produto.
- **Endpoint(s):**
    - `POST /api/v1/comentarios`
- **Componentes envolvidos:**
    - Backend: camada de validação (DTOs, Bean Validation)
    - DB: constraints de `comentario` (nota 1–5)
- **Testes relacionados:**
    - T-API-002 – Comentário sem texto → 400.
    - T-API-003 – Nota fora de [1,5] → 400.
    - T-API-004 – Produto inexistente → 404/400.
    - T-DB-002 – Inserção direta com nota inválida deve falhar (CHECK).

---

#### RF03 – Integrar com modelo de ML para análise de sentimento

- **Descrição:** Ao salvar um comentário válido, a API deve acionar o microserviço de ML para classificar o sentimento.
- **Caso(s) de uso:**
    - UC02 – Classificar sentimento de comentário (ML).
- **Endpoint(s):**
    - interno: `POST /predict` (FastAPI ML)
- **Componentes envolvidos:**
    - Backend: `SentimentService`, cliente HTTP para ML
    - Microserviço ML: FastAPI (`/predict`)
    - Modelo: arquivo `.pkl`
- **Testes relacionados:**
    - T-ML-001 – POST /predict com texto positivo, resposta coerente.
    - T-ML-002 – POST /predict com texto negativo, resposta coerente.
    - T-API-005 – Backend chamando ML e recebendo resposta esperada.

---

#### RF04 – Persistir resultado da análise de sentimento

- **Descrição:** O sistema deve registrar o sentimento, a probabilidade e o link para o comentário e modelo utilizados.
- **Caso(s) de uso:**
    - UC02 – Classificar sentimento de comentário (ML).
    - UC03 – Registrar resultados e notificações.
- **Endpoint(s):**
    - `POST /api/v1/comentarios` (fluxo interno, após o ML)
- **Componentes envolvidos:**
    - DB: tabelas `resultado_analise`, `modelo_ml`
    - Backend: `ResultadoAnaliseRepository`, `ModeloMLRepository`
- **Testes relacionados:**
    - T-DB-003 – Inserção correta em `resultado_analise`.
    - T-API-006 – Verificar se, após POST comentário, há um registro correspondente em `resultado_analise`.

---

#### RF05 – Criar notificações para comentários críticos

- **Descrição:** Quando o resultado da análise indicar `sentimento = NEGATIVO` e `eh_critico = TRUE`, deve ser criada uma notificação para o Cliente - Vendedor.
- **Caso(s) de uso:**
    - UC03 – Registrar resultados e notificações.
- **Endpoint(s):**
    - `POST /api/v1/comentarios` (fluxo interno, após inserir `resultado_analise`)
- **Componentes envolvidos:**
    - DB: tabelas `notificacao`, `resultado_analise`, `produto`, `cliente`
    - Backend: `NotificacaoService`
- **Testes relacionados:**
    - T-API-007 – Comentário com resultado NEGATIVO + crítico → `NOTIFICACAO` criada.
    - T-DB-004 – Integridade de FK `notificacao.resultado_id`.

---

#### RF06 – Cadastrar e listar produtos de vendedor

- **Descrição:** Vendedores podem cadastrar produtos, e os produtos cadastrados podem ser listados tanto para o vendedor quanto para os compradores.
- **Caso(s) de uso:**
    - UC07 – Cadastrar e gerenciar produtos.
- **Endpoint(s):**
    - `POST /api/v1/produtos`
    - `GET /api/v1/produtos`
    - `GET /api/v1/produtos?vendedorId=...`
- **Componentes envolvidos:**
    - Frontend: `vendedor.html`, `comprador.html`
    - Backend: `ProdutoController`, `ProdutoService`
    - DB: tabela `produto`
- **Testes relacionados:**
    - T-API-008 – Criar produto válido.
    - T-API-009 – Listar produtos por vendedorId.
    - T-FE-002 – Vendedor vê seus produtos na tela.
    - T-FE-003 – Comprador vê lista de produtos.

---

#### RF07 – Exibir dashboard de sentimentos para o vendedor

- **Descrição:** O vendedor deve visualizar estatísticas agregadas de sentimentos, notas e comentários relacionados aos seus produtos.
- **Caso(s) de uso:**
    - UC04 – Visualizar dashboard de sentimentos.
- **Endpoint(s):**
    - `GET /api/v1/stats?vendedorId=...`
- **Componentes envolvidos:**
    - Frontend: `vendedor.html` (dashboard)
    - Backend: `DashboardController` / `DashboardService`
    - DB: `comentario`, `resultado_analise`, `produto`
- **Testes relacionados:**
    - T-API-010 – GET /stats retorna dados coerentes (não vazios, somas batendo).
    - T-FE-004 – Dashboard exibe cards/gráficos corretos a partir do JSON de `/stats`.

---

#### RF08 – Listar comentários analisados para o vendedor

- **Descrição:** Exibir para o vendedor a lista de comentários com notas, sentimento e probabilidade.
- **Caso(s) de uso:**
    - UC05 – Visualizar comentários negativos/críticos.
- **Endpoint(s):**
    - `GET /api/v1/comments?vendedorId=...`
- **Componentes envolvidos:**
    - Frontend: `vendedor.html` (lista de comentários)
    - Backend: `DashboardController` / `ComentarioController` (dependendo do design)
    - DB: `comentario`, `resultado_analise`
- **Testes relacionados:**
    - T-API-011 – GET /comments filtra por vendedorId.
    - T-FE-005 – Lista de comentários aparece com texto, nota, sentimento e probabilidade.

---

#### RF09 – Listar notificações e permitir marcá-las como lidas

- **Descrição:** O vendedor deve ver notificações pendentes e poder marcá-las como lidas.
- **Caso(s) de uso:**
    - UC05 – Visualizar comentários negativos/críticos (via notificações).
- **Endpoint(s):**
    - `GET /api/v1/notificacoes?vendedorId=...`
    - `PATCH /api/v1/notificacoes/{id}/ler`
- **Componentes envolvidos:**
    - Frontend: `vendedor.html` (lista de notificações)
    - Backend: `NotificacaoController`, `NotificacaoService`
    - DB: tabela `notificacao`
- **Testes relacionados:**
    - T-API-012 – GET /notificacoes retorna apenas notificações do vendedor.
    - T-API-013 – PATCH /notificacoes/{id}/ler atualiza `status = LIDA`.
    - T-FE-006 – UI atualiza o estado visual ao marcar notificação como lida.

---

#### RF10 – Exportar dados de feedback em JSON

- **Descrição:** O vendedor deve conseguir exportar, em JSON, a lista de comentários e seus sentimentos para análise externa.
- **Caso(s) de uso:**
    - UC06 – Exportar feedback em JSON.
- **Endpoint(s):**
    - `GET /api/v1/export?vendedorId=...`
- **Componentes envolvidos:**
    - Frontend: `vendedor.html` (botão "Exportar JSON")
    - Backend: `DashboardController` / `ExportService`
    - DB: `cliente`, `produto`, `comentario`, `resultado_analise`
- **Testes relacionados:**
    - T-API-014 – GET /export retorna JSON com estrutura conforme especificação.
    - T-API-015 – Export respeita filtro por vendedorId.
    - T-FE-007 – Botão de export inicia download do arquivo.

---

### 2.2. Requisitos não funcionais (visão resumida)

> Detalhes completos em `docs/requisitos.md`. Aqui vai apenas o vínculo principal.

| RNF | Descrição resumida                                  | Área principal        | Testes ligados           |
|-----|-----------------------------------------------------|-----------------------|--------------------------|
| RNF01 | API deve usar JSON em todas as comunicações        | API / ML / Frontend   | T-API-001..015, T-ML-*   |
| RNF02 | Projeto deve usar Java 17 + Spring Boot            | Backend               | Verificado por build     |
| RNF03 | ML em Python 3 + scikit-learn + joblib            | Data Science / ML     | T-ML-001..003            |
| RNF04 | Banco PostgreSQL (ou H2 para dev)                 | DB / Backend          | T-DB-001..004            |
| RNF05 | Não expor segredos no Git                         | Segurança / DevOps    | T-SEC-001 (revisão repo) |
| RNF06 | Respostas claras de erro (HTTP 4xx/5xx)           | API                   | T-API-002..004           |
| RNF07 | Simplicidade de execução (poucos comandos)        | DevOps                | T-DEV-001..002           |
| RNF08 | Logging consistente por nível (INFO/WARN/ERROR)   | Observabilidade       | T-LOG-001..003           |

---

## 3. Catálogo de testes referenciados

> Os detalhes (passos, dados de entrada, resultado esperado) podem ficar em `docs/test-strategy.md` / `docs/test-report.md`.  
> Aqui é só o *mapa*.

### 3.1. Testes de API (exemplos)

- **T-API-001** – POST `/api/v1/comentarios` com payload válido → 201 Created.
- **T-API-002** – POST `/api/v1/comentarios` sem texto → 400 Bad Request.
- **T-API-003** – POST `/api/v1/comentarios` com nota inválida → 400 Bad Request.
- **T-API-004** – POST `/api/v1/comentarios` com produto inexistente → erro esperado.
- **T-API-005** – Verificar se o backend chama `/predict` e trata a resposta.
- **T-API-007** – Criação de `NOTIFICACAO` após comentário crítico.
- **T-API-010** – GET `/api/v1/stats` retorna estatísticas coerentes.
- **T-API-012** – PATCH `/api/v1/notificacoes/{id}/ler` atualiza status.

### 3.2. Testes de ML

- **T-ML-001** – `/predict` com texto claramente positivo.
- **T-ML-002** – `/predict` com texto claramente negativo.
- **T-ML-003** – `/predict` com texto neutro / ambíguo.

### 3.3. Testes de Frontend

- **T-FE-001** – Fluxo ‘comprador envia comentário’.
- **T-FE-002** – Fluxo ‘vendedor cadastra produto’.
- **T-FE-004** – Dashboard exibe cards de stats.
- **T-FE-006** – Marcar notificação como lida atualiza UI.

### 3.4. Testes de Banco

- **T-DB-001** – Inserção em `comentario` com nota válida.
- **T-DB-002** – Inserção em `comentario` com nota inválida → falha (CHECK).
- **T-DB-003** – `resultado_analise` respeita FK para `comentario`.
- **T-DB-004** – `notificacao` respeita FK para `cliente` e `resultado_analise`.

---

## 4. Como manter esta matriz atualizada

- Sempre que:
    - um novo RF for criado → adicionar linha na matriz.
    - um novo endpoint for adicionado → vincular ao RF/UC correspondente.
    - um novo teste for criado → referenciar aqui.
- Recomenda-se atualizar esta matriz **junto com o PR** que introduz a mudança, para evitar divergência entre código e documentação.

```

---

```markdown
<!-- docs/dataset.md -->

# Dataset · Hackathon One Sentiment API

Este documento descreve como os dados usados no treinamento do modelo de análise de sentimentos são escolhidos, tratados e conectados ao restante do sistema.

O objetivo é que qualquer pessoa que olhe o projeto entenda:

- de onde vieram os dados;
- como foram limpos e transformados;
- quais são as limitações e cuidados éticos.

---

## 1. Objetivo do dataset

O modelo de Machine Learning do **Hackathon One Sentiment API** precisa aprender a classificar comentários em:

- Positivos
- Negativos
- (Opcional / Futuro) Neutros

O dataset deve, portanto:

- representar linguagem natural em português (pt-BR);
- conter textos com opinião/avaliação;
- ter rótulos de sentimento confiáveis.

---

## 2. Origem dos dados

A equipe de Data Science é responsável por:

- selecionar um ou mais datasets públicos em português, por exemplo:
  - avaliações de produtos;
  - comentários de clientes;
  - reviews de serviços.

Em versões anteriores do projeto, foram considerados datasets do tipo:

- **Avaliações em português (PT-BR)** liberadas em plataformas públicas como Kaggle.

> **Importante:**  
> Os dados devem ser sempre de fonte permitida, com uso aberto para fins educacionais/experimentais, respeitando termos de uso e privacidade.

---

## 3. Estrutura lógica dos dados brutos

De forma geral, espera-se que os dados brutos tenham pelo menos:

- **texto**: comentário, avaliação ou frase de opinião.
- **rótulo_original**: etiqueta de sentimento (por exemplo: `POS`, `NEG`, `NEU`).
- (Opcional) **nota**: score numérico (por exemplo, 1–5 estrelas).
- (Opcional) campos adicionais: id original, data, categoria de produto, etc.

Quando o dataset é internalizado para o banco da aplicação (`dataset_registro`), o mapeamento sugerido é:

| Campo no dataset original | Campo em `dataset_registro` | Observação                              |
|---------------------------|-----------------------------|-----------------------------------------|
| texto / sentence / review | `texto`                     | conteúdo textual                         |
| label / sentiment         | `rotulo_original`           | ex.: `POS`, `NEG`, `NEU`                |
| rating / stars            | `nota`                      | se existir, 1–5                         |
| source / dataset_name     | `fonte`                     | ex.: nome do dataset ou URL de origem   |
| split (train/test/val)    | `split`                     | `TRAIN`, `TEST`, `VALID`                |
| id / row_id               | `id_externo`                | id no dataset de origem, se aplicável   |

---

## 4. Processo de preparação (Data Preparation)

A preparação típica segue estes passos (registrados nos notebooks em `/datascience`):

### 4.1. Coleta

- Baixar o dataset bruto (ex.: CSV, JSON).
- Armazenar localmente em `/datascience/data/` (ignorado pelo Git se necessário).

### 4.2. Limpeza básica

- Remover linhas com:
  - texto nulo;
  - rótulo nulo;
  - textos extremamente curtos (ex.: menos de 3 caracteres).
- Opcionalmente:
  - remover duplicados exatos;
  - remover spam óbvio (links repetidos, etc.).

### 4.3. Filtragem de idioma

- Sempre que possível, garantir que os textos estejam em português (se o dataset for misto).
- Caso seja necessário filtro de idioma:
  - usar bibliotecas de language detection **apenas no notebook**, sem aumentar a complexidade do backend.

### 4.4. Normalização de rótulos

- Traduzir rótulos específicos do dataset (`pos`, `negative`, `1`, `5`…) para um conjunto comum:
  - `POS` – positivo;
  - `NEG` – negativo;
  - (Opcional) `NEU` – neutro.
- Esse rótulo padronizado é armazenado em `rotulo_original` em `dataset_registro`, e serve de base para treinar o modelo.

### 4.5. Divisão em treino / teste / validação

- Definir, por exemplo:
  - 70% → treino (`TRAIN`);
  - 15% → validação (`VALID`);
  - 15% → teste (`TEST`).
- Gravar a partição no campo `split`.

> A divisão exata pode ser ajustada pela equipe DS conforme o tamanho do dataset.  
> O importante é manter **documentado** no notebook qual estratégia foi adotada.

---

## 5. Inserção no banco da aplicação (`dataset_registro`)

O preenchimento da tabela `dataset_registro` não é obrigatório para o MVP, mas é recomendado para:

- rastrear dados de treino dentro do contexto da aplicação;
- possibilitar análises futuras e auditoria do modelo.

Fluxo sugerido:

1. A partir do notebook, exportar o dataset preparado (texto + rótulo + split + fonte + id_externo).
2. Usar um script Python ou um processo de ETL simples para:
   - conectar ao Postgres;
   - inserir registros em `dataset_registro`.
3. Garantir que:
   - `fonte` descreva claramente a origem;
   - `split` seja apenas `TRAIN`, `TEST` ou `VALID`;
   - `rotulo_original` mantenha os rótulos padronizados (`POS`, `NEG`, `NEU`).

---

## 6. Relacionamento com o modelo de ML

O dataset preparado serve como entrada para o pipeline:

- Tokenização + vetorização (TF-IDF).
- Treino de modelo (ex.: Regressão Logística).
- Avaliação (accuracy, F1, etc.).
- Serialização (arquivo `.pkl`).

A tabela `modelo_ml` registra:

- nome do modelo (ex.: `sentiment-logreg-tfidf`);
- versão (ex.: `v1`);
- métricas principais (F1, acurácia);
- data de treinamento;
- caminho do arquivo `.pkl`.

Dessa forma:

- `dataset_registro` → representação dos **dados**.
- `modelo_ml` → representação dos **modelos**.
- `resultado_analise` → aplicação do modelo aos comentários reais dos usuários.

---

## 7. Limitações e viés

Alguns pontos importantes (para serem detalhados pela equipe DS de acordo com o dataset real):

- **Domínio do dataset:**  
  Se o dataset é de reviews de produtos, o modelo pode:
  - funcionar muito bem para e-commerce;
  - mas pode não generalizar tão bem para comentários políticos, notícias, etc.

- **Equilíbrio de classes:**  
  Se houver muito mais positivos do que negativos, o modelo pode:
  - “tender” a classificar tudo como positivo;
  - exigir técnicas como balanceamento ou ajuste de limiar.

- **Linguagem informal:**  
  Gírias, abreviações e ironia são desafios naturais para modelos simples.

- **Sensibilidade ética:**  
  O modelo não deve ser usado para:
  - decisões de crédito;
  - triagem automática de usuários em situações sensíveis;
  - qualquer cenário onde erros de classificação causem impacto grave.

---

## 8. Privacidade e ética

Mesmo usando dados públicos, o projeto adota alguns cuidados:

- Não copiar dados de usuários individuais sem permissão.
- Para datasets públicos, seguir os termos de uso originais.
- Em ambientes reais (fora do hackathon), seria necessário:
  - anonimizar dados pessoais;
  - evitar armazenar identificadores que permitam reidentificação do autor.

---

## 9. Atualização do modelo e do dataset

Caso o modelo seja re-treinado com novos dados:

- criar uma nova entrada em `modelo_ml` com `versao` incrementada (ex.: `v2`);
- registrar:
  - nova data de treinamento;
  - novas métricas;
  - referência à nova fonte de dados (se houver);
- opcionalmente, registrar um subconjunto significativo do novo dataset em `dataset_registro`, indicando:
  - `fonte` adequada;
  - `split` correto.

---

## 10. Pontos a preencher pela equipe de Data Science

Assim que o dataset definitivo estiver escolhido e o notebook consolidado, recomenda-se complementar este documento com:

- nome exato do dataset usado;
- tamanho aproximado (número de linhas);
- proporção de cada classe (quantos positivos/negativos/neutros);
- principais transformações aplicadas (remoção de stopwords, stems, etc.);
- link direto para o notebook responsável pelo preparo.

```

---

````markdown
<!-- docs/coding-standards.md -->

# Padrões de Código e Estilo · Hackathon One Sentiment API

Este documento define convenções de código para as principais partes do projeto:

- Backend Java (Spring Boot)
- Microserviço ML em Python (FastAPI)
- Frontend Web (HTML/CSS/JS)
- SQL / Banco de Dados
- Logs e mensagens

A ideia não é ser um manual de 200 páginas, mas sim um conjunto de regras claras o suficiente para deixar o código coeso e fácil de manter.

---

## 1. Princípios gerais

1. **Clareza acima de esperteza**  
   Nomeie variáveis, métodos e classes de forma que alguém lendo pela primeira vez entenda o propósito sem esforço.

2. **Consistência > preferência pessoal**  
   Se uma convenção foi adotada (ex.: `camelCase` para campos JSON), siga o padrão em todo o projeto.

3. **Separação de responsabilidades**  
   - Backend: Controller ↔ Service ↔ Repository.  
   - Frontend: HTML (estrutura) ↔ CSS (estilo) ↔ JS (comportamento).  
   - ML: lógica de treino em notebooks, serviço de inferência no microserviço.

4. **Sem segredos no código**  
   Nunca hardcode usuário/senha, tokens ou chaves.

---

## 2. Backend Java (Spring Boot)

### 2.1. Estrutura de pacotes

Padrão sugerido (ajustado para a estrutura existente):

```text
backend/
└── src/main/java/com/example/demo/
    ├── DemoApplication.java
    ├── domain/
    │   ├── Cliente.java
    │   ├── Produto.java
    │   ├── Comentario.java
    │   ├── ModeloML.java
    │   ├── ResultadoAnalise.java
    │   ├── Notificacao.java
    │   ├── DatasetRegistro.java
    │   └── LogEvento.java
    ├── domain/enums/
    ├── repository/
    ├── service/
    └── controller/
````

* `domain` → entidades JPA e enums.
* `repository` → interfaces `extends JpaRepository<...>`.
* `service` → regras de negócio, integração com ML, composição de respostas.
* `controller` → endpoints REST, apenas orquestrando chamada de serviços.

### 2.2. Nome de classes e métodos

* Classes:

    * `ClienteController`, `ProdutoService`, `ComentarioRepository`.
* Métodos em serviços:

    * `criarProduto`, `listarProdutosDoVendedor`, `registrarComentario`, `gerarEstatisticas`, etc.
* Métodos em controllers:

    * seguir verbos HTTP:

        * `createComentario`, `getStats`, `listComments`, etc.
    * ou delegar direto para o service mantendo nomes claros.

### 2.3. DTOs e JSON

* JSON enviado/recebido pelo frontend deve usar **camelCase**:

    * `tipoCliente`, `clienteVendedorId`, `clienteCompradorId`, `imagemUrl`, etc.
* DTOs devem refletir isso:

    * `ComentarioRequest { String texto; Integer nota; Long produtoId; Long clienteCompradorId; }`
* **Regra:** não expor diretamente as entidades JPA no JSON de API; usar DTOs próprios quando houver risco de vazar campos internos.

### 2.4. Validação

* Usar anotações de Bean Validation (`jakarta.validation`), por exemplo:

    * `@NotBlank` para texto;
    * `@Min(1)`, `@Max(5)` para nota;
    * `@NotNull` para IDs obrigatórios.
* Tratar erros de validação com um handler central (ex.: `@ControllerAdvice`), retornando mensagens amigáveis em JSON.

### 2.5. Tratamento de erros

* Não devolver stacktrace bruto para o cliente.
* Usar exceções específicas para:

    * recursos não encontrados (`ResourceNotFoundException`);
    * erro de integração com ML (`MlServiceException`);
    * validação de negócio.
* Mapear essas exceções em respostas HTTP adequadas (400, 404, 500).

---

## 3. Microserviço ML (Python + FastAPI)

### 3.1. Estrutura de arquivos

Modelo simples:

```text
ml_service/
├── app.py
├── model/
│   ├── sentiment_model.pkl
│   └── vectorizer.pkl (se necessário)
└── requirements.txt
```

Opcionalmente:

```text
ml_service/
├── app.py
├── core/
│   ├── config.py
│   └── model_loader.py
├── schemas/
│   └── sentiment.py
└── model/
    └── sentiment_model.pkl
```

### 3.2. Estilo de código

* Padrão PEP8 (nomes em `snake_case`).
* Usar **type hints** sempre que possível:

  ```python
  from pydantic import BaseModel

  class SentimentRequest(BaseModel):
      text: str

  class SentimentResponse(BaseModel):
      label: str
      probability: float
      model_name: str
      model_version: str
  ```
* Separar:

    * modelos Pydantic (`schemas/`);
    * lógica de carregamento de modelo (`core/model_loader.py`);
    * rotas FastAPI (`app.py` ou `routes/`).

### 3.3. Carregamento do modelo

* Carregar o `.pkl` uma vez, na inicialização do app, sempre que possível:

  ```python
  from fastapi import FastAPI
  import joblib

  app = FastAPI()

  model = joblib.load("model/sentiment_model.pkl")

  @app.post("/predict")
  def predict(req: SentimentRequest) -> SentimentResponse:
      # ...
  ```
* Evitar recarregar o modelo a cada requisição.

### 3.4. Logging no ML

* Usar o módulo `logging` do Python.
* Logar:

    * carregamento do modelo;
    * requisições recebidas (apenas metadados, não o texto completo se for sensível);
    * erros internos.

---

## 4. Frontend Web (HTML/CSS/JS)

### 4.1. Estrutura

```text
frontend/web/
├── login.html
├── comprador.html
├── vendedor.html
├── css/
│   └── styles.css
└── js/
    ├── config.js
    ├── login.js
    ├── comprador.js
    └── vendedor.js
```

### 4.2. Naming e organização

* IDs e classes:

    * usar nomes descritivos: `#form-login`, `#lista-produtos`, `.card-produto`, `.badge-notificacao`.
* JS:

    * uma função por intenção:

        * `carregarProdutos()`, `enviarComentario()`, `carregarStats()`, etc.
    * evitar funções anônimas gigantes dentro de `onclick`; preferir `addEventListener`.

### 4.3. Configuração centralizada

* Em `config.js`:

  ```js
  const API_BASE_URL = "http://localhost:8080/api/v1";

  const TipoCliente = {
    CLIENTE_COMPRADOR: "CLIENTE_COMPRADOR",
    CLIENTE_VENDEDOR: "CLIENTE_VENDEDOR",
    ADMIN: "ADMIN"
  };
  ```
* Demais scripts devem importar/consumir estas constantes.

### 4.4. Chamada à API

* Usar `fetch` com `async/await`:

  ```js
  async function enviarComentario(payload) {
    const resp = await fetch(`${API_BASE_URL}/comentarios`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!resp.ok) {
      // tratar erro
    }

    return await resp.json();
  }
  ```

* Tratar erros de rede e exibir mensagens amigáveis.

---

## 5. SQL e Banco de Dados

### 5.1. Localização do schema

* DDL principal:

    * `ddl/schema-postgres.sql`
* Alterações futuras:

    * podem ser registradas como comentários datados no próprio arquivo ou como novas migrações (se a equipe decidir usar Flyway/Liquibase futuramente).

### 5.2. Convenções

* Nomes de tabelas em **snake_case**:

    * `cliente`, `produto`, `comentario`, `resultado_analise`, etc.
* Nomes de colunas em **snake_case**, coerentes com as entidades:

    * `cliente_vendedor_id`, `tipo_cliente`, `data_criacao`, etc.
* Constraints nomeadas:

    * `fk_<tabela>_<tabela_referenciada>`, `ck_<tabela>_<campo>`.

---

## 6. Logs e mensagens

### 6.1. Níveis

* `INFO` – eventos normais (criação de comentário, acesso ao dashboard).
* `WARN` – comportamentos estranhos, mas que não quebram o sistema (tentativas inválidas, timeouts recuperáveis).
* `ERROR` – falhas que impedem o fluxo normal (erro ao chamar ML, falha de banco).

### 6.2. Origem

* `API`, `ML_SERVICE`, `FRONTEND`, `DB`, etc.
* Gravar origem de forma consistente em `log_evento.origem`.

### 6.3. Padrão de mensagem

* Mensagens sempre em português simples, claras e sem dados sensíveis.
* Exemplo:

    * `"Comentário salvo com sucesso"`
    * `"Falha ao chamar ML_SERVICE /predict: timeout"`

---

## 7. Comentários e documentação inline

* Evitar comentários redundantes:

    * ruim: `// soma 1 ao contador` para `contador++;`
* Bons comentários:

    * explicar **por que** algo é assim (não só o “como”).
    * marcar TODOs bem específicos: `// TODO: tratar cenário em que ML está fora do ar com fallback X`.

---

## 8. Ferramentas de formatação (opcional, recomendado)

Se o time concordar, podem ser adotadas ferramentas como:

* Java:

    * `mvn fmt:...` ou plugin de formatação no próprio IDE.
* Python:

    * `black` ou `isort` (opcional).
* JavaScript:

    * `prettier` (opcional).

O importante é que a formatação fique consistente, mesmo que não seja automatizada logo no MVP.

````

---

```markdown
<!-- CONTRIBUTING.md -->

# Contribuindo para o Hackathon One Sentiment API

Obrigado por querer contribuir 💙  
Este guia explica, de forma objetiva, como colaborar com o projeto sem se perder na estrutura ou quebrar o que já existe.

---

## 1. Visão rápida do projeto

O repositório é organizado em módulos:

```text
/
├── backend/      # API Java + Spring Boot
├── datascience/  # Notebooks e ML service em Python
├── frontend/     # Interface Web (login, comprador, vendedor)
├── docs/         # Documentação e diagramas
├── ddl/          # schema-postgres.sql
└── docker-compose.yml (futuro)
````

Antes de alterar qualquer coisa, é uma boa ideia dar uma olhada em:

* `docs/README-docs.md`
* `docs/requisitos.md`
* `docs/arquitetura.md`

---

## 2. Pré-requisitos básicos

Para desenvolver localmente, você vai precisar de:

* **Git**
* **Java 17** + Maven (ou wrapper do Maven)
* **Python 3.x** (para o ML service)
* **Node/Browser** (para testar o frontend estático)
* **PostgreSQL** (ou H2 configurado no Spring para dev)

---

## 3. Como rodar localmente (visão geral)

> Detalhes mais completos em `docs/devops-deploy.md`.

### 3.1. Backend

```bash
cd backend
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

### 3.2. ML Service (FastAPI)

```bash
cd datascience/ml_service
pip install -r requirements.txt
uvicorn app:app --reload --port 8000
```

### 3.3. Frontend

Abra os arquivos em:

* `frontend/web/login.html`
* `frontend/web/comprador.html`
* `frontend/web/vendedor.html`

ou use um servidor estático simples (ex.: `npx serve frontend/web`).

---

## 4. Fluxo de contribuição (Git)

### 4.1. Branches

* `main` (ou `master`) deve ficar estável.
* Para contribuir:

    1. Crie uma branch a partir de `main`:

       ```bash
       git checkout main
       git pull
       git checkout -b feature/nome-da-feature
       ```
    2. Trabalhe nela.
    3. Faça commits pequenos e claros.
    4. Abra um Pull Request (PR).

Sugestão de nomes de branch:

* `feature/backend-sentiment-endpoint`
* `feature/frontend-dashboard`
* `fix/ml-error-handling`

### 4.2. Commits

Usar mensagens claras, por exemplo:

* `feat: adicionar endpoint /api/v1/comentarios`
* `fix: corrigir validação de nota`
* `docs: atualizar README de docs`
* `test: adicionar testes de integração do ML`

---

## 5. Código: onde mexer

### 5.1. Se você for mexer no Backend (Java)

* Controllers → `backend/src/main/java/.../controller`
* Services → `backend/src/main/java/.../service`
* Repositories → `backend/src/main/java/.../repository`
* Entidades / enums → `backend/src/main/java/.../domain/`

Antes de enviar PR:

* rode os testes:

  ```bash
  cd backend
  mvn test
  ```

### 5.2. Se você for mexer no ML Service (Python)

* Arquivo principal → `datascience/ml_service/app.py`
* Modelo treinado → `datascience/ml_service/model/sentiment_model.pkl`
* Notebooks → `datascience/ml_notebooks/` ou `datascience/notebooks/`

Antes de enviar PR:

* rode o app localmente e teste `/predict` com alguns textos de exemplo.

### 5.3. Se você for mexer no Frontend

* Telas → `frontend/web/*.html`
* CSS → `frontend/web/css/styles.css`
* JS → `frontend/web/js/*.js`

Cuide especialmente de:

* usar `API_BASE_URL` em `config.js`;
* não duplicar lógica entre os arquivos JS.

---

## 6. Estilo e padrões

* Siga as convenções em `docs/coding-standards.md`.
* Mantém os nomes de entidades/DTOs alinhados com o domínio:

    * `Cliente`, `Produto`, `Comentario`, `ResultadoAnalise`, `Notificacao`, etc.
* Em JSON, use **camelCase**:

    * `clienteVendedorId`, `clienteCompradorId`, `imagemUrl`, `tipoCliente`.

---

## 7. Testes antes de enviar PR

Antes de abrir um Pull Request, tente:

* Backend:

    * `mvn test`
* ML service:

    * executar alguns testes manuais em `/predict` com cURL ou HTTP client;
* Frontend:

    * navegar pelos fluxos principais:

        * login → comprador envia comentário;
        * login → vendedor vê dashboard.

Se estiver adicionando novos endpoints ou fluxos:

* inclua pelo menos 1 ou 2 testes automatizados de exemplo (quando possível);
* atualize `docs/test-strategy.md` e, se aplicável, `docs/test-report.md`.

---

## 8. Documentação

Se sua mudança alterar:

* regras de negócio;
* fluxo de tela;
* endpoints;
* comportamento do modelo;

então **atualize também**:

* `docs/requisitos.md` (se for um novo requisito ou uma mudança relevante);
* `docs/arquitetura.md` ou algum diagrama em `docs/uml/`;
* `docs/traceability-matrix.md`, para manter a rastreabilidade alinhada.

---

## 9. Dúvidas e alinhamento

Como este projeto foi pensado inicialmente para um hackathon, pode haver partes ainda não finalizadas.
Antes de fazer uma mudança grande:

* verifique a documentação em `docs/`;
* veja os diagramas em `docs/uml/`;
* se estiver em equipe, combine rapidamente as decisões (para evitar trabalho retrabalhado).

Obrigado por contribuir 💫
Toda melhoria de código, documentação ou testes ajuda o projeto a ficar com cada vez mais cara de produção.
