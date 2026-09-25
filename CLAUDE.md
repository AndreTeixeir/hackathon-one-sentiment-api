# hackathon-one-sentiment-api

API de análise de sentimento em texto: backend **Java 17 / Spring Boot 3.2** integrado a um microserviço de inferência em **Python / FastAPI**, com persistência em PostgreSQL. Projeto do Hackathon One, mantido como peça de portfólio.

---

## Branch canônica

**`main` é a branch de referência e é a que está implantada em produção** (Oracle Cloud, `http://152.67.61.11:8080`). Todo trabalho parte dela.

⚠️ **Armadilha importante:** este repositório tem seis branches com **três arquiteturas de backend diferentes**, que nunca foram integradas. O pacote Java muda entre elas:

| Branch | Pacote Java | Raiz do módulo Maven |
|---|---|---|
| `main` | `com.sentimentapi` | `backend/sentiment-backend/` |
| `DTO+DOCs`, `feature/*` | `br.com.hackathonone.sentiment_backend` | `backend/sentiment-backend/` |
| `refactor/nova-estrutura` | `br.com.hackathonone.sentiment_backend` | `backend/` |

Ao consultar código, confirmar em que branch está antes de assumir caminhos de arquivo ou nomes de pacote. Exemplos e trechos encontrados em uma branch podem não existir na `main`.

As branches fora da `main` contêm código que **não compila** (declaram o artefato `spring-boot-starter-webmvc`, que não existe no Maven Central) — não usá-las como referência de implementação.

---

## Layout

```
backend/sentiment-backend/   Spring Boot — API pública (módulo Maven)
  └ src/main/resources/static/index.html   frontend real do projeto
ds-service/                  FastAPI — inferência de ML (POST /predict)
datascience/                 notebooks, datasets e artefatos treinados (.pkl)
contracts/examples/          exemplos de payload
ddl/                         schema PostgreSQL
nginx/docs/                  documentação técnica (arquitetura, ADRs, diagramas)
nginx/docs/adr/              Architecture Decision Records
scripts/                     setup-oci-vm.sh e deploy-oci.sh (provisionamento e deploy)
docker-compose.yml           orquestração: postgres + ds-service + backend
```

Observações de layout que costumam confundir:

- A documentação fica em **`nginx/docs/`**, não em `docs/`.
- O frontend é servido pelo **próprio Spring Boot** (`static/index.html`), não pelo container Nginx. O serviço `frontend` do `docker-compose.yml` aponta para `./frontend/web`, pasta que não existe no repositório.

---

## Comandos

```bash
# Stack completa
docker compose up -d --build

# Backend isolado (perfil dev usa H2 em memória, não precisa de Postgres)
cd backend/sentiment-backend
mvn spring-boot:run          # usar mvn, NÃO ./mvnw
mvn clean package -DskipTests
mvn test

# ds-service isolado
cd ds-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

⚠️ Na `main` existe `.mvn/wrapper/maven-wrapper.properties`, mas **os scripts `mvnw` e `mvnw.cmd` não existem** — `./mvnw` falha. Usar `mvn` direto.

**Perfis Spring:** `dev` (padrão, H2 em memória) e `prod` (PostgreSQL, usado no container). Variáveis relevantes: `SPRING_PROFILES_ACTIVE`, `DS_SERVICE_URL`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.

---

## Endpoints (branch `main`)

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/api/v1/sentiment` | Analisa um texto. Entrada: `{"text": "..."}`. Saída: `{"previsao": "...", "probabilidade": 0.0}` |
| `POST` | `/api/v1/sentiment/batch` | Analisa uma lista de textos |
| `GET` | `/api/v1/stats` | Estatísticas agregadas de todas as análises |
| `GET` | `/api/v1/health` | Saúde da aplicação e da dependência do ds-service |
| `GET` | `/swagger-ui.html` | Documentação OpenAPI |

Contrato interno com o ds-service: `POST /predict` com `{"text": "..."}`, resposta `{"label": "...", "probability": 0.0}`.

---

## Modelo de Machine Learning — características e limitações

Os artefatos treinados ficam em `datascience/`:

| Item | Valor |
|---|---|
| Classificador | `sklearn.linear_model.LogisticRegression` (`sentiment_model.pkl`) |
| Vetorizador | `TfidfVectorizer`, 1.000 features (`tfidf_vectorizer.pkl`) |
| Classes | **`[0, 1]` — binário.** `0 = Negativo`, `1 = Positivo` |
| Treinado com | scikit-learn **1.6.1** |
| Base de treino | 4.044 avaliações do parque Hopi Hari (`hopi_hari (1).csv`) |

Quatro pontos que precisam ser respeitados em qualquer alteração:

1. **São dois objetos, não um pipeline.** O texto precisa passar pelo `TfidfVectorizer` antes do classificador. Carregar apenas o `.pkl` do modelo e chamar `predict_proba` com texto cru não funciona.

2. **A versão do scikit-learn precisa bater com a do treino (1.6.1).** Carregar os artefatos com outra versão emite `InconsistentVersionWarning`, e a documentação do scikit-learn adverte que pode produzir resultados inválidos.

3. **Não existe classe "Neutro" no modelo.** O dataset agrupou notas 1–3 como `0` e 4–5 como `1`. O enum `Sentimento` do backend tem `NEUTRO`, e `Sentimento.fromLabel()` devolve `NEUTRO` como *default silencioso* para qualquer rótulo não reconhecido — então um erro de mapeamento se manifesta como "tudo virou Neutro", sem exceção nem log. Atenção redobrada ao mexer nesse caminho.

4. **O modelo é sensível a acentuação.** O vocabulário TF-IDF contém 168 tokens acentuados; texto sem acento perde essas features e a classificação degrada bastante ("péssimo" classifica corretamente, "pessimo" não). Normalizar acentos apenas na inferência piora o resultado — a correção exigiria consistência entre treino e inferência.

**Domínio de treino:** avaliações de parque de diversões. A acurácia fora desse domínio (e-commerce, redes sociais) é menor. Isso deve constar da documentação voltada ao usuário.

**Modo de degradação:** o `ds-service` mantém um fallback heurístico para o caso de o modelo não carregar. Ele deve permanecer **explícito e observável** — sinalizado no `/health` e em log. Fallback silencioso já causou um problema neste projeto (ver `nginx/docs/adr/ADR-004-*` e `nginx/docs/revisao-tecnica-2026-09.md`); não reintroduzir esse padrão.

⚠️ **Artefatos de modelo e `.gitignore`:** o `.gitignore` historicamente ignorava `*.pkl` e `*.joblib` sob um comentário "decida depois". Essa indefinição foi a causa de o modelo nunca chegar ao serviço em produção. Ao mexer em artefatos de modelo, verificar o que o `.gitignore` faz com eles — um artefato que o serviço precisa não pode depender de existir só na máquina de quem fez o build.

---

## Convenções

**Commits:** Conventional Commits, mensagem em português.
```
feat(backend): adiciona endpoint de estatísticas por período
fix(ds-service): carrega modelo treinado em vez de operar em fallback
docs: corrige lista de endpoints no README
chore: atualiza pin do scikit-learn
```

**Decisões de arquitetura:** registrar como ADR em `nginx/docs/adr/`, seguindo a numeração e a estrutura dos existentes (`ADR-001-microservico-ml.md` e seguintes). Ler um ADR existente antes de criar um novo, para manter o formato.

**Documentação:** o `README.md` descreve o **estado atual** do sistema, para quem vai usar — não narra histórico de mudanças. Histórico e justificativa de decisão vão para os ADRs.

**Branches:** trabalho em branch curta com PR, mesclada na `main` assim que aprovada. Este repositório já sofre de branches abandonadas — não criar mais uma sem fechá-la.

---

## Ambiente de produção

Instância OCI (`sentiment-api-server`, Ubuntu 22.04, `VM.Standard.E2.1.Micro`, região `sa-saopaulo-1`), IP público `152.67.61.11`. O link está publicado no README do portfólio — **não derrubar sem plano de retorno**.

**Deploy:** manual, via `scripts/deploy-oci.sh` executado na própria VM, a partir de um clone do repositório em `~/sentiment-api`. O script roda `docker-compose down` seguido de `docker-compose up -d --build`. Não há pipeline automatizado — todo redeploy exige shell na máquina. O script também espera um `.env.example` que não existe no repositório.

Acesso: a chave SSH original foi perdida. O acesso atual é feito pelo **Cloud Shell da OCI**, que alcança o IP público normalmente. O plugin "Run Command" do Oracle Cloud Agent não está disponível nesta instância, e a tentativa de acesso por *instance console connection* falhou com `Permission denied (publickey)`. Restaurar acesso ao sistema de arquivos do servidor é um trabalho em aberto.
