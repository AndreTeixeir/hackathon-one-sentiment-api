# Estratégia de Testes
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Introdução

### 1.1. Objetivo deste documento

Este documento define a **estratégia de testes** do projeto **Hackathon One Sentiment API**.

Ele explica:

- **o que** será testado;
- **como** será testado (tipo de teste, ferramenta, abordagem);
- **quem** é responsável por cada parte;
- **quando** consideramos que o sistema está “bom o suficiente” para ser demonstrado.

A ideia não é criar um projeto de testes gigante, mas sim algo **organizado, realista e profissional**, que qualquer pessoa da equipe consiga entender e seguir.

### 1.2. Escopo dos testes

Os testes cobrem os seguintes componentes do sistema:

- **Backend Java (Spring Boot)**
    - Endpoints REST `/api/v1/...`
    - Regras de negócio (validação, criação de notificações, logging)
    - Integração com o banco de dados
    - Integração com o microserviço de ML (FastAPI)
- **Microserviço de ML (Python / FastAPI)**
    - Endpoint `/predict`
    - Carregamento do modelo `.pkl`
    - Tratamento de entradas inválidas
- **Banco de Dados (PostgreSQL)**
    - Estrutura do schema
    - Constraints (FK, CHECK)
    - Fluxos principais de gravação (`comentario`, `resultado_analise`, `notificacao`, `log_evento`)
- **Frontend Web (HTML/CSS/JS)**
    - Fluxo de navegação (login → comprador / vendedor)
    - Interações principais (envio de comentário, cadastro de produto, dashboard, notificações, export)

---

## 2. Referências

Este documento se apoia em:

- `docs/requisitos.md` – requisitos funcionais e não funcionais.
- `docs/arquitetura.md` – visão de arquitetura, componentes e integrações.
- `docs/database.md` – modelo de dados e DDL do banco.
- `docs/frontend.md` – comportamento esperado da interface web.
- Diagramas em `docs/uml/`:
    - Contexto / containers;
    - ER do banco;
    - Sequência (comentário, notificações, dashboard);
    - Fluxos de banco.

Quando houver conflito, **os requisitos e os diagramas são a fonte da verdade** para os cenários de teste.

---

## 3. Princípios de teste

1. **Cobrir o fluxo de ponta a ponta do MVP**  
   Do comprador enviando comentário → ML classificando → resultado salvo → notificação → dashboard do vendedor.

2. **Começar pequeno e garantir o essencial**  
   Melhor ter poucos testes **bem pensados** do que dezenas de testes quebrados.

3. **Testar o que é estável**  
   Focar em endpoints, contratos JSON e fluxos que já estão claros.

4. **Automatizar o que for repetitivo**  
   Mas sem forçar automação onde não cabe (por exemplo, teste de UX visual pode ser manual).

5. **Usar dados simples e claros nos testes**  
   Comentários e produtos fáceis de entender, que deixem óbvio se o teste passou ou deu ruim.

---

## 4. Níveis e tipos de teste

### 4.1. Níveis de teste

- **Teste unitário**
    - Escopo: funções, métodos e classes isoladas.
    - Onde:
        - Serviços e validações no backend Java.
        - Funções de ML / pré-processamento no Python.
- **Teste de integração**
    - Escopo: componentes trabalhando juntos.
    - Onde:
        - Backend Java + PostgreSQL;
        - Backend Java + ML (FastAPI);
        - ML + modelo `.pkl`.
- **Teste de sistema / ponta a ponta (E2E)**
    - Escopo: fluxo completo.
    - Onde:
        - Frontend → API → ML → DB → Dashboard.

### 4.2. Tipos de teste

- **Funcionais**
    - Verificar se cada endpoint faz o que promete: inputs válidos/ inválidos, regras de negócio, filtros.
- **Não funcionais (escopo reduzido, mas importante)**
    - Resiliência básica (erros do ML, do banco);
    - Performance leve (ex.: tempo razoável em `/predict` e `/sentiment`);
    - Logging (se os erros importantes estão sendo registrados).
- **Testes de contrato**
    - API ↔ ML: `/predict` (JSON de entrada/saída);
    - Frontend ↔ API: `/api/v1/...`.

---

## 5. Visão geral: Matriz de testes por componente

| Componente                     | Unitário            | Integração                           | Sistema / E2E                              |
| ------------------------------ | ------------------- | ------------------------------------ | ------------------------------------------ |
| Backend Java (API)            | JUnit + Spring Test | API + DB (H2/Postgres), API + ML     | Frontend chamando `/api/v1/...`           |
| Microserviço ML (FastAPI)     | pytest              | FastAPI + modelo `.pkl`              | Via backend (chamadas reais a `/predict`) |
| Banco de Dados (PostgreSQL)   | – (SQL manual)      | Migratons / DDL + inserções de teste | Validado pelos fluxos de negócio          |
| Frontend Web (HTML/JS)        | – (JS opcional)     | –                                    | Testes manuais navegando pelas telas      |

---

## 6. Backend Java (Spring Boot)

### 6.1. Ferramentas

- **JUnit 5**
- **Spring Boot Test**
- **MockMvc** para testar controllers sem subir servidor completo.
- **Banco em memória (H2)** ou PostgreSQL em Docker para testes de integração.

### 6.2. Escopo de testes unitários

Testar principalmente:

- Serviços:
    - `SentimentService`
        - Chamada ao ML (mockada).
        - Interpretação do resultado (mapeamento `label` → `Sentimento`).
        - Cálculo de `ehCritico` (por exemplo, probabilidade > limite).
        - Criação de `ResultadoAnalise` e `Notificacao` (regra de negócio).
    - `ProdutoService`
        - Validação de cadastro de produto.
    - `NotificacaoService`
        - Marcar notificação como lida.
- Validações:
    - Requests recebidos nos controllers (`@Valid`, regras manuais).
- Lógica de “stats”:
    - Cálculo de agregados (positivos / negativos / neutros) a partir de dados simulados.

#### Exemplos de casos de teste unitário (backend)

- `SentimentService`:
    - Dado: comentário com texto longo suficiente.
    - Quando: o ML retorna `label = NEGATIVE`, `probability = 0.91`.
    - Então: `Sentimento.NEGATIVO`, `probabilidade = 0.91`, `ehCritico = true`, notificação é criada para o vendedor.
- `NotificacaoService`:
    - Dado: notificação `PENDENTE`.
    - Quando: chamamos “marcarComoLida(id)”.
    - Então: status vira `LIDA` e `dataEnvio` é preenchida.

### 6.3. Testes de integração (backend)

#### 6.3.1. API + DB

Objetivo: garantir que as entidades JPA estão alinhadas com o schema SQL, e que o fluxo grava nas tabelas corretas.

Sugestão:

- Subir Spring Boot com:
    - H2 em modo compatível com PostgreSQL ou
    - PostgreSQL real em Docker (se possível).
- Usar `@SpringBootTest` e testar endpoints com `TestRestTemplate` ou `MockMvc`.

Casos típicos:

1. **Fluxo `/api/v1/comentarios` sem chamar ML (ML mockado)**:
    - Enviar payload válido.
    - Verificar:
        - linha em `comentario`;
        - log INFO em `log_evento` para “Comentário salvo”.
2. **Fluxo `/api/v1/comentarios` com ML real (se tiver ML ativo no ambiente de teste)**:
    - Enviar texto negativo.
    - Conferir:
        - `comentario` salvo;
        - `resultado_analise` criado com `NEGATIVO`;
        - `notificacao` criada se `eh_critico = TRUE`.

3. **Fluxo `/api/v1/notificacoes/{id}/ler`**:
    - Criar notificação `PENDENTE`.
    - Chamar endpoint.
    - Verificar no DB:
        - `status = 'LIDA'`;
        - `data_envio` preenchida.

#### 6.3.2. API + ML (contrato `/predict`)

Testar:

- Erros quando o ML está fora do ar (timeout, erro 500);
- Formato do JSON enviado ao ML;
- Tratamento de resposta do ML.

Exemplo:

- Simular ML devolvendo:

```json
{
  "label": "POSITIVE",
  "probability": 0.93,
  "model_name": "logreg-tfidf",
  "model_version": "v1"
}
````

* Verificar que a API converte para:

```json
{
  "previsao": "POSITIVO",
  "probabilidade": 0.93
}
```

---

## 7. Microserviço de ML (Python / FastAPI)

### 7.1. Ferramentas

* **pytest**
* Possivelmente **httpx** ou o client de testes do FastAPI (`TestClient`).

### 7.2. Testes unitários

Escopo:

* Funções de carregamento do modelo (`load_model()`).
* Funções de pré-processamento e inferência (chamada ao pipeline TF-IDF + Logistic Regression).

Casos:

* Garantir que:

    * modelo é carregado só uma vez (se esse for o design);
    * textos curtos ou vazios são rejeitados com mensagem clara.
* Verificar:

    * saída de `predict(text)` sempre com `label` dentro dos valores esperados (`POSITIVE`, `NEGATIVE`, `NEUTRAL` ou equivalente);
    * `probability` entre 0 e 1.

### 7.3. Testes de integração (FastAPI)

Com `TestClient`:

* `POST /predict` com:

    * texto claramente positivo;
    * texto claramente negativo;
    * texto neutro.
* Verificar:

    * presença de campos `label`, `probability` no JSON;
    * status HTTP correto (200 em sucesso, 400 em entrada inválida).

Testes de erro:

* `POST /predict` sem campo `text` → 422/400 com mensagem clara.
* `POST /predict` com texto muito curto → 400 (caso essa regra exista).

---

## 8. Banco de Dados (PostgreSQL)

### 8.1. Validação do schema

* Executar `ddl/schema-postgres.sql` em uma instância vazia.
* Verificar se todas as tabelas são criadas:

    * `cliente`, `produto`, `comentario`, `resultado_analise`, `notificacao`, `modelo_ml`, `dataset_registro`, `log_evento`.

### 8.2. Testes de constraints e relacionamentos

Casos manuais (SQL):

1. **FK de `produto` → `cliente` (vendedor)**

    * Inserir `produto` com `cliente_vendedor_id` inexistente → deve falhar.
2. **FK de `comentario` → `produto`**

    * Inserir `comentario` com `produto_id` inexistente → deve falhar.
3. **CHECK de `nota`**

    * `nota = 0` ou `nota = 6` → deve falhar.
4. **CHECK de `sentimento` em `resultado_analise`**

    * Inserir `sentimento = 'MUITO_POSITIVO'` → deve falhar.
5. **CHECK de `status` e `canal` em `notificacao`**

    * `status = 'XPTO'` → falha.
    * `canal = 'SMS'` (se não previsto) → falha.

### 8.3. Testes de fluxo no banco

Baseados nos diagramas:

* **Fluxo novo comentário → resultado_analise → notificacao**:

    * Inserir manualmente um `comentario`.
    * Simular inserção em `resultado_analise` com `NEGATIVO` e `eh_critico = true`.
    * Criar `notificacao` vinculada.
    * Verificar que todas as FKs estão consistentes.

* **Fluxo dashboard e export**:

    * Criar dados de teste (1 vendedor, alguns produtos, comentários com diversos sentimentos).
    * Rodar as queries que a API deve usar:

        * agregação por sentimento;
        * join entre `comentario`, `resultado_analise`, `produto`.

---

## 9. Frontend (HTML/CSS/JS)

### 9.1. Testes manuais (prioritário no MVP)

Fluxos principais:

1. **Login como comprador**

    * Abre `login.html`.
    * Seleciona “Cliente - Comprador”.
    * Vai para `comprador.html`.
2. **Login como vendedor**

    * Mesma ideia, indo para `vendedor.html`.
3. **Comprador vê produtos**

    * Em `comprador.html`, lista de produtos aparece (vinda da API ou mock).
4. **Comprador envia comentário**

    * Seleciona produto, escreve comentário, envia.
    * Recebe mensagem de sucesso.
5. **Vendedor vê dashboard**

    * Em `vendedor.html`, aba “Dashboard” carrega stats.
6. **Vendedor vê notificações**

    * Aba “Notificações” mostra críticas.
    * “Marcar como lida” atualiza visualmente.
7. **Vendedor exporta JSON**

    * Botão de export → baixa arquivo `.json`.

### 9.2. (Opcional) Testes automatizados de JS

Se houver tempo:

* Criar funções JS isoladas (ex.: `buildComentarioPayload()`).
* Testar com Jest (opcional) para garantir que JSON enviado bate com o esperado.

---

## 10. Testes funcionais por endpoint

### 10.1. `/api/v1/comentarios` (POST)

**Cenários principais:**

1. Comentário válido:

    * texto ≥ tamanho mínimo;
    * nota entre 1 e 5 ou nula.
    * Esperado:

        * 201 Created;
        * comentário salvo;
        * log INFO “Comentário salvo”.
2. Texto ausente:

    * Esperado: 400 com mensagem amigável.
3. Texto muito curto:

    * 400 com mensagem de validação.
4. Nota fora do intervalo:

    * 400 e nenhuma gravação no banco.
5. Erro no ML:

    * ML fora do ar ou erro 500;
    * API deve:

        * registrar log ERROR;
        * retornar 503 ou 500 com mensagem clara.

### 10.2. `/api/v1/sentiment` (se público) ou fluxo interno de sentimento

* Entradas válidas e inválidas.
* Garantir:

    * retorno com `previsao` e `probabilidade`;
    * `probabilidade` entre 0 e 1.

### 10.3. `/api/v1/produtos` (GET / POST)

* GET:

    * com e sem `vendedorId` (se aplicável);
    * retorno esperado: lista vazia ou com produtos do vendedor.
* POST:

    * produto válido → 201 + gravado;
    * preço negativo ou campos vazios → 400.

### 10.4. `/api/v1/stats`

* Sem dados:

    * total = 0, porcentagens = 0.
* Com dados variados:

    * conferir se agregados batem com o que está no banco (pelo menos em 1 cenário de teste).

### 10.5. `/api/v1/comments`

* Filtrar por vendedor.
* Garantir:

    * apenas comentários dos produtos daquele vendedor;
    * incluir sentimento, probabilidade, flags (`ehCritico`).

### 10.6. `/api/v1/notificacoes` e `/api/v1/notificacoes/{id}/ler`

* Lista de notificações:

    * retorno correto (pendentes + lidas).
* Marcar como lida:

    * muda status para `LIDA`;
    * atualiza `data_envio`;
    * gera log de INFO.

### 10.7. `/api/v1/export`

* Verificar:

    * retorno em JSON bem formado;
    * contenha somente dados do vendedor autenticado / informado via parâmetro;
    * preserve estrutura combinada nos requisitos.

---

## 11. Testes de ML (modelo e comportamento)

Além dos testes de API do ML:

* Confirmar que:

    * para um conjunto pequeno de frases “fixas” (positivas, negativas), o modelo responde consistentemente;
    * os rótulos usados pelo modelo (`POSITIVE`, `NEGATIVE`, etc.) são corretamente mapeados para o enum `Sentimento` no backend (`POSITIVO`, `NEGATIVO`, `NEUTRO`).
* Testar valores-limite de probabilidade:

    * Definir claramento o que é “crítico” (por exemplo, `NEGATIVO` com `probabilidade >= 0.8`).
    * Verificar se esse limiar está sendo respeitado pelo backend ao criar `NOTIFICACAO`.

---

## 12. Dados de teste

### 12.1. Usuários

* 1 Cliente - Vendedor (ex.: `Loja XYZ`, `id = 2`).
* 1–2 Clientes - Compradores.

### 12.2. Produtos

* Pelo menos 2–3 produtos com nomes simples:

    * “Fone Bluetooth”
    * “Notebook Gamer”
    * “Cafeteira Elétrica”

### 12.3. Comentários

* Exemplos de comentários:

    * Positivo: “Adorei, chegou antes do prazo e funciona muito bem.”
    * Negativo: “Chegou quebrado, péssima qualidade.”
    * Neutro: “Produto ok, nada demais.”

Relacionar esses comentários a produtos diferentes e guardar esses exemplos também para demonstrar o sistema.

---

## 13. Automação e CI

### 13.1. Pipelines mínimos recomendados (GitHub Actions)

* Backend:

    * `mvn test` (JUnit + testes de integração leves).
* ML service:

    * `pytest` nos testes básicos de `/predict`.
* Opcionalmente:

    * linting de Python (`flake8` ou `black` – se configurado);
    * checks de formatação em Java (Spotless, etc.).

### 13.2. Política simples de qualidade

* Só fazer merge em `main` se:

    * testes de backend estiverem passando;
    * testes de ML estiverem passando;
    * não houver erros críticos abertos relacionados aos fluxos principais.

---

## 14. Critérios de aceite (para o MVP)

Podemos considerar o MVP “testado e pronto para demo” quando:

1. Fluxo **Comprador → Comentário → ML → DB** funciona do começo ao fim, sem erro.
2. Fluxo **Vendedor → Dashboard** mostra estatísticas condizentes com dados reais do banco.
3. Notificações são criadas para comentários críticos e aparecem no painel do vendedor.
4. `GET /export` retorna JSON válido com comentários e sentimentos.
5. Não há erros graves no console da API durante esses fluxos.
6. Pelo menos:

    * testes unitários básicos do backend estão implementados (validação, SentimentService, NotificacaoService),
    * testes do `/predict` no ML estão implementados.

---

## 15. Riscos e fora de escopo

* **Carga alta / performance pesada**:

    * Não faz parte do escopo medir performance sob grande volume de requisições.
    * Apenas serão feitas verificações “de bom senso” (resposta em tempo aceitável em ambiente de dev).
* **Segurança avançada (auth, autorização)**:

    * MVP não implementa autenticação robusta (login com senha, JWT etc.).
    * Os testes de segurança concentram-se em:

        * não expor segredos;
        * não quebrar com entradas estranhas (validação de input).
* **Cobertura total de frontend com testes automatizados**:

    * Por ser um hackathon / MVP, a maioria dos testes de UI será manual.

---

## 16. Conclusão

Esta estratégia de testes busca um equilíbrio entre:

* **profissionalismo** (organização, rastreabilidade, clareza de responsabilidades);
* **viabilidade** (tempo limitado de hackathon e escopo do MVP).

Seguindo este documento, qualquer pessoa da equipe consegue:

* entender o que precisa ser testado;
* onde escrever testes automatizados;
* quais cenários manuais precisam ser validados antes da apresentação.
