# Test Report · Hackathon One Sentiment API

Este documento registra, de forma resumida e objetiva:

- o que foi testado;
- como foi testado (tipo de teste);
- o que passou / não passou;
- eventuais pendências.

Ele complementa o `docs/test-strategy.md`, que descreve **como** pretendemos testar. Aqui o foco é **o que de fato foi executado**.

> Observação: este arquivo pode começar com exemplos preenchidos e ser atualizado pela equipe à medida que os testes forem sendo executados.

---

## 1. Escopo do ciclo de testes

- **Versão do sistema:** MVP do Hackathon One Sentiment API.
- **Componentes cobertos:**
    - Backend (Spring Boot) – endpoints principais de comentários, stats, notificações.
    - Microserviço ML (FastAPI) – endpoint `/predict`.
    - Banco – integridade básica das principais tabelas.
    - Frontend – fluxos principais de comprador e vendedor.

---

## 2. Ambiente de teste

- **Backend:**
    - Java 17
    - Spring Boot (versão conforme `pom.xml`)
- **ML Service:**
    - Python 3.x
    - FastAPI + Uvicorn
- **Banco de Dados:**
    - PostgreSQL rodando localmente (ou em container)
    - Schema inicial aplicado via `ddl/schema-postgres.sql`
- **Ferramentas:**
    - Postman / curl para testar API
    - Navegador (Chrome/Firefox) para frontend
    - IDEs (IntelliJ, VS Code) conforme preferência da equipe

---

## 3. Resumo executivo

> Os números abaixo são um modelo. A equipe deve ajustar conforme o que realmente for rodado.

| Tipo de teste            | Planejados | Executados | Aprovados | Reprovados | Observações                              |
|--------------------------|-----------:|-----------:|----------:|-----------:|------------------------------------------|
| Testes de API (backend)  |         15 |         12 |        11 |         1  | Erro conhecido em cenário de borda X     |
| Testes de ML             |          5 |          4 |         4 |         0  | Cobriu posit/neg/ neutro (se houver)     |
| Testes de frontend       |          7 |          5 |         5 |         0  | Testes manuais nos fluxos principais     |
| Testes de banco (SQL)    |          4 |          3 |         3 |         0  | Checks e FKs principais                  |
| Testes de segurança base |          3 |          2 |         2 |         0  | Verificação de segredos no repositório   |

> Em um cenário real de hackathon, é aceitável que alguns testes planejados não sejam executados por falta de tempo.  
> O importante é documentar o que ficou de fora.

---

## 4. Testes de API (Backend)

### 4.1. T-API-001 – POST /api/v1/comentarios (comentário válido)

- **Objetivo:** confirmar que o endpoint aceita um comentário válido e devolve 201.
- **Entrada:**
    - `produtoId` válido;
    - `clienteCompradorId` válido;
    - `texto` com mais de 5 caracteres;
    - `nota` entre 1 e 5.
- **Resultado esperado:**
    - HTTP `201 Created`;
    - corpo com mensagem de sucesso ou dados do comentário;
    - registro em `comentario` e `resultado_analise`.
- **Status:** _a preencher pela equipe_ (`APROVADO` / `REPROVADO`).

---

### 4.2. T-API-002 – POST /api/v1/comentarios sem texto

- **Objetivo:** garantir validação de campo obrigatório.
- **Entrada:**
    - `texto` vazio ou nulo;
    - demais campos válidos.
- **Resultado esperado:**
    - HTTP `400 Bad Request`;
    - JSON com mensagem de erro.
- **Status:** _a preencher pela equipe_.

---

### 4.3. T-API-005 – Integração backend ↔ ML (/predict)

- **Objetivo:** verificar se o backend consegue chamar o microserviço ML e tratar a resposta.
- **Cenário:**
    - comentário válido;
    - ML retornando `label = POSITIVO` e `probability` razoável.
- **Resultado esperado:**
    - backend registra `resultado_analise` com os campos retornados;
    - nenhuma exceção não tratada.
- **Status:** _a preencher pela equipe_.

---

### 4.4. T-API-010 – GET /api/v1/stats?vendedorId=...

- **Objetivo:** checar se o endpoint de estatísticas retorna dados coerentes.
- **Resultado esperado:**
    - JSON com campos como `total`, `positivos`, `negativos`, `neutros` (quando aplicável);
    - soma dos positivos/negativos/neutros = `total` (dentro do subconjunto do vendedor).
- **Status:** _a preencher pela equipe_.

---

### 4.5. T-API-012 – PATCH /api/v1/notificacoes/{id}/ler

- **Objetivo:** verificar a atualização do status de notificação.
- **Resultado esperado:**
    - HTTP `200 OK`;
    - `status` da notificação vira `LIDA`;
    - registro correspondente em `log_evento`.
- **Status:** _a preencher pela equipe_.

---

## 5. Testes do microserviço de ML

### 5.1. T-ML-001 – /predict com texto positivo

- **Entrada:** `"Adorei o produto, chegou antes do prazo"`
- **Esperado:**
    - `label` → algum equivalente a `POS` / `POSITIVO`;
    - `probability` > 0.7 (por exemplo).
- **Status:** _a preencher pela equipe_.

### 5.2. T-ML-002 – /predict com texto negativo

- **Entrada:** `"Péssimo atendimento, produto veio quebrado"`
- **Esperado:**
    - `label` → algum equivalente a `NEG` / `NEGATIVO`;
    - `probability` > 0.7.
- **Status:** _a preencher pela equipe_.

---

## 6. Testes de frontend (fluxos principais)

### 6.1. T-FE-001 – Fluxo “Comprador envia comentário”

- **Passos resumidos:**
    1. Acessar `login.html` e entrar como Cliente - Comprador.
    2. Redirecionar para `comprador.html`.
    3. Selecionar um produto.
    4. Preencher comentário e nota.
    5. Enviar.
- **Esperado:**
    - feedback visual de sucesso (ex.: mensagem “Comentário registrado”);
    - nenhum erro de JS no console.
- **Status:** _a preencher pela equipe_.

### 6.2. T-FE-004 – Fluxo “Vendedor visualiza dashboard”

- **Passos resumidos:**
    1. Acessar `login.html` e entrar como Cliente - Vendedor.
    2. Redirecionar para `vendedor.html`.
    3. Ver cards de stats e lista de comentários.
- **Esperado:**
    - dados coerentes com o que está no banco;
    - notificações pendentes destacadas.
- **Status:** _a preencher pela equipe_.

---

## 7. Testes de banco (SQL)

### 7.1. T-DB-002 – Constraint de nota em `comentario`

- **Objetivo:** garantir que `nota` fora de 1–5 gera erro de banco.
- **Comando exemplo:**

```sql
INSERT INTO comentario (texto_original, nota, produto_id, data_criacao)
VALUES ('Teste', 7, 1, NOW());
````

* **Esperado:** falha com mensagem de violação de CHECK.
* **Status:** *a preencher pela equipe*.

---

## 8. Testes de segurança básicos

### 8.1. T-SEC-001 – Verificação de segredos no repositório

* **Objetivo:** certificar-se de que não há senhas, tokens ou chaves sensíveis versionadas.
* **Passos:**

    * revisar `.gitignore`;
    * procurar por strings suspeitas em `application*.yml`, `.env`, etc.
* **Status:** *a preencher pela equipe*.

---

## 9. Pendências conhecidas

> Se algum teste reprovar ou não for executado, registre aqui.

Exemplos:

* Endpoint X ainda não implementado.
* Integração com serviço externo Y ainda não concluída.
* Falta implementar tratamento de erro mais amigável em `/api/v1/comentarios`.

---

## 10. Conclusão

Este relatório não precisa ser perfeito na primeira versão.
O mais importante é:

* ser honesto sobre o que foi coberto;
* apontar o que ainda falta;
* servir de base para próximos ciclos de melhoria.

À medida que o projeto evoluir, este arquivo pode ser duplicado/versioando por ciclo (`test-report-v1.md`, `test-report-v2.md`, etc.), preservando o histórico.