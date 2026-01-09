# Especificação de Requisitos de Software (ERS)
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Introdução

### 1.1. Propósito

Este documento descreve a **Especificação de Requisitos de Software (ERS)** do sistema **Sentiment E-Commerce API**, que será utilizado para:

- receber comentários de clientes (Compradores) sobre produtos;
- aplicar um modelo de **Análise de Sentimentos** em português;
- priorizar comentários negativos e críticos para o **Cliente - Vendedor**;
- apoiar a visualização de métricas de satisfação por meio de um dashboard e exportação de dados.

O objetivo é registrar, de forma clara e rastreável:

- **o que o sistema deve fazer** (requisitos funcionais);
- **como ele deve se comportar** do ponto de vista técnico e de qualidade (requisitos não-funcionais);
- **as regras de negócio** que ligam as partes (Frontend, API, ML e Banco de Dados).

### 1.2. Escopo do Sistema

O **Sentiment E-Commerce API** é composto, em alto nível, por:

- **Frontend Web** (HTML/CSS/JS):  
  páginas de navegação para:
    - **Cliente - Comprador**: vitrine de produtos, formulário de avaliação;
    - **Cliente - Vendedor**: cadastro de produtos, dashboard, notificações, exportação.
- **Backend em Java (Spring Boot)**:
    - expõe endpoints REST sob `/api/v1/...`;
    - integra o Frontend com o microserviço de ML e o banco de dados;
    - aplica regras de negócio (criação de notificações, cálculo de estatísticas, etc.).
- **Microserviço de ML em Python (FastAPI)**:
    - carrega um modelo treinado (TF-IDF + Regressão Logística, ou equivalente);
    - expõe `POST /predict` para classificação de sentimento.
- **Banco de Dados relacional (PostgreSQL)**:
    - armazena clientes, produtos, comentários, resultados de análise, notificações, dataset_registro e logs.
- **Notebooks de Data Science (Jupyter/Colab)**:
    - usados para exploração de dados, treino e exportação do modelo.

O sistema será inicialmente entregue como um **MVP (Minimum Viable Product)**, com foco em:

- **classificação funcional de sentimentos**;
- **dashboards simples para o Vendedor**;
- **integração real entre DS e Backend**.

### 1.3. Público-alvo deste documento

- Integrantes do time de desenvolvimento (Backend, Frontend, Data Science, DevOps);
- Equipe de QA / Testes;
- Avaliadores técnicos e professores/mentores;
- Futuros desenvolvedores que venham dar manutenção no projeto.

### 1.4. Definições, acrônimos e abreviações

- **API** – Application Programming Interface.
- **MVP** – Minimum Viable Product (Produto Mínimo Viável).
- **DS** – Data Science (Ciência de Dados).
- **ML** – Machine Learning.
- **Cliente - Comprador** – usuário que envia comentários e avaliações de produtos.
- **Cliente - Vendedor** – usuário (empresa ou dono de loja) que cadastra produtos e acompanha feedbacks.
- **Sentimento** – classificação do comentário em termos de opinião (POSITEVO / NEGATIVO / NEUTRO).
- **RF** – Requisito Funcional.
- **RNF** – Requisito Não Funcional.
- **OCI** – Oracle Cloud Infrastructure (alvo futuro de deploy do banco).

### 1.5. Referências

- Proposta oficial do Hackathon de Análise de Sentimentos (documento do cliente/professor).
- Diagramas em `docs/uml/`:
    - Diagrama de Arquitetura (C4) – `01-diagrama-de-arquitetura-c4.puml`
    - Diagrama ER do Banco – `02-diagrama-de-er-banco.puml`
    - Diagramas de Sequência e Fluxo de Banco (03–11).
- Documentos complementares:
    - `docs/arquitetura.md`
    - `docs/database.md`
    - `docs/frontend.md`
    - `docs/ml-model-card.md`
    - `ddl/schema-postgres.sql`

### 1.6. Visão geral do documento

O documento está organizado da seguinte forma:

- Seção 2: visão geral do produto e contexto.
- Seção 3: visão funcional de alto nível.
- Seção 4: requisitos funcionais detalhados.
- Seção 5: requisitos não funcionais.
- Seção 6: restrições, premissas e riscos.
- Seção 7: referência cruzada com diagramas (rastreabilidade básica).

---

## 2. Visão geral do produto

### 2.1. Contexto de negócio

Empresas de e-commerce recebem diariamente dezenas ou centenas de comentários de clientes sobre produtos, prazos de entrega, atendimento, etc. Ler tudo manualmente é caro e lento.

O sistema proposto:

- **classifica automaticamente** o sentimento dos comentários;
- destaca comentários **negativos e críticos** para tratamento prioritário;
- fornece ao **Cliente - Vendedor**:
    - uma visão geral da satisfação (stats),
    - uma lista de comentários,
    - notificações de casos críticos,
    - exportação dos dados em formato JSON.

O **Cliente - Comprador** apenas envia seus comentários; ele **não deve receber feedback explícito** de “seu comentário foi classificado como NEGATIVO/POSITIVO”. Toda a inteligência de análise é voltada para o Vendedor/empresa.

### 2.2. Perspectiva do produto

O sistema se posiciona como uma solução intermediária entre:

- uma **interface leve** (telas HTML/JS);
- um **backend corporativo** (Spring Boot + PostgreSQL);
- um **microserviço de ML** (FastAPI + modelo trainado em Notebook).

Ele pode servir como:

- protótipo evolutivo para uma solução real de mercado;
- material de portfólio para avaliação técnica dos participantes.

### 2.3. Atores e perfis de usuário

- **Cliente - Comprador**
    - Envia comentários e notas para produtos.
    - Não tem acesso direto às análises de sentimento.
    - A experiência é semelhante à de um usuário de loja virtual.

- **Cliente - Vendedor**
    - Cadastra produtos (anúncios).
    - Visualiza dashboard de estatísticas de sentimento.
    - Recebe notificações sobre comentários negativos críticos (em dashboard e, futuramente, e-mail/SMS).
    - Exporta dados em JSON para análise adicional.

- **Equipe de Data Science**
    - Treina, valida e exporta o modelo de ML.
    - Mantém o microserviço `ml_service` e o contrato `/predict`.

- **Equipe de Backend**
    - Implementa a API Java.
    - Integra com banco e ML.
    - Aplica regras de negócio, logs e notificações.

- **Equipe de Frontend**
    - Implementa a interface Web.
    - Consome a API conforme contratos definidos.

### 2.4. Ambiente operacional

- Ambiente de desenvolvimento:
    - Java 17 + Spring Boot;
    - Python 3 + FastAPI;
    - PostgreSQL em execução local;
    - Frontend HTML/CSS/JS rodando em navegador.

- Ambiente de produção (alvo futuro):
    - Possível uso de Docker/Docker Compose;
    - Banco em OCI (Oracle Cloud Infrastructure);
    - API e ML em containers ou serviços gerenciados.

### 2.5. Dependências externas

- Serviços de e-mail/SMS (para notificações externas) – inicialmente pode ser simulado.
- Ferramentas/grids de dados para dashboards (opcional).
- Ferramentas de visualização de diagramas (PlantUML, plugins em IDE).

---

## 3. Visão funcional de alto nível

### 3.1. Principais funcionalidades

De forma resumida, o sistema deverá:

1. Permitir o **cadastro de Clientes** (Comprador e Vendedor).
2. Permitir o **cadastro de Produtos** por Clientes - Vendedores.
3. Permitir que Clientes - Compradores **enviem comentários** e notas sobre produtos.
4. **Persistir** esses comentários no banco.
5. **Chamar o microserviço de ML** para classificar o sentimento do comentário.
6. **Salvar o resultado da análise** (sentimento, probabilidade, modelo utilizado).
7. Quando necessário, **criar notificações** para o Cliente - Vendedor.
8. Disponibilizar um **dashboard** ao Vendedor com:
    - estatísticas agregadas por sentimento,
    - lista de comentários, com seus sentimentos e probabilidades,
    - notificações pendentes/lidas.
9. Permitir a **exportação em JSON** dos comentários e análises (por Vendedor).
10. Manter um **log de eventos** relevante para debug e auditoria.

### 3.2. Fluxos principais (resumo)

Os fluxos detalhados estão representados nos diagramas de sequência e fluxos de banco em `docs/uml/`. Abaixo, um resumo em linguagem natural:

1. **Vendedor publica produto**  
   Vendedor cadastra um produto na interface → API recebe os dados → valida e grava na tabela `produto` → registra log de criação.

2. **Comprador avalia/comenta um produto**  
   Comprador acessa a vitrine, escolhe um produto e envia texto + nota → API valida, grava em `comentario` → chama o ML (`/predict`) → grava um registro em `resultado_analise` → se for negativo crítico, cria um registro em `notificacao` → responde ao Comprador com mensagem genérica (“comentário recebido”).

3. **Vendedor vê dashboard e notificações**  
   Vendedor acessa `vendedor.html` → frontend chama `/api/v1/stats`, `/api/v1/comments`, `/api/v1/notificacoes` → API consulta as tabelas (`produto`, `comentario`, `resultado_analise`, `notificacao`) → monta as estatísticas e listas → frontend exibe gráficos, tabelas e badges de notificação.

4. **Vendedor marca notificação como lida**  
   Vendedor clica em “Marcar como lida” → frontend faz `PATCH /api/v1/notificacoes/{id}/ler` → API atualiza status para `LIDA` e registra log.

5. **Vendedor exporta JSON**  
   Vendedor aciona “Exportar JSON” → frontend chama `/api/v1/export?vendedorId=...` → API monta um JSON com comentários e análises do Vendedor → devolve para download.

### 3.3. Regras de negócio chave

- **RB01 – Perfil de Cliente**
    - Um registro na tabela `cliente` pode representar:
        - `CLIENTE_COMPRADOR`
        - `CLIENTE_VENDEDOR`
        - `ADMIN` (reserva para usos futuros).
- **RB02 – Comentário vinculado a Produto**
    - Todo `comentario` deve estar ligado a um `produto` existente.
- **RB03 – Análise de Sentimento obrigatória**
    - Todo comentário válido deve receber exatamente um registro em `resultado_analise` (no MVP).
- **RB04 – Notificações apenas para negativos críticos**
    - Notificações só são criadas quando:
        - `sentimento = 'NEGATIVO'` **e**
        - `eh_critico = TRUE`.
- **RB05 – Comprador não vê o sentimento**
    - A resposta ao Comprador após enviar o comentário não contém o rótulo de sentimento, apenas uma confirmação genérica.
- **RB06 – Vendedor enxerga consolidado**
    - Sentimentos, probabilidades e comentários críticos são exibidos apenas para o Cliente - Vendedor, via dashboard e notificações.

---

## 4. Requisitos Funcionais (RF)

> Numeração RFxx para facilitar rastreabilidade.

### 4.1. Gestão de Clientes

**RF01 – Cadastro de Cliente**

O sistema deve permitir o cadastro de um Cliente com os campos:

- `nome` (obrigatório),
- `email` (opcional, porém único quando informado),
- `tipoCliente` ∈ {`CLIENTE_COMPRADOR`, `CLIENTE_VENDEDOR`, `ADMIN`}.

Deve existir um endpoint, por exemplo:

```http
POST /api/v1/clientes
````

Recebendo JSON com esses campos e retornando o Cliente criado (id + timestamps).

---

**RF02 – Tipo de Cliente obrigatório**

O campo `tipoCliente` é obrigatório e deve aceitar apenas os valores:

* `CLIENTE_COMPRADOR`
* `CLIENTE_VENDEDOR`
* `ADMIN`

Valores inválidos devem gerar erro HTTP 400 com mensagem clara.

---

**RF03 – E-mail único por Cliente**

Se informado, o e-mail do Cliente deve ser único no sistema.
Tentativas de criação com e-mail duplicado devem ser rejeitadas com erro 400.

---

### 4.2. Gestão de Produtos

**RF04 – Cadastro de Produto pelo Vendedor**

O sistema deve permitir que um **Cliente - Vendedor** cadastre produtos por meio de um endpoint, por exemplo:

```http
POST /api/v1/produtos
```

Campos mínimos:

* `nome` (obrigatório),
* `preco` (obrigatório),
* `imagemUrl` (opcional),
* `categoria` (opcional),
* `tags` (opcional, string com tags separadas por vírgulas),
* `descricao` (opcional),
* `clienteVendedorId` (obrigatório, referência a `cliente` do tipo `CLIENTE_VENDEDOR`).

---

**RF05 – Listagem de Produtos**

O sistema deve fornecer endpoint para listar produtos:

* todos os produtos (para vitrine geral do Comprador), ex.:

```http
GET /api/v1/produtos
```

* produtos de um Vendedor específico, ex.:

```http
GET /api/v1/produtos?vendedorId={idVendedor}
```

---

### 4.3. Comentários e Análise de Sentimento

**RF06 – Envio de comentário pelo Comprador**

O sistema deve permitir que o **Cliente - Comprador** envie uma avaliação de um produto, com pelo menos:

* `texto` (obrigatório),
* `nota` (opcional, 1 a 5),
* `produtoId` (obrigatório),
* `clienteCompradorId` (obrigatório ou derivado da sessão),
* `origem` (opcional – valores como `SITE`, `APP`, etc.),
* `idioma` (opcional, default `pt-BR`).

Endpoint sugerido:

```http
POST /api/v1/comentarios
```

---

**RF07 – Validação de entrada de comentário**

Antes de persistir o comentário, a API deve:

* verificar se `texto` está presente e acima de um tamanho mínimo configurável (ex.: ≥ 5 caracteres);
* validar (quando informado) se `nota` está entre 1 e 5;
* verificar se `produtoId` se refere a um produto existente.

Comentário inválido → deve retornar HTTP 400 com mensagem explicando o erro.

---

**RF08 – Persistência do comentário**

Comentário válido deve ser persistido na tabela `comentario`, com:

* `texto_original`,
* `nota`,
* `origem`,
* `idioma`,
* `cliente_comprador_id`,
* `produto_id`,
* `data_criacao` (data/hora atual).

Um log de nível `INFO` deve ser registrado em `log_evento` indicando sucesso.

---

**RF09 – Chamada ao microserviço de ML**

Após salvar o comentário, a API deve invocar o microserviço de ML via HTTP:

```http
POST /predict
Content-Type: application/json

{
  "text": "<texto_original>"
}
```

O ML deve responder com JSON contendo, no mínimo:

```json
{
  "label": "NEGATIVE",
  "probability": 0.87,
  "model_name": "sentiment-logreg-tfidf",
  "model_version": "v1"
}
```

Mapeando `label` para o enum de `Sentimento` em português (`NEGATIVO`, `POSITIVO`, `NEUTRO`).

---

**RF10 – Persistência do resultado de análise**

O resultado retornado pela ML deve ser salvo na tabela `resultado_analise` com:

* `sentimento` (mapeado),
* `probabilidade`,
* `eh_critico` (regra de negócio – ver RB04),
* `data_analise`,
* `comentario_id` (FK para o comentário recém-criado),
* `modelo_id` (FK opcional para `modelo_ml`, se registrado).

---

**RF11 – Não retorno de sentimento ao Comprador**

A resposta do endpoint de envio de comentário para o Comprador **não deve conter** o sentimento previsto.
Retorno esperado (exemplo):

```json
{
  "mensagem": "Comentário registrado com sucesso."
}
```

Eventuais códigos HTTP de erro devem ser informativos (400, 500, etc.), mas sem vazar detalhes internos do modelo.

---

### 4.4. Notificações e Dashboard do Vendedor

**RF12 – Criação de notificação para comentário negativo crítico**

Sempre que um `resultado_analise` for:

* `sentimento = 'NEGATIVO'` e
* `eh_critico = TRUE`,

a API deve:

1. Identificar o `cliente_vendedor_id` responsável pelo produto avaliado.
2. Criar um registro em `notificacao` com:

    * `vendedor_id`,
    * `resultado_id`,
    * `mensagem` apropriada,
    * `status = 'PENDENTE'`,
    * `canal = 'DASHBOARD'`,
    * `data_criacao = now()`.

---

**RF13 – Listagem de notificações do Vendedor**

O sistema deve disponibilizar um endpoint, por exemplo:

```http
GET /api/v1/notificacoes?vendedorId={idVendedor}
```

Retornando a lista de notificações do Vendedor, incluindo:

* `id`,
* `mensagem`,
* `status` (`PENDENTE`, `ENVIADA`, `LIDA`),
* `canal`,
* `data_criacao`,
* `data_envio` (quando aplicável),
* dados básicos relacionados (ex.: produto/comentário, se necessário).

---

**RF14 – Marcar notificação como lida**

Deve existir endpoint para marcar uma notificação como lida:

```http
PATCH /api/v1/notificacoes/{id}/ler
```

Esse endpoint deve:

* atualizar o registro `notificacao`:

    * `status = 'LIDA'`,
    * `data_envio = NOW()` se ainda estiver vazia;
* registrar um log de nível `INFO` em `log_evento`.

---

**RF15 – Estatísticas de sentimento para o dashboard**

O sistema deve fornecer endpoint para estatísticas consolidadas para o Vendedor, por exemplo:

```http
GET /api/v1/stats?vendedorId={idVendedor}
```

Retornando algo como:

```json
{
  "totalComentarios": 120,
  "positivos": 80,
  "negativos": 30,
  "neutros": 10,
  "criticos": 5
}
```

Esses números podem ser calculados pela API com base em `comentario` + `resultado_analise` filtrando por produtos do Vendedor.

---

**RF16 – Lista detalhada de comentários analisados**

Endpoint para listar comentários e seus sentimentos, por Vendedor, ex.:

```http
GET /api/v1/comments?vendedorId={idVendedor}
```

Cada item deve incluir (pelo menos):

* dados do comentário (texto, nota, data),
* dados do resultado (sentimento, probabilidade, eh_critico),
* identificação do produto.

---

### 4.5. Exportação de dados

**RF17 – Exportar feedback em JSON por Vendedor**

O sistema deve oferecer endpoint para exportação de dados:

```http
GET /api/v1/export?vendedorId={idVendedor}
```

Retornando um JSON (download) com os comentários e suas análises associados àquele Vendedor.

Esse JSON servirá de base para análises posteriores (em ferramenta externa, por exemplo).

---

### 4.6. Integração com ML e modelo de dados de treino

**RF18 – Registro de metadados do modelo de ML**

O sistema deve permitir registrar metadados do modelo de ML utilizado na tabela `modelo_ml`, incluindo:

* `nome`,
* `versao`,
* `tipo_modelo`,
* `caminho_arquivo`,
* `f1_score`,
* `acuracia`,
* `data_treinamento`,
* `ativo`.

Esse registro poderá ser feito por script ou endpoint interno (não necessariamente exposto no MVP).

---

**RF19 – Registro opcional de dataset_registro**

Se a equipe de DS julgar relevante, o sistema pode importar amostras do dataset de treino para a tabela `dataset_registro`, com:

* `texto`,
* `nota` (se aplicável),
* `rotulo_original` (POS, NEG, NEU),
* `fonte` (ex.: Kaggle),
* `split` (TRAIN, TEST, VALID),
* `id_externo`.

---

### 4.7. Logs e Auditoria

**RF20 – Registro de logs de eventos**

O sistema deve registrar logs na tabela `log_evento` para eventos importantes, incluindo, mas não se limitando a:

* comentário inválido (WARN);
* comentário salvo (INFO);
* erro ao chamar microserviço de ML (ERROR);
* resultado de análise salvo (INFO);
* notificação criada (INFO);
* notificação marcada como lida (INFO);
* acessos ao dashboard (INFO);
* geração de export JSON (INFO).

Campos principais:

* `nivel` (INFO, WARN, ERROR),
* `origem` (API, ML_SERVICE, DB, FRONTEND),
* `mensagem`,
* `detalhe_json` (payload ou contexto),
* `data_evento`,
* `cliente_id` (quando fizer sentido),
* `comentario_id` (quando relacionado).

---

## 5. Requisitos Não Funcionais (RNF)

### 5.1. Tecnologia e Plataformas

**RNF01 – Tecnologias obrigatórias**

* Backend: Java 17 + Spring Boot.
* ML Service: Python 3 + FastAPI ou Flask.
* Banco de Dados: PostgreSQL (desenvolvimento com instância local; alvo futuro OCI).
* Notebooks de DS: Jupyter/Colab, com `pandas`, `numpy`, `scikit-learn`, `joblib`.

---

**RNF02 – Comunicação entre componentes**

* Todas as comunicações entre Frontend ↔ API ↔ ML devem usar HTTP/HTTPS com payload em JSON.
* Endpoints REST devem seguir convenções razoáveis (verbos/métodos: GET/POST/PATCH).

---

### 5.2. Desempenho (nível MVP)

**RNF03 – Latência da classificação**

* Para o MVP, não há SLA rígido, mas a classificação de um comentário (do envio até a criação do registro em `resultado_analise`) deve ocorrer, em condições normais, em poucos segundos (ex.: até 3–5s).

---

**RNF04 – Volume de dados**

* MVP focado em volumes pequenos/médios (poucos milhares de comentários).
* O modelo de dados deve permitir escala vertical/moderada sem reestruturações drásticas.

---

### 5.3. Usabilidade

**RNF05 – Simplicidade da interface**

* As telas devem ser simples e objetivas:

    * Comprador: vitrine + formulário de avaliação;
    * Vendedor: dashboard com cards, gráficos básicos e tabelas;
* Não é requisito adotar frameworks sofisticados de front (React, etc.) neste MVP.

---

### 5.4. Segurança e Privacidade

**RNF06 – Não versionar segredos**

* Nenhuma senha, token ou chave sensível deve ser versionada no repositório:

    * arquivos `.env`,
    * `application-*.yml` com credenciais reais,
    * chaves de serviços externos.

---

**RNF07 – Cuidados com logs**

* Logs não devem conter:

    * senhas,
    * dados sensíveis além do necessário,
    * dados pessoais desnecessários (quando puder ser evitado).
* Em ambiente de produção, logs detalhados podem ser restringidos ou anonimizados.

---

**RNF08 – Acesso a funcionalidades sensíveis**

* A área do Vendedor (dashboard, notificações, exportação) deve ser separada conceitualmente da área do Comprador.
* Autenticação/autorização completas podem ser tratadas como evolução futura, mas o desenho já considera essa separação de responsabilidades.

---

### 5.5. Qualidade de Código e Arquitetura

**RNF09 – Separação de camadas no backend**

* O código Java deve seguir, no mínimo, a divisão:

    * `controller` – entrada/saída HTTP;
    * `service` – regras de negócio e orquestração;
    * `repository` – acesso ao banco de dados;
    * `domain` – entidades de domínio (Cliente, Produto, Comentario, etc.);
    * `domain/enums` – enums (TipoCliente, Sentimento, etc.).

---

**RNF10 – Consistência de nomenclatura**

* Os nomes usados em:

    * entidades de domínio,
    * tabelas do banco,
    * DTOs da API,
    * JSON no front,

  devem ser **coerentes e previsíveis**, evitando termos diferentes para o mesmo conceito.

Exemplo: sempre usar `clienteVendedorId` (front/DTO) para o mesmo conceito mapeado ao campo `cliente_vendedor_id` na tabela e ao atributo equivalente na entidade Java.

---

**RNF11 – Documentação mínima obrigatória**

* O repositório deve conter, no mínimo:

    * `README.md` na raiz com instruções de execução;
    * `docs/` com:

        * este `requisitos.md`,
        * documentação de arquitetura (`arquitetura.md`),
        * documentação de banco (`database.md`),
        * documentação de frontend (`frontend.md`),
        * diagramas em `docs/uml/`;
    * `ddl/schema-postgres.sql` com o DDL oficial.

---

### 5.6. Testes e Qualidade

**RNF12 – Testes mínimos**

* Devem existir, no mínimo:

    * alguns testes unitários de backend (ex.: validação de entrada, regras de negócio simples);
    * pelo menos um teste de integração simples exercitando:

        * envio de comentário,
        * chamada ao ML (pode ser mock),
        * gravação no banco.

---

**RNF13 – Example-driven development**

* O projeto deve manter, na documentação (README, etc.), exemplos reais de:

    * comentários positivos/negativos,
    * requisição e resposta do endpoint de sentimento,
    * fluxo completo de envio de comentário e consumo da API pelo front.

---

## 6. Restrições, premissas e riscos

### 6.1. Restrições

* Tecnologias de base (Java, Spring, Python, PostgreSQL) não podem ser trocadas sem forte justificativa, pois fazem parte do escopo do hackathon.
* Recursos de infraestrutura são limitados (uso de camadas “always free” na OCI, quando adotados).

### 6.2. Premissas

* O microserviço de ML estará disponível e responderá dentro de um tempo razoável.
* O dataset utilizado para treino representa suficientemente bem o tipo de comentários que o sistema irá receber.
* Os comentários são enviados em português brasileiro na maioria dos casos (idioma padrão `pt-BR`).

### 6.3. Riscos (resumo)

* **Risco de acurácia**: modelo de ML com desempenho abaixo do esperado → métricas devem ser documentadas em `ml-model-card.md`.
* **Risco de indisponibilidade do ML**: falha no `/predict` pode impedir análises → a API deve lidar com erros de forma controlada (log + retorno de erro ao cliente ou marcação de status “não analisado”).
* **Risco de escala**: se o volume de dados crescer muito além do esperado para o MVP, poderão ser necessários ajustes em índices, consultas e infraestrutura.

---

## 7. Rastreabilidade (visão simples)

A tabela abaixo mostra como alguns requisitos funcionais se relacionam com diagramas e partes do sistema:

| RF      | Descrição resumida                          | Diagrama relacionado                                     | Componentes principais                     |
| ------- | ------------------------------------------- | -------------------------------------------------------- | ------------------------------------------ |
| RF04    | Cadastro de Produto                         | 05-diagrama-de-sequencia-vendedor-publica-produto...     | Frontend Vendedor, API, DB (`produto`)     |
| RF06–11 | Envio de comentário + análise de sentimento | 03-diagrama-de-sequencia-comprador-envia-comentario      | Frontend Comprador, API, ML, DB            |
| RF12–16 | Notificações e Dashboard                    | 04, 06, 10, 11 (diagramas de sequência e fluxo no DB)    | Frontend Vendedor, API, DB (`notificacao`) |
| RF17    | Exportar feedback em JSON                   | 11-diagrama-de-fluxo-banco-dashboard-notificacoes...     | API, DB, Frontend Vendedor                 |
| RF18–20 | Integração com ML, dataset_registro, logs   | 01-diagrama-de-arquitetura-c4, 09-diagrama-de-integracao | API, ML Service, DB                        |

Uma matriz de rastreabilidade completa (RF ↔ Casos de Uso ↔ Testes) pode ser detalhada em um documento à parte, caso o projeto evolua para um nível de formalização ainda maior.

