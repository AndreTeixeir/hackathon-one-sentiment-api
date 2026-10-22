# Threat Model · Hackathon One Sentiment API

Este documento descreve, de forma simples e direta, o modelo de ameaças do sistema.  
Não é um documento de segurança corporativa, mas mostra que as principais preocupações foram pensadas.

---

## 1. Ativos a proteger

Os principais “bens” do sistema são:

1. **Comentários dos clientes**
    - Texto enviado pelos compradores.
    - Pode conter opiniões fortes, dados sensíveis por engano, etc.

2. **Dados do vendedor**
    - Produtos cadastrados.
    - Estatísticas de sentimentos.

3. **Infraestrutura de execução**
    - Backend (Spring Boot).
    - Microserviço ML (FastAPI).
    - Banco PostgreSQL.

4. **Código-fonte e repositório Git**
    - Scripts de deploy.
    - Configurações de conexão (que não devem conter segredos).

---

## 2. Visão geral dos fluxos de dados

Fluxo principal:

1. Comprador → Frontend → Backend via HTTP.
2. Backend → ML Service via HTTP interno.
3. Backend → Banco (leituras/escritas).
4. Vendedor → Frontend → Backend via HTTP.
5. Backend → Frontend → JSON com estatísticas, comentários, notificações.

Referência detalhada:

- Ver `docs/arquitetura.md` e os diagramas de sequência/fluxo.

---

## 3. Possíveis ameaças (visão simplificada)

### 3.1. Inputs maliciosos (comentários com conteúdo inesperado)

- **Risco:** comentários contendo scripts, HTML malicioso, SQL embutido, etc.
- **Impacto potencial:**
    - XSS se o texto for exibido sem escapar no frontend;
    - logs “poluídos”;
    - confusão na interpretação dos dados.

**Mitigações:**

- Tratar os comentários sempre como texto simples no frontend (escapar HTML).
- Evitar executar ou interpretar o conteúdo do comentário em qualquer lugar.
- Validar tamanho máximo dos comentários.

---

### 3.2. Acesso indevido a dados (exposição de informações além do necessário)

- **Risco:** um cliente ou vendedor acessar dados que não são do seu contexto.
- **Exemplo:** endpoint `/stats` sendo chamado com `vendedorId` de outro vendedor.

**Mitigações (nível MVP):**

- Separar claramente o que é retornado para:
    - comprador;
    - vendedor (dados agregados, não dados brutos de outros vendedores).
- Não expor o sentimento analisado para o comprador.
- Se futuramente houver autenticação, amarrar `vendedorId` à identidade do usuário logado.

---

### 3.3. Vazamento de segredos em configuração

- **Risco:** senhas de banco, tokens ou chaves aparecerem no repositório Git.
- **Impacto:** qualquer pessoa com acesso ao repo pode usar essas credenciais.

**Mitigações:**

- `.gitignore` incluindo:
    - `.env`
    - `application-*.properties` sensíveis (produção, por exemplo)
- Uso de variáveis de ambiente ou arquivos locais não versionados.

---

### 3.4. Depredação ou queda de serviços

- **Risco:** ML Service fora do ar, banco indisponível, backend em looping, etc.
- **Impacto:** sistema fica indisponível ou se comporta de forma errática.

**Mitigações:**

- Tratar erros de chamada ao ML Service (timeout, falha de conexão).
- Retornar mensagens de erro claras para o cliente (sem stacktrace).
- Logar erros em `log_evento` com nível `ERROR`.
- Documentar, no `docs/runbook.md`, o que fazer quando algum serviço cair.

---

### 3.5. Exposição de dados em logs

- **Risco:** textos sensíveis em logs; logs enviados para ambientes que outras pessoas têm acesso.
- **Mitigações:**
    - Evitar logar o texto completo do comentário em logs de produção;
    - Logar apenas IDs e metadados (ex.: `comentario_id`, `cliente_id`, `resultado_id`).

---

## 4. Controle por camada

### 4.1. Frontend

- Validar inputs básicos (tamanho mínimo).
- Escapar o texto do comentário ao exibir na tela, evitando XSS.
- Não armazenar dados sensíveis em `localStorage` (somente IDs genéricos e tipo do cliente para navegação).

### 4.2. Backend

- Usar validação de DTOs (Bean Validation).
- Aplicar regras de negócio para:
    - comprador não ver sentimento;
    - vendedor só ver dados relacionados a seus produtos.
- Centralizar tratamento de exceções, evitando vazamento de stacktrace bruto.
- Gravar logs com `nivel` e `origem` claros.

### 4.3. Microserviço ML

- Ser acessível apenas pela API backend (em produção, idealmente numa rede interna).
- Validar o payload de entrada (`text` não nulo, tamanho máximo razoável).
- Proteger o serviço de loops ou dependências externas.

### 4.4. Banco de dados

- Usar usuário com permissões mínimas necessárias.
- Não expor o banco diretamente para a internet.
- Aplicar constraints (FK, CHECK) para manter integridade de dados.

---

## 5. Threat Model vs. Requisitos de segurança

Este modelo de ameaças complementa o que está descrito em:

- `docs/security.md` – visão geral de segurança e boas práticas.
- `docs/devops-deploy.md` – como subir o sistema sem expor credenciais.

A ideia é que, quando o projeto evoluir para algo mais próximo de produção, esta base sirva para:

- incluir autenticação real;
- definir regras de autorização;
- adicionar criptografia em repouso/transporte;
- configurar monitoração e alertas.

---

## 6. Atualização

Este documento deve ser revisado quando:

- novos componentes forem adicionados;
- o projeto passar a lidar com dados sensíveis reais;
- forem introduzidos mecanismos de login / autorização.
