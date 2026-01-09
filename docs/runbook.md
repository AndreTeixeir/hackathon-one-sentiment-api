# Runbook · Hackathon One Sentiment API

Este documento é um manual rápido de operação do sistema.  
A ideia é: se alguém precisa “subir, derrubar ou investigar” o sistema, consiga fazer sem depender da memória de quem implementou.

---

## 1. Componentes do sistema

- **Backend Java** – API principal em `/backend`
- **Microserviço ML** – FastAPI com `/predict` em `/datascience/ml_service`
- **Banco de Dados** – PostgreSQL com o schema definido em `ddl/schema-postgres.sql`
- **Frontend Web** – HTML/CSS/JS em `/frontend/web`

---

## 2. Como subir tudo em ambiente local

### 2.1. Passo 1 – Banco de dados

1. Subir um PostgreSQL local (por exemplo, com Docker ou instalação local).
2. Criar um banco, ex.: `sentimentdb`.
3. Aplicar o schema:

```bash
psql -h localhost -U <usuario> -d sentimentdb -f ddl/schema-postgres.sql
````

4. Ajustar o `application-dev.yml` (ou equivalente) no backend com:

    * URL do banco;
    * usuário;
    * senha.

### 2.2. Passo 2 – Microserviço de ML

```bash
cd datascience/ml_service
pip install -r requirements.txt
uvicorn app:app --reload --port 8000
```

Verificar:

* Abrir `http://localhost:8000/docs` e confirmar que `/predict` está disponível.

### 2.3. Passo 3 – Backend

```bash
cd backend
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

Backend deve subir em algo como:

* `http://localhost:8080`

### 2.4. Passo 4 – Frontend Web

Opção 1 (mais simples):

* Abrir os arquivos diretamente no navegador:

    * `frontend/web/login.html`
    * `frontend/web/comprador.html`
    * `frontend/web/vendedor.html`

> Verificar se `config.js` aponta para `http://localhost:8080/api/v1`.

Opção 2 (server estático):

```bash
cd frontend/web
# se tiver Node instalado
npx serve .
```

---

## 3. Health-check manual

### 3.1. Verificar ML Service

```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{"text": "Exemplo positivo"}'
```

* Esperado: resposta JSON com `label` e `probability`.

### 3.2. Verificar Backend

```bash
curl http://localhost:8080/actuator/health
```

> Se o actuator estiver habilitado – senão, pode usar um endpoint simples como `/api/v1/health` (se implementado).

### 3.3. Verificar conexão com o banco

* Fazer um `POST /api/v1/comentarios` e checar se registros aparecem em `comentario` e `resultado_analise`.

---

## 4. Rotinas comuns

### 4.1. “Nada funciona, o que eu faço primeiro?”

1. Confirmar se o banco está rodando.
2. Confirmar se o ML Service está rodando.
3. Confirmar se o backend consegue ligar no banco (logs do backend).
4. Confirmar se o frontend está apontando para o endereço certo (`API_BASE_URL` em `config.js`).

### 4.2. Backend não sobe

* Checar logs no console:

    * erro de conexão com o banco?
    * porta já em uso (8080)?
* Possíveis ações:

    * ajustar URL/credenciais do banco;
    * encerrar processos que estejam usando a porta (`8080` ou outra);
    * revisar dependências (rodar `mvn clean install`).

### 4.3. ML Service não responde

* Checar se `uvicorn` está rodando.
* Rodar diretamente:

```bash
cd datascience/ml_service
uvicorn app:app --reload --port 8000
```

* Verificar se o caminho para o `.pkl` está correto e se o arquivo existe.
* Ver erros no console do ML Service (ex.: problema ao carregar o modelo).

### 4.4. Erro ao enviar comentário

* Verificar:

    * se o produtoId usado existe no banco;
    * se `clienteCompradorId` é válido;
    * se o payload JSON está conforme especificação.
* Checar logs de backend (tabela `log_evento` ou console).

---

## 5. Incidentes típicos e como reagir

### 5.1. ML fora do ar

**Sintomas:**

* Backend retorna erro (500) ao tentar analisar comentários.
* Logs mencionam falha de conexão com o ML.

**Ações:**

1. Reiniciar ML Service (`uvicorn app:app --reload --port 8000`).
2. Confirmar `/predict` com curl.
3. Retentar a operação no backend.

### 5.2. Banco fora do ar

**Sintomas:**

* Backend lança erros de conexão com o banco.
* Nenhum endpoint consegue ler/gravar dados.

**Ações:**

1. Reiniciar o serviço PostgreSQL.
2. Confirmar conexão via `psql` ou ferramenta gráfica.
3. Verificar se credenciais não foram alteradas.

### 5.3. Frontend sem dados

**Sintomas:**

* Tela do vendedor não mostra produtos, stats ou comentários.
* Erros de CORS ou 404 no console do navegador.

**Ações:**

1. Verificar se `API_BASE_URL` está correta:

    * host/porta corretos.
2. Checar no console se as requisições estão indo para a URL certa.
3. Testar os endpoints diretamente via Postman/curl.

---

## 6. Logs

### 6.1. Onde estão

* Backend:

    * console (durante desenvolvimento);
    * tabela `log_evento` no banco (para eventos relevantes).
* ML Service:

    * console.
* Frontend:

    * console do navegador (F12) para erros de JS.

### 6.2. O que procurar

* Erros com `nivel = 'ERROR'` em `log_evento`.
* Mensagens de log com `origem = 'API'` ou `'ML_SERVICE'`.

---

## 7. Checklist antes de apresentar o sistema

1. Subir banco e aplicar schema.
2. Subir ML Service e validar `/predict`.
3. Subir backend e validar:

    * `/api/v1/produtos` (lista vazia ou com dados de teste);
    * `/api/v1/stats` (vendedor de teste).
4. Subir frontend e conferir:

    * login → comprador → fluxo de envio de comentário;
    * login → vendedor → dashboard e notificações.
5. Ter pelo menos 2–3 comentários cadastrados para a demo.

---

Este runbook é uma base.
Se o projeto evoluir para ambiente real (cloud, múltiplos ambientes), vale expandir com:

* passos de deploy por ambiente;
* checklists de segurança;
* contato de suporte (quem “está de plantão”).
