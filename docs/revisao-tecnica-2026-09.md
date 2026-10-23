# Revisão Técnica — hackathon-one-sentiment-api

**Repositório:** https://github.com/AndreTeixeir/hackathon-one-sentiment-api
**Data:** 17 de setembro de 2026
**Escopo:** todas as 6 branches, revisão de código linha a linha, comparação de READMEs, execução real dos serviços e verificação contra o ambiente de produção (OCI).

---

## Sumário

Esta revisão auditou o repositório em busca de divergências entre documentação e implementação, e de falhas funcionais. Foram examinadas todas as branches, executados os serviços em ambiente controlado, inspecionados os artefatos de Machine Learning e testada a instância em produção.

O achado central: **a API nunca utilizou o modelo de Machine Learning treinado.** Ela opera permanentemente em um fallback heurístico baseado em sete palavras-chave, sem que nada no sistema sinalize esse estado — nem logs, nem o endpoint de health, nem a documentação. Isso foi confirmado tanto em execução local quanto por requisições diretas à instância de produção.

Além disso, a revisão identificou um build Docker quebrado, divergências entre o README e os endpoints implementados, e uma inconsistência estrutural no contrato da API (três classes de sentimento documentadas contra duas classes suportadas pelo modelo).

Todos os achados abaixo estão classificados pelo tipo de evidência que os sustenta: **execução** (comando rodado, output observado), **verificação externa** (consulta a fonte autoritativa) ou **leitura de código** (análise estática).

---

## 1. Estado das branches

O repositório tem seis branches com **três arquiteturas de backend distintas**, em pacotes Java diferentes e com contratos de API incompatíveis entre si.

```
feature/backend-sentiment (29/12 – 02/01)
        └─▶ feature/backend-quality (02/01 – 03/01)
                └─▶ refactor/nova-estrutura (03/01 – 05/01)
DTO+DOCs (05/01 – 08/01)
file (28/12 – 02/01)
main (jan/2026 + 2 commits de documentação em jul/2026)
```

**`main` não é o resultado da integração das demais.** O `merge-base` entre `main` e `refactor/nova-estrutura` é um commit de 03/01; todo o trabalho posterior nas branches de feature — a reestruturação de pacotes, a correção de dependências, a integração Backend↔DS — nunca foi mesclado em `main`. Os dois últimos commits de `main` (13/07 e 15/07) alteram apenas documentação.

Na prática, `main` usa o pacote `com.sentimentapi` (arquitetura própria, com `StatsController` e persistência de `AnaliseResultado`), enquanto as outras cinco branches usam `br.com.hackathonone.sentiment_backend`. São dois projetos distintos sob o mesmo repositório.

| Branch | Último commit | vs. `main` | Pacote Java | Endpoint principal |
|---|---|---|---|---|
| `main` | 15/07/2026 | — | `com.sentimentapi` | `POST /api/v1/sentiment` |
| `refactor/nova-estrutura` | 05/01/2026 | +28 / −2 | `br.com.hackathonone…` | `POST /api/sentiments` |
| `DTO+DOCs` | 08/01/2026 | +18 / −1 | `br.com.hackathonone…` | `POST /api/v1/sentiment` |
| `feature/backend-quality` | 03/01/2026 | +29 / −0 | `br.com.hackathonone…` | `POST /api/v1/sentiment` |
| `feature/backend-sentiment` | 02/01/2026 | +31 / −0 | `br.com.hackathonone…` | `POST /api/v1/sentiment` |
| `file` | 02/01/2026 | +35 / −1 | — | — |

> **`main` é a branch implantada em produção.** Confirmado por execução: o endpoint `GET /api/v1/health` existe e responde no formato específico dessa branch (`service` / `status` / `dependencies.ds-service`), e não existe em nenhuma outra. Os campos de resposta `previsao` / `probabilidade` também são exclusivos dela.

---

## 2. Achado principal: a análise de sentimento não usa o modelo treinado

### 2.1 O mecanismo

`ds-service/app/model.py` tenta carregar o modelo de `models/sentiment.joblib`. Esse arquivo não existe no repositório — nem a pasta `ds-service/models/`. Quando o carregamento falha, o atributo `pipeline` permanece `None` e a predição recai sobre uma heurística:

```python
negative_markers = ["ruim", "péssim", "horr", "defeito", "demor", "atras", "não recomendo"]
if any(m in lowered for m in negative_markers):
    return ModelResult(label="Negativo", probability=0.85)
return ModelResult(label="Positivo", probability=0.75)
```

Qualquer texto que não contenha uma dessas sete expressões retorna `Positivo` com probabilidade fixa de 0,75 — independentemente do conteúdo.

### 2.2 Confirmação em execução local

O `ds-service` foi instalado e executado em ambiente controlado:

```
POST /predict {"text":"Produto péssimo, veio com defeito e atrasou muito"}
→ {"label":"Negativo","probability":0.85}

POST /predict {"text":"Produto excelente, superou minhas expectativas"}
→ {"label":"Positivo","probability":0.75}

POST /predict {"text":"asdkjasjdk qualquer coisa sem sentido 12345"}
→ {"label":"Positivo","probability":0.75}
```

O terceiro caso — texto sem significado algum — recebe classificação positiva com 75% de confiança.

### 2.3 Confirmação em produção (OCI)

As mesmas requisições foram feitas contra a instância em produção, em 17/09/2026:

```
POST /api/v1/sentiment {"text":"produto excelente, recomendo"}
→ {"previsao":"Positivo","probabilidade":0.75}

POST /api/v1/sentiment {"text":"produto ruim, muito defeito"}
→ {"previsao":"Negativo","probabilidade":0.85}

GET /api/v1/health
→ {"service":"sentiment-backend","status":"UP","dependencies":{"ds-service":"UP"}}
```

As probabilidades fixas confirmam que o ambiente de produção opera na heurística. **O comportamento não é uma particularidade do ambiente de testes: é o que está no ar.**

### 2.4 A causa-raiz: uma decisão adiada que nunca foi tomada

O `.gitignore` do projeto contém:

```
# ===== Model artifacts (decida depois) =====
# Para hackathon, pode ser útil versionar o modelo.
# Se você quiser versionar, REMOVA as 2 linhas abaixo.
*.joblib
*.pkl
```

O "decida depois" nunca foi decidido. Com essa regra ativa, o artefato do modelo ficou fora do controle de versão, a pasta `ds-service/models/` nunca foi criada no repositório, e qualquer implantação feita a partir de um clone limpo sobe sem modelo — caindo no fallback de forma permanente e silenciosa.

Não se trata de esquecimento pontual, e sim de uma decisão de configuração deixada em aberto, cujo efeito só se manifesta em tempo de execução e sem sinalização.

*(Os arquivos `.pkl` em `datascience/` permanecem versionados porque já eram rastreados antes de a regra ser adicionada — o Git continua rastreando o que já rastreava.)*

### 2.5 O health check não revela o problema

O endpoint `/api/v1/health` reporta `"ds-service":"UP"`. Isso é tecnicamente correto — o serviço responde — mas não distingue "modelo carregado" de "modo heurístico". Um health check que não diferencia esses estados é, por si só, uma lacuna de observabilidade: o modo degradado é indistinguível do modo normal por qualquer observador externo.

---

## 3. Os artefatos de Machine Learning

Os artefatos treinados existem em `datascience/sentiment_model.pkl` e `datascience/tfidf_vectorizer.pkl`. Foram carregados e inspecionados diretamente.

| Propriedade | Valor |
|---|---|
| Classificador | `sklearn.linear_model.LogisticRegression` |
| Suporte a `predict_proba` | Sim |
| `classes_` | `[0, 1]` — binário |
| Vetorizador | `TfidfVectorizer`, 1.000 features |
| scikit-learn usado no treino | 1.6.1 |
| scikit-learn pinado em `ds-service/requirements.txt` | 1.5.2 |

Quatro consequências práticas:

### 3.1 Incompatibilidade de formato

O `model.py` espera um único objeto que receba texto cru (`pipeline.predict_proba([text])`). Os artefatos são dois objetos independentes: o texto precisa passar pelo `TfidfVectorizer` antes de chegar ao classificador. Copiar os `.pkl` para dentro do serviço não é suficiente — é necessário combiná-los em um `Pipeline` ou adaptar o carregamento.

### 3.2 Incompatibilidade de versão

Os artefatos foram serializados com scikit-learn 1.6.1, mas o serviço pina a versão 1.5.2. O carregamento nessas condições emite `InconsistentVersionWarning`, e a documentação do scikit-learn adverte que isso pode produzir *"breaking code or invalid results"*. Precisa ser alinhado antes de qualquer integração.

### 3.3 O modelo é binário — "Neutro" não é previsível

O dataset de treino (`dataset_sentimento_limpo.csv`, 4.044 registros) contém apenas dois rótulos:

| Rótulo | Registros | Origem |
|---|---|---|
| `0` (Negativo) | 2.157 | notas 1, 2 e 3 |
| `1` (Positivo) | 1.887 | notas 4 e 5 |

As avaliações de nota 3 — as mais próximas de um sentimento neutro — foram agrupadas como negativas. **O modelo real não tem como produzir a classe "Neutro".**

Isso conflita com o restante do sistema, que promete três classes: o enum `Sentimento` (`POSITIVO`, `NEGATIVO`, `NEUTRO`), o README ("Positivo, Negativo ou Neutro") e o arquivo `contracts/examples/neutral.json`.

Há um agravante: o método `Sentimento.fromLabel()` retorna `NEUTRO` como valor padrão para qualquer rótulo não reconhecido. Se o mapeamento de classes for integrado incorretamente, o sintoma será "todas as respostas viram Neutro", sem erro nem log — uma falha silenciosa.

### 3.4 Sensibilidade a acentuação

O mesmo texto, com e sem acentos, produz classificações opostas:

```
"produto péssimo, veio com defeito e atrasou"  → classe 0 (Negativo), p(1)=0.178   correto
"produto pessimo, veio com defeito e atrasou"  → classe 1 (Positivo), p(1)=0.536   incorreto
```

O vocabulário do TF-IDF contém 168 tokens acentuados; texto sem acentuação perde essas features. Como entrada real de usuário frequentemente vem sem acentos, isso representa um risco operacional relevante. Normalizar acentos apenas na inferência pioraria o resultado, já que o vocabulário treinado os contém — a correção adequada passaria por normalizar treino e inferência de forma consistente.

### 3.5 Descompasso de domínio

O modelo foi treinado com 4.044 avaliações do parque Hopi Hari (`datascience/hopi_hari (1).csv`) — comentários sobre filas, brinquedos e estrutura do parque. O vocabulário reflete isso (`atrações`, `almoço`, `adultos crianças`).

O README posiciona o produto como classificador de "comentários (de e-commerce, redes sociais, etc.)". São domínios distintos, e a acurácia fora do domínio de treino tende a ser menor. Essa limitação deve constar da documentação e do model card.

---

## 4. Build e configuração

### 4.1 O build Docker do ds-service falha

`ds-service/Dockerfile` contém:

```dockerfile
COPY app ./app
COPY models ./models
```

A pasta `models/` não existe no contexto de build. A instrução `COPY` de um diretório inexistente interrompe o build da imagem — precisamente no comando que o README instrui a executar (`docker-compose up -d --build`).

### 4.2 O script de deploy depende de um arquivo que não existe

O repositório contém `scripts/setup-oci-vm.sh` (provisionamento inicial da VM) e `scripts/deploy-oci.sh` (implantação). O fluxo de implantação documentado por eles é: clonar o repositório em `~/sentiment-api` na VM e executar `docker-compose up -d --build`.

O `deploy-oci.sh` executa:

```bash
if [ ! -f ".env" ]; then
    cp .env.example .env
```

O arquivo `.env.example` **não existe no repositório**. Em uma implantação limpa, o script falharia nesse ponto.

O mesmo script valida o frontend em `http://localhost:80` e anuncia a API em `http://<IP>/api/v1/sentiment` (porta 80, via Nginx) — mas a API em produção responde na porta **8080**, o que indica que a implantação atual não seguiu integralmente esse roteiro, ou que o container de frontend nunca funcionou (consistente com a pasta ausente descrita em 4.3).

### 4.3 Serviço de frontend aponta para pasta inexistente

O `docker-compose.yml` define um serviço `frontend` (Nginx) que monta `./frontend/web`. Essa pasta não existe no repositório. O frontend real do projeto é uma página estática de 411 linhas servida pelo próprio Spring Boot, em `backend/sentiment-backend/src/main/resources/static/index.html`.

### 4.4 Dependência Maven inexistente em três branches

As branches `DTO+DOCs`, `feature/backend-quality` e `feature/backend-sentiment` declaram:

```xml
<artifactId>spring-boot-starter-webmvc</artifactId>
```

Esse artefato não existe. Verificação na API do Maven Central:

| Consulta | Resultado |
|---|---|
| `a:spring-boot-starter-webmvc` | `numFound: 0` |
| `g:org.springframework.boot AND a:spring-boot-starter-web` | `numFound: 1` |

O nome correto é `spring-boot-starter-web`. Nessas branches o Maven falha na resolução de dependências, antes de compilar qualquer classe. O erro foi corrigido em `refactor/nova-estrutura`, que já usa o artefato correto — mas essa correção nunca chegou a `main` nem a `DTO+DOCs`.

*(Os artefatos `spring-boot-starter-actuator-test` e `spring-boot-starter-validation-test`, presentes nos mesmos arquivos, aparentam ter o mesmo problema, mas não foram verificados de forma conclusiva.)*

### 4.5 `DTO+DOCs`: JPA sem driver de banco

A branch `DTO+DOCs` inclui `spring-boot-starter-data-jpa` e sete entidades JPA com seus repositórios, mas nenhum driver JDBC — nem H2, nem PostgreSQL. A aplicação falharia na autoconfiguração do `DataSource`.

### 4.6 Controllers vazios

Na mesma branch, `DashboardController.java`, `ClienteController.java` e `ProdutoController.java` têm 57 bytes cada — apenas a declaração de pacote, sem classe ou método. A pasta `docs/` dessa branch, entretanto, contém ADRs, diagrama C4, diagrama ER e três diagramas de sequência descrevendo dashboard de vendedor, notificações e fluxo de comentários como arquitetura entregue.

### 4.7 CORS permissivo

Em `main`, `CorsConfig.java` combina `setAllowedOriginPatterns(List.of("*"))` com `setAllowCredentials(true)`. Para um projeto de demonstração o impacto é baixo, mas o padrão não deve ser reaproveitado em contexto com dados reais de usuário.

---

## 5. Divergências entre README e implementação

O repositório tem dois READMEs distintos: o de `main` (104 linhas, detalhado) e um template de 21 linhas replicado nas outras cinco branches.

Divergências entre o README de `main` e o código da própria `main`:

| Afirmação no README | Realidade |
|---|---|
| `POST /api/v1/comentarios` | Não existe |
| `GET /api/v1/dashboard/stats/{id}` (por vendedor) | Não existe. O real é `GET /api/v1/stats`, sem filtro — e não há conceito de vendedor no modelo de dados |
| Pasta `/docs` | A pasta real é `/nginx/docs` |
| Frontend servido por Nginx via compose | Servido pelo Spring Boot; a pasta do compose não existe |
| "solução completa… com Machine Learning" | Em produção, heurística de 7 palavras-chave |

Endpoints que existem e **não** estão documentados: `GET /api/v1/stats`, `GET /api/v1/health`, `POST /api/v1/sentiment/batch`.

---

## 6. Contratos inconsistentes entre branches

| Branch | Path | Campo de entrada | Campos de saída |
|---|---|---|---|
| `main` | `POST /api/v1/sentiment` | `text` | `previsao`, `probabilidade` |
| `DTO+DOCs`, `feature/*` | `POST /api/v1/sentiment` | `text` | — |
| `refactor/nova-estrutura` | `POST /api/sentiments` | `texto` | `sentimento`, `probabilidade` |

Não há contrato único e estável entre as branches.

---

## 7. Limitações desta revisão

Para que os achados sejam interpretados corretamente, registram-se os pontos que **não** puderam ser verificados por execução:

| Item | Situação |
|---|---|
| Build completo via `docker-compose` | Não executado — o ambiente de análise não permitia daemon Docker aninhado. A falha do `COPY models` é determinística e verificável estaticamente. |
| Compilação Java (`mvn package`) | Não executada — o ambiente de análise não tinha acesso ao Maven Central. A inexistência do artefato foi confirmada pela API do Maven Central. |
| `spring-boot-starter-actuator-test` / `-validation-test` | Verificação inconclusiva. |
| Métricas de acurácia do modelo | Não avaliadas formalmente. Os testes de predição foram pontuais e servem para demonstrar comportamento, não para medir desempenho. |

---

## 8. Recomendações, por prioridade

**Alta — exposto publicamente agora**
1. Conectar o modelo treinado ao `ds-service`, resolvendo formato, versão do scikit-learn e mapeamento de classes. Isso inclui **decidir explicitamente** a questão deixada em aberto no `.gitignore`: versionar o artefato do modelo, ou provisioná-lo por outro meio documentado (o que não pode é permanecer indefinido).
2. Tornar o fallback explícito e observável: manter a degradação graciosa, mas sinalizá-la no `/health` e em log.
3. Decidir o tratamento da classe "Neutro" — derivá-la de faixa de probabilidade, ou remover do contrato.
4. Corrigir o README para refletir endpoints, pastas e comportamento reais, incluindo o domínio de treino do modelo.
5. Corrigir o `Dockerfile` do `ds-service`.
6. Criar o `.env.example` de que o `scripts/deploy-oci.sh` depende, ou ajustar o script.

**Média — afeta quem clona o repositório**
7. Consolidar as branches: definir qual arquitetura é a oficial e aposentar as demais.
8. Corrigir o `spring-boot-starter-webmvc` nas branches afetadas.
9. Adicionar driver de banco em `DTO+DOCs`, ou remover a dependência de JPA.
10. Remover a branch `file` e o serviço `frontend` inoperante do compose.

**Baixa**
11. Padronizar o contrato da API (path e nomes de campos).
12. Restringir a configuração de CORS.
13. Tratar a sensibilidade a acentuação no pipeline de treino e inferência.

---

*Revisão conduzida por análise estática de todas as branches, execução controlada do `ds-service`, inspeção direta dos artefatos de Machine Learning e verificação por requisições ao ambiente de produção. Nenhuma alteração de código foi feita durante a revisão.*
