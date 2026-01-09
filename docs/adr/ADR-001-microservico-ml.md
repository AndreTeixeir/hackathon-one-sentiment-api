# ADR-001 – Uso de microserviço Python para o modelo de ML
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025
**Status:** Aprovado  
**Escopo:** Integração entre back-end Java (Spring Boot) e modelo de Machine Learning (Python)

---

## 1. Contexto

O objetivo do projeto Hackathon One Sentiment API é disponibilizar uma API que receba comentários em português, aplique um modelo de análise de sentimento e retorne uma classificação (`POSITIVO`, `NEGATIVO`, `NEUTRO`) com probabilidade associada.

O time de desenvolvimento está dividido em subgrupos:

- **Data Science**: trabalha em Python (notebooks, pandas, scikit-learn, joblib).
- **Backend**: trabalha em Java 17 + Spring Boot (REST, JPA, validação).
- **Frontend**: trabalha com HTML/CSS/JS (e opcionalmente Streamlit).
- **DevOps**: cuida de containers, docker-compose, futura integração com OCI.

O modelo de ML será treinado em Python, serializado (`.pkl`) e precisa ser consumido pela API Java. Havia duas opções principais:

1. Embutir o modelo diretamente no projeto Java (via ONNX, tradução ou libs de ML em Java).
2. Expor o modelo em um **microserviço em Python** (FastAPI/Flask) e a API Java fazer chamadas HTTP para esse serviço.

---

## 2. Problema

Precisamos integrar a API Java com o modelo de ML, garantindo:

- **Produtividade**: o time de DS deve trabalhar com as ferramentas que domina (Python, notebooks, scikit-learn).
- **Clareza de responsabilidades**: separar bem “lidar com dados e modelo” de “regras de negócio e API pública”.
- **Facilidade de evolução**: permitir troca de modelo ou ajuste de pipeline sem quebrar o back-end.
- **Simplicidade** dentro do contexto de um hackathon (tempo limitado e muita coisa para integrar).

Qual abordagem de integração adotar?

---

## 3. Decisão

O modelo de ML será exposto por um **microserviço independente em Python**, usando FastAPI (ou Flask), com um endpoint REST:

```http
POST /predict
Content-Type: application/json
````

```json
{
  "text": "Exemplo de comentário em português."
}
```

Resposta esperada do microserviço:

```json
{
  "label": "NEGATIVE",
  "probability": 0.82,
  "model_name": "hackathon-one-sentiment-ptbr",
  "model_version": "v1"
}
```

A API Java, por sua vez:

* recebe o comentário do usuário (via `/api/v1/comentarios` ou `/api/v1/sentiment`);
* salva o comentário no banco;
* chama o microserviço Python (`POST /predict`);
* mapeia `label` → enum `Sentimento` (`POSITIVO`, `NEGATIVO`, `NEUTRO`);
* grava o resultado em `resultado_analise` e, se necessário, gera `notificacao`.

---

## 4. Alternativas consideradas

### 4.1. Embutir o modelo diretamente no Java (sem microserviço)

**Ideia:** exportar o modelo em um formato como ONNX, ou reimplementar a pipeline em Java (usando algumas libs de ML), para ser carregado dentro do próprio Spring Boot.

**Prós:**

* Sem overhead de rede entre API e modelo.
* Apenas um serviço a ser monitorado e escalado.
* Menos componentes em termos de infraestrutura.

**Contras:**

* A equipe de DS teria que se adaptar a ferramentas que talvez não domine (ML em Java).
* A pipeline TF-IDF + LogisticRegression teria que ser reimplementada ou convertida, o que consome tempo num cenário de hackathon.
* Cada alteração de modelo exigiria rebuild e redeploy do backend Java.
* A curva de aprendizado para integrar ONNX/ML em Java é maior que a de expor FastAPI com um `.pkl`.

### 4.2. Chamar diretamente um ambiente notebook (Colab/Notebook em produção)

**Ideia:** manter o modelo rodando “dentro de um notebook” e de alguma forma integrar isso com a API.

**Prós:**

* Quase zero esforço adicional para DS (usa o próprio ambiente onde já treina).

**Contras:**

* Muito frágil e pouco profissional.
* Dificuldade para automatizar deploy, logs e escalabilidade.
* Não é uma arquitetura aceitável para produção ou mesmo para um MVP sólido.

### 4.3. Microserviço Python – decisão escolhida

**Prós:**

* Time de DS usa o ecossistema natural (Python, scikit-learn, joblib).
* Deploy simples com FastAPI/Flask, bem documentado.
* API Java continua focada em regras de negócio, logs, banco, notificações.
* Possibilidade futura de ter **mais de um modelo** exposto (ex.: versão PT-BR, versão ES, etc.).
* A troca do modelo (v1 → v2) pode ser feita no microserviço com impacto mínimo para o back-end (contrato JSON permanece).

**Contras:**

* Mais um serviço para subir, monitorar e documentar.
* Latência de rede entre Java e Python (mesmo que em localhost ou docker-compose).
* Depende de lidar com timeouts, erros de comunicação e retries.

---

## 5. Impactos da decisão

### 5.1. No código Java (Spring Boot)

* Criação de um cliente HTTP (por exemplo, usando `WebClient` ou `RestTemplate`) para chamar o endpoint `/predict`.
* Uma classe de DTO para requisição ao ML (ex.: `MlPredictRequest` com campo `text`).
* Uma classe de DTO para resposta do ML (ex.: `MlPredictResponse` com `label`, `probability`, `modelName`, `modelVersion`).
* Tratamento explícito de erros:

    * indisponibilidade do ML;
    * timeouts;
    * respostas inválidas.

### 5.2. No microserviço Python

* Definição clara da interface `/predict`, documentada em `docs/contrato-api-ml.md`.
* Carregamento do modelo `.pkl` uma única vez na inicialização, reaproveitando em cada requisição.
* Logging mínimo para:

    * requisições recebidas,
    * erros,
    * latência do modelo.

### 5.3. Na documentação e testes

* Documentar o contrato Java ↔ Python (input/output JSON e códigos de erro).
* Criar testes de integração:

    * em Java: teste chamando o microserviço real (ambiente de dev/docker).
    * em Python: testes unitários e de API para o endpoint `/predict`.

### 5.4. Em DevOps / deploy

* `docker-compose.yml` deve considerar três serviços principais:

    * `backend` (Spring Boot),
    * `ml-service` (FastAPI),
    * `db` (PostgreSQL).
* Configurações como host/porta do microserviço vão em variáveis de ambiente ou em `application.yml` (ex.: `ml.service.base-url: http://ml-service:8000`).

---

## 6. Consequências (curto e longo prazo)

### 6.1. Benefícios imediatos

* Permite que as equipes de DS e Backend trabalhem **em paralelo**, com base em um contrato de integração bem definido.
* Reduz risco de travar o projeto tentando “forçar” o modelo para dentro do ecossistema Java.
* Mantém o foco do backend em regras de negócio, logs e segurança.

### 6.2. Riscos e mitigação

* **Risco:** ML fora do ar → API não consegue classificar.
  **Mitigação:**

    * definir mensagens de erro claras para o vendedor;
    * logar o incidente em `log_evento`;
    * eventualmente, implementar fila de reprocessamento de comentários.

* **Risco:** contrato JSON se quebra com mudanças no microserviço.
  **Mitigação:**

    * versionar o endpoint (`/predict/v1`, `/predict/v2` se necessário);
    * manter testes de contrato simples.

### 6.3. Futuras evoluções

* Adicionar novos modelos (por idioma ou tipo de texto) no mesmo microserviço.
* Incluir explicabilidade simples (palavras mais influentes) como campo adicional na resposta JSON.
* Deploy do microserviço Python em ambiente separado (OCI) com escalabilidade independente da API Java.

---

## 7. Quando revisitar esta decisão

Essa decisão deve ser revisitada se:

* o projeto deixar de ser apenas um MVP/hackathon e migrar para produção com volumes altos de requisições;
* a equipe passar a ter forte expertise em ML em Java, tornando viável unificar tudo em um único serviço;
* aparecer uma exigência específica do cliente que impeça uso de múltiplos serviços (restrição de infraestrutura, por exemplo).

Enquanto isso não acontecer, o uso do microserviço Python é considerado a abordagem padrão para o Hackathon One Sentiment API.
