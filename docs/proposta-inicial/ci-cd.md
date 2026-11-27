# CI/CD · Hackathon One Sentiment API

Este documento descreve como o projeto **pode** ser integrado a pipelines de integração contínua (CI) e entrega contínua (CD).  
Mesmo que a implantação completa não seja feita no hackathon, o desenho já deixa o projeto pronto para isso.

---

## 1. Objetivos da CI/CD

- Garantir que:
    - o código compila;
    - testes básicos rodam;
    - padrões mínimos de qualidade são respeitados.
- Automatizar ao máximo:
    - build do backend;
    - testes do ML service;
    - verificação de formatação básica.
- Preparar terreno para:
    - empacotar serviços em contêiner;
    - deploy em ambiente único (por exemplo, na cloud).

---

## 2. Fluxo geral (conceito)

### 2.1. Integração contínua (CI)

Disparada em:

- pushes na branch principal;
- aberturas e atualizações de Pull Requests.

Etapas típicas:

1. **Checkout do código**
2. **Build do backend (Java)**
3. **Teste do backend (JUnit)**
4. **Validação do ML service (import, testes básicos)**
5. **Lint/checagens simples (opcional)**

### 2.2. Entrega contínua (CD)

- Em um cenário de hackathon, pode ser descrita, não necessariamente implementada.
- Em ambiente real, poderia:
    - construir imagens Docker do backend e do ML service;
    - fazer deploy em ambiente de teste/homolog;
    - atualizar um cluster ou VM.

---

## 3. Sugestão de pipeline (GitHub Actions)

Arquivo possível: `.github/workflows/ci.yml`

> Obs.: aqui é só um esqueleto conceitual; os detalhes podem ser ajustados conforme o projeto for evoluindo.

Passos principais:

1. Job `build-backend`:
    - roda em `ubuntu-latest`;
    - usa Java 17;
    - executa `mvn -f backend/pom.xml test`.

2. Job `test-ml-service`:
    - roda em `ubuntu-latest`;
    - usa Python 3.x;
    - instala dependências em `datascience/ml_service`;
    - executa testes simples (por exemplo, importar o app e talvez rodar um teste de função isolada).

3. (Opcional) Job `lint-frontend`:
    - valida se JS está sem erros de sintaxe (simples).

---

## 4. Variáveis de ambiente e segredos

Mesmo em ambiente simples, é importante:

- não hardcodear:
    - usuário / senha do banco;
    - tokens de serviços externos (se algum dia forem usados).
- usar:
    - `secrets` do GitHub para valores sensíveis;
    - variáveis de ambiente no pipeline para passar dados ao backend ou containers.

Exemplos de secrets:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `ML_SERVICE_URL` (URL para o `/predict` em ambiente de teste/homolog)

---

## 5. Critérios mínimos para CI “verde”

Para considerar a pipeline **aprovada**:

1. Build do backend deve:
    - compilar sem erros;
    - rodar testes JUnit básicos;
2. ML service:
    - deve ter pelo menos um teste de import e, se possivel, teste de função de inferência isolada (sem precisar subir servidor).
3. Caso algum desses passos falhe:
    - PR deve ser revisado antes de merge;
    - problema deve ser corrigido ou marcado explicitamente como pendência (se for hackathon, com anotação no `docs/test-report.md`).

---

## 6. Evolução futura (CD completa)

Quando o projeto sair do contexto de hackathon, o pipeline pode evoluir para:

- construir imagens Docker para:
    - backend;
    - ml_service;
- publicar imagens em registry privado;
- usar `docker-compose` ou orquestrador (Kubernetes, por exemplo) em ambiente de teste/homolog.

Etapas possíveis de CD:

1. **Stage de teste em contêiner**
    - subir Postgres em container;
    - subir backend e ML;
    - rodar smoke tests automáticos.

2. **Stage de homolog**
    - deploy automático a partir de tags de release;
    - rodar smoke tests em ambiente quase idêntico à produção.

3. **Stage de produção**
    - no contexto desta aplicação, poderia ser:
        - uma VM na nuvem rodando os serviços;
        - ou uma instância simples com docker-compose.

---

## 7. Relação com outros documentos

- `docs/devops-deploy.md`  
  → mostra como rodar localmente / com Docker (quando implementado).

- `docs/test-strategy.md` e `docs/test-report.md`  
  → definem quais testes existem e quais devem ser incluídos na pipeline.

- `docs/security.md`  
  → reforça a importância de não vazar segredos no repositório ou nos logs da pipeline.

---

Este documento não precisa refletir uma pipeline 100% implementada, mas dar uma visão clara de **como o projeto foi pensado** para crescer em direção a CI/CD profissional.
