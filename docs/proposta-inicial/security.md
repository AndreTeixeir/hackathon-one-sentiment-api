# Segurança · Hackathon One Sentiment API
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Objetivo deste documento

Este documento descreve a **visão de segurança** do projeto **Hackathon One Sentiment API**, cobrindo:

- como tratamos **dados de clientes** (compradores e vendedores);
- práticas para **proteger o código e o repositório**;
- cuidados básicos para **API, banco de dados, ML e frontend**;
- riscos conhecidos e o que ficou **fora de escopo** no MVP.

A ideia é ser **prático**: nada de teoria demais, e sim um guia que qualquer pessoa da equipe consiga entender e seguir, hoje ou no futuro.

---

## 2. Escopo de segurança

A segurança aqui é pensada em torno destes componentes:

- **Frontend Web** (`/frontend/web`)
    - Páginas `login.html`, `comprador.html`, `vendedor.html`
- **Backend Java (Spring Boot)** (`/backend`)
    - Endpoints `/api/v1/...`
    - Integração com banco e com o serviço de ML
- **Microserviço de ML (FastAPI)** (`/ml_service`)
    - Endpoint `/predict`
- **Banco de Dados (PostgreSQL)** (`schema-postgres.sql` em `/ddl`)
    - Tabelas: `cliente`, `produto`, `comentario`, `resultado_analise`, `notificacao`, `modelo_ml`, `dataset_registro`, `log_evento`
- **Repositório Git / GitHub**
    - Código, histórico de commits, pipelines de CI (se houver)

> Autenticação/autorização forte (login real, controle de sessão, etc.) **não** faz parte do MVP, mas este documento já aponta caminhos para evoluir com segurança no futuro.

---

## 3. Princípios gerais de segurança

1. **Mínimo necessário**  
   O sistema expõe apenas o que é necessário. Exemplo importante:
    - O **Comprador** não vê o sentimento calculado pelo modelo.
    - Essas informações são **somente para o Vendedor** (negócio/empresa).

2. **Menos é mais (exposição)**
    - Apenas endpoints necessários são expostos.
    - Logs não devem conter dados sensíveis demais (privacidade básica).

3. **Sem segredos no código**
    - Nada de senha de banco ou token em código-fonte ou commit.

4. **Valide entrada, desconfie de tudo**
    - Tudo que vem de fora (requests, texto da API, parâmetros) é considerado “não confiável”.

5. **Ambientes separados**
    - Comportamento de desenvolvimento ≠ comportamento de produção.
    - Logs, níveis de detalhe e URLs diferentes por profile (`dev`, `oci`, etc.).

---

## 4. Dados e classificação

### 4.1. Principais categorias de dados

- **Dados de Cliente (cliente)**
    - `nome`, `email`, `tipo_cliente` (COMPRADOR / VENDEDOR / ADMIN)
    - Sensibilidade: **média**
        - Devem ser protegidos contra acesso indevido, mas não são informações hiper-críticas como senha ou documento.

- **Dados de Produto (produto)**
    - Nome, descrição, tags, etc.
    - Sensibilidade: **baixa**

- **Comentários (comentario)**
    - `texto_original` (texto livre do cliente)
    - Podem conter:
        - opiniões,
        - reclamações,
        - eventualmente algum dado pessoal digitado pelo cliente.
    - Sensibilidade: **média**
        - Devemos assumir que pode haver informação pessoal digitada pelo usuário.

- **Resultados de Análise (resultado_analise)**
    - `sentimento` (POSITIVO/NEGATIVO/NEUTRO)
    - `probabilidade`, `eh_critico`
    - Sensibilidade: **média**, mas com impacto de negócio para o Vendedor.

- **Notificações (notificacao)**
    - Ligadas a resultados críticos (comentários negativos)
    - Sensibilidade: **média**, pois indicam situações de risco para o negócio.

- **Dataset de treino (dataset_registro)**
    - Textos de origem pública (ex.: Kaggle) + rótulos originais.
    - Sensibilidade: **baixa a média**, mas é bom evitar expor id_externo que identifique usuário real.

- **Logs (log_evento)**
    - Mensagens de erro, info, warn, payloads resumidos.
    - Sensibilidade: **média**, porque podem conter contexto de requisições.

### 4.2. Regras simples

- Não registrar:
    - senhas (não temos no MVP),
    - tokens,
    - chaves secretas,
    - qualquer dado que não seja necessário para o propósito do sistema.

- Quando logar algo:
    - preferir IDs (`cliente_id`, `comentario_id`) em vez de textos inteiros.

---

## 5. Segurança do repositório (Git / GitHub)

### 5.1. Sem segredos em commit

Nunca comitar:

- strings de conexão com usuário/senha (ex.: `jdbc:postgresql://... user=... password=...`);
- chaves da OCI, tokens de e-mail/SMS, credenciais de qualquer serviço externo;
- arquivos `.env`, `.pfx`, `.pem` ou equivalentes.

Arquivos que **devem estar no `.gitignore`**:

```text
# Config locais
.env
*.local
application-local.yml
application-local.properties

# Build outputs
/target/
/build/

# Python
__pycache__/
*.pyc

# Modelos ML
*.pkl

# IDEs
.vscode/
.idea/
*.iml
````

### 5.2. GitHub Secrets

Se for usar pipelines (GitHub Actions):

* tokens e senhas vão em **Settings → Secrets and variables → Actions**;
* o pipeline lê esses valores via variáveis de ambiente, nunca direto no YAML.

### 5.3. Branches

* Usar `main` como branch estável.
* Criar branches de feature:

    * `feature/frontend-ui`
    * `feature/backend-sentiment`
    * `feature/ml-service`
* Revisar antes de dar merge (mesmo que seja só pra treinar o hábito).

---

## 6. Segurança do Backend (API Java)

### 6.1. Exposição de endpoints

**Principais endpoints** (resumo):

* `POST /api/v1/comentarios`
* `GET  /api/v1/stats`
* `GET  /api/v1/comments`
* `GET  /api/v1/notificacoes`
* `PATCH /api/v1/notificacoes/{id}/ler`
* `GET  /api/v1/export`
* `GET  /api/v1/produtos` / `POST /api/v1/produtos`
* (Opcional) `POST /api/v1/sentiment` público

**Cuidados**:

* Não expor endpoints internos usados apenas pelo sistema (no MVP, tudo pode ficar aberto para teste, mas deixe anotado no código que, em produção, devem ser protegidos com autenticação/ACL).
* Se houver `/actuator`:

    * Em produção, restringir ou desativar endpoints sensíveis.

### 6.2. Validação de entrada

Regras importantes:

* Em `POST /api/v1/comentarios`:

    * `texto` obrigatório, tamanho mínimo (ex.: 3–5 caracteres);
    * `nota` entre 1 e 5 (ou nula).
* Em `POST /api/v1/produtos`:

    * campos obrigatórios (nome, preço, clienteVendedorId);
    * preço positivo.

No código:

* Usar `@Valid`, `@NotBlank`, `@Size`, `@Min`, `@Max`, etc.
* Não confiar em dados vindos do frontend/mundo externo.

### 6.3. Tratamento de erros

* Nunca devolver **stacktrace** diretamente para o cliente.
* Usar um `@ControllerAdvice` / `@RestControllerAdvice` para:

    * mapear exceções para códigos HTTP adequados;
    * retornar mensagens amigáveis, sem expor detalhes internos.

Exemplo (simplificado):

```json
{
  "erro": "Requisição inválida",
  "detalhes": [
    "texto: tamanho mínimo é 5 caracteres"
  ]
}
```

### 6.4. Logs (log_evento)

Regras:

* `nivel` ∈ {`INFO`, `WARN`, `ERROR`}
* `origem` ∈ {`API`, `ML_SERVICE`, `FRONTEND`, `DB`}

Boas práticas:

* Logar:

    * acessos importantes (ex.: dashboard do vendedor);
    * falhas de integração com ML;
    * erros de validação relevantes.
* Não logar:

    * texto inteiro do comentário em todo erro;
    * dados pessoais que não acrescentem nada ao diagnóstico.

---

## 7. Segurança do Microserviço de ML (FastAPI)

### 7.1. Exposição do `/predict`

* O endpoint `/predict` deve ser visto como um serviço **interno**, chamado apenas pelo backend Java.
* Em ambiente real:

    * restringir a rede (por IP, VPC, security group);
    * ou usar autenticação simples entre serviços (ex.: token de serviço).

No MVP:

* Documentar bem o contrato:

    * entrada:

      ```json
      { "text": "Exemplo de comentário" }
      ```

    * saída:

      ```json
      {
        "label": "NEGATIVE",
        "probability": 0.91,
        "model_name": "logreg-tfidf",
        "model_version": "v1"
      }
      ```

### 7.2. Entrada maliciosa

Mesmo sendo texto “simples”, o serviço de ML deve:

* validar se `text` é string;
* limitar tamanho máximo (para não sobrecarregar o modelo);
* rejeitar json inválido com erro 400.

### 7.3. Carregamento do modelo

* Carregar o modelo `.pkl` de forma segura:

    * arquivo deve vir de fonte confiável (repositório interno, storage confiável).
* Não aceitar upload de `.pkl` de fora para dentro da aplicação.

---

## 8. Segurança do Banco de Dados

### 8.1. Usuários e permissões

Em ambiente real:

* Criar um usuário de banco **somente para a aplicação**:

    * sem permissão de `DROP DATABASE` ou `CREATE ROLE`, etc.
* Permissões mínimas:

    * `SELECT`, `INSERT`, `UPDATE`, `DELETE` nas tabelas que precisa.

No ambiente de desenvolvimento:

* Pode ser mais aberto, mas já pensando no modelo de produção.

### 8.2. Riscos de SQL Injection

* Usar sempre JPA / Spring Data ou queries parametrizadas.
* Nunca concatenar string com parâmetros do usuário na query.

### 8.3. Consistência e integridade

As constraints do `schema-postgres.sql` ajudam a:

* impedir dados quebrados;
* evitar estados inesperados.

As `CHECK` constraints (nota, sentimentos, status, canal, split, etc.) também servem como **camada extra de proteção** contra bugs da aplicação.

### 8.4. Backup e recuperação (ideia futura)

Não implementado no MVP, mas:

* Registrar na documentação:

    * qual banco é usado;
    * qual é o nome do schema;
    * como seria feito um dump (`pg_dump`) e um restore em produção.

---

## 9. Segurança do Frontend

### 9.1. CORS

* Em desenvolvimento:

    * backend pode permitir `http://localhost:*` para testes.
* Em produção:

    * configurar CORS para apenas o domínio do frontend oficial.

### 9.2. XSS (Cross-Site Scripting)

Mesmo com UI simples, tomar cuidado com:

* Exibição de textos vindos do backend (comentários, nomes, tags de produto).
* Evitar inserir texto diretamente como HTML (`innerHTML`) sem sanitização.
* Preferir APIs de DOM seguras (`textContent` em vez de `innerHTML`, por exemplo) quando estiver mostrando dados que vêm do servidor.

### 9.3. Armazenamento local

Se usar `localStorage` para guardar `clienteAtual` ou `tipoCliente`:

* Não armazenar dados sensíveis (senhas, tokens).
* Guardar apenas o mínimo (ID, tipo, nome, talvez e-mail).
* Em uma versão futura com autenticação real, o ideal é mover isso para cookies seguros ou tokens com expiração.

---

## 10. Segurança em ambiente / configuração

### 10.1. Profiles do Spring (`application-*.yml`)

Arquivos:

* `application.yml` – configurações comuns.
* `application-dev.yml` – desenvolvimento (banco local, logs mais detalhados).
* `application-oci.yml` – futuro deploy na OCI (banco na nuvem, logs reduzidos, HTTPS, etc.).

Boas práticas:

* Nunca colocar senha diretamente nesses arquivos em produção:

    * usar variáveis de ambiente (`SPRING_DATASOURCE_USERNAME`, etc.).
* Configurar:

    * logging:

        * dev: `DEBUG`/`INFO`;
        * prod: mais para `INFO`/`WARN`/`ERROR`.

### 10.2. HTTPS (produção)

* Em ambiente real, todo acesso ao backend deve ser feito via **HTTPS**.
* TLS pode ser:

    * direto no Spring Boot (com certificado configurado), ou
    * através de um proxy reverso (Nginx, gateway, load balancer).

---

## 11. Privacidade e ética (ML + feedback do cliente)

### 11.1. Quem vê o quê

Regras de negócio importantes que também são decisões de segurança/privacidade:

* **Comprador**:

    * envia comentários;
    * **não vê** o rótulo de sentimento nem a probabilidade.
* **Vendedor / Empresa**:

    * vê agregados, comentários, críticas e notificações.

Isso reduz o risco de:

* usuário tentar “enganar” o modelo conscientemente;
* conflitos sobre como o sistema interpretou a opinião dele.

### 11.2. Dados de treino

* Dataset vem de fontes públicas (por exemplo, Kaggle).
* Usar apenas campos necessários (texto + rótulo).
* Não expor `dataset_registro` em nenhuma API pública.

---

## 12. Ameaças principais e mitigação (resumo)

| Ameaça                                      | Impacto                                      | Mitigação atual / planejada                            |
| ------------------------------------------- | -------------------------------------------- | ------------------------------------------------------ |
| Segredos expostos no git                    | Vazamento de acesso a banco / serviços       | `.gitignore`, uso de env vars / GitHub Secrets         |
| SQL Injection                               | Alteração/leitura indevida de dados          | JPA, queries parametrizadas, sem concatenação          |
| Entrada inválida ou maliciosa nos endpoints | Erros, travamentos, inconsistência           | Validação de entrada (Java/Python), constraints DB     |
| ML endpoint abusado externamente            | Carga alta, custos, exploração do modelo     | Tratar `/predict` como serviço interno                 |
| Logs com dados demais                       | Exposição acidental de dados sensíveis       | Limitar payloads, focar em IDs, evitar textos inteiros |
| CORS aberto em produção                     | Risco de chamadas da API por sites terceiros | Restringir CORS ao domínio oficial                     |
| Exposição acidental de dados de treino      | Questões legais/privacidade                  | Não expor dataset_registro em APIs públicas            |

---

## 13. Fora de escopo (MVP) e próximos passos

Não estão implementados no MVP, mas são possíveis evoluções futuras:

* Autenticação/autorização real:

    * login com senha, JWT, perfis distintos (comprador/vendedor/admin).
* Rate limiting:

    * limitar número de requisições por minuto em endpoints sensíveis.
* Observabilidade mais avançada:

    * métricas, traces, dashboards de monitoramento.
* Criptografia em repouso:

    * criptografar volume de banco,
    * campos específicos sensíveis (se forem adicionados).
* Testes de segurança automatizados:

    * varreduras de dependências vulneráveis,
    * scanners simples de segurança em pipeline.

---

## 14. Conclusão

Este documento define um conjunto de **boas práticas de segurança** adaptado à realidade do **Hackathon One Sentiment API**:

* protege o básico (**dados, API, ML, banco, repositório**);
* não foge do escopo de um MVP, mas já prepara o terreno para uma versão mais robusta;
* organiza decisões para qualquer pessoa que entrar no projeto entender **o que é aceitável** e **o que é proibido** do ponto de vista de segurança.

Ele deve ser lido em conjunto com:

* `docs/requisitos.md`
* `docs/arquitetura.md`
* `docs/database.md`
* `docs/test-strategy.md`
* diagramas em `docs/uml/`

e atualizado sempre que novas funcionalidades forem adicionadas, principalmente se envolverem:

* autenticação,
* novos tipos de dado,
* integração com serviços externos.
