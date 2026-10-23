# ADR-006 – CORS restrito, remoção do nginx antigo e portas expostas

**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 22/10/2026
**Status:** Aprovado  
**Escopo:** Superfície de exposição da aplicação: origens aceitas pelo navegador (CORS), serviço nginx do `docker-compose.yml` e portas publicadas no host

---

## 1. Contexto

Uma revisão da superfície de exposição do sistema encontrou três pontos:

- **CORS aberto, com credenciais.** O `CorsConfig` aceitava qualquer origem (`setAllowedOriginPatterns("*")`) com `setAllowCredentials(true)`. A API não tem autenticação, mas a configuração era a mais permissiva possível: qualquer site podia chamar a API a partir do navegador de um visitante.
- **Nginx sobrando do frontend antigo.** O serviço `frontend` do `docker-compose.yml` (imagem `nginx:alpine`, porta 80) entrou em 11/01/2026 para servir `./frontend/web`. Essa pasta foi apagada em 13/01/2026 ("Removendo versoes antigas de backend e frontend"), mas o serviço ficou. O `nginx/nginx.conf` repassava `/api/` ao backend com `Access-Control-Allow-Origin: *`, e o `scripts/deploy-oci.sh` ainda checava `http://localhost:80` e imprimia endereços pela porta 80. O frontend real é servido pelo próprio Spring Boot (`static/index.html`), na mesma origem da API.
- **Postgres e `ds-service` publicados em todas as interfaces.** O compose publicava a 5432 e a 8000 sem restringir o endereço do host, e a senha do banco está escrita no próprio `docker-compose.yml`, que é público. O guia de deploy (`docs/deploy-oci.md`) ainda orientava abrir a 8000 para `0.0.0.0/0` na security list.

## 2. Problema

Como reduzir a superfície exposta sem derrubar o que está no ar:

1. quem pode chamar a API a partir de um navegador;
2. o que escuta na VM sem ter função (nginx, porta 80);
3. o que está acessível de fora sem necessidade (Postgres e `ds-service`).

Restrições: a porta 8080 precisa continuar aberta, porque o Caddy da `oci-rag` chega por ela (ADR-005) e o acesso direto pelo IP foi mantido; não há uso legítimo de acesso externo ao Postgres e ao `ds-service`; o link publicado no README não pode cair.

## 3. Decisão

### 3.1. CORS restrito, por configuração

- A lista de origens vem da propriedade `app.cors.allowed-origins` do `application.yml`, com o valor `https://sentiment.andreteixeira.dev.br,http://localhost:8080`.
- Origens exatas (`setAllowedOrigins`, não padrões), sem credenciais (`setAllowCredentials(false)`), só os métodos que os controllers usam (`GET` e `POST`) e só o cabeçalho `Content-Type`, o único que o frontend envia.
- Origem fora da lista recebe 403, tanto no preflight quanto na requisição. Requisição sem `Origin` (curl) e requisição de mesma origem (como o acesso direto `http://152.67.61.11:8080`) não passam pelo CORS e seguem funcionando: o Spring só trata como CORS a requisição cujo `Origin` difere do esquema, host e porta da própria requisição. Os dois casos têm teste.

### 3.2. Remoção do nginx

O serviço `frontend` e o arquivo `nginx/nginx.conf` saem do repositório, e a pasta `nginx/` deixa de existir. O frontend continua sendo servido pelo Spring Boot. A porta 80 deixa de ser usada por este projeto. O `scripts/deploy-oci.sh` perde a checagem da porta 80 e passa a imprimir os endereços reais (porta 8080 e o domínio).

### 3.3. Portas do Postgres e do `ds-service` só no loopback

`"127.0.0.1:5432:5432"` e `"127.0.0.1:8000:8000"`. O backend continua falando com os dois pela rede interna do Docker (`postgres:5432` e `ds-service:8000`), e o acesso por `localhost` na própria máquina (notebooks, `curl`, a checagem do `deploy-oci.sh`) continua igual. A 8080 não muda.

## 4. Alternativas consideradas

### 4.1. Manter `*` no CORS, apenas sem credenciais

**Não escolhida:** tira o risco das credenciais, mas qualquer site continuaria podendo chamar a API pelo navegador de um visitante (consumindo o `ds-service` e gravando no banco). A lista restrita reduz isso à demo e ao ambiente local.

### 4.2. Corrigir o nginx em vez de removê-lo

**Não escolhida:** o serviço serviria uma pasta que não existe e duplicaria o frontend que o Spring já serve na mesma origem da API, mantendo um segundo ponto de entrada na porta 80, fora do controle da aplicação, com CORS próprio.

### 4.3. Depender só do firewall e da security list da OCI

**Não escolhida:** o compose publicava as portas em todas as interfaces, e o guia de deploy documentava a 8000 aberta. Publicar só no loopback não depende de nenhuma regra de rede estar correta.

### 4.4. Retirar o `ports` do Postgres e do `ds-service`

**Não escolhida:** o acesso por `localhost` na própria máquina (notebooks, `curl`, health check do `deploy-oci.sh`) deixaria de funcionar.

### 4.5. Restringir a 8080 ao IP da `oci-rag`

**Continua adiada:** já registrada no ADR-005 (§6.4). Esta decisão não altera a 8080.

### 4.6. Opção escolhida

CORS por configuração e restrito; nginx removido; 5432 e 8000 só em `127.0.0.1`; 8080 inalterada. É a menor mudança que fecha os três pontos sem tocar no caminho que o domínio usa.

## 5. Impactos da decisão

### 5.1. Neste repositório

- `CorsConfig` e `application.yml` (`app.cors.allowed-origins`), com testes em `CorsConfigTest`.
- `docker-compose.yml`: serviço `frontend` removido e portas 5432 e 8000 no loopback.
- `nginx/nginx.conf` removido.
- `scripts/deploy-oci.sh` e `docs/deploy-oci.md`: sem a porta 80, sem a 8000 aberta, endereços reais.

### 5.2. Fora deste repositório

- **VM de produção:** se o container `sentiment-frontend` existir lá, ele não faz mais parte do compose e precisa ser removido no deploy (§6.2).
- **Security list e firewall da OCI:** regras de entrada para as portas 80 e 8000 da VM do sentiment, se existirem, deixam de ser necessárias. Não foi verificado se existem.
- **Caddy da `oci-rag`:** nenhuma mudança.

## 6. Consequências (curto e longo prazo)

### 6.1. Adicionar uma origem é mudança de configuração e deploy

Um novo domínio, ou um frontend servido em outra porta local (por exemplo `http://localhost:5500`), só passa a ser aceito depois de entrar em `app.cors.allowed-origins` e de um novo deploy. Hoje, esse frontend em outra porta seria recusado.

### 6.2. O container nginx que já exista na VM precisa ser removido no deploy

`docker compose down` sem `--remove-orphans` não remove containers de serviços que saíram do arquivo: o container continua rodando e a rede do projeto não é removida ("Resource is still in use"), e o comando ainda termina com código 0. Foi verificado localmente com Docker Compose v5.6.0, num projeto descartável. Para o `docker-compose` instalado na VM, o comportamento não foi verificado.

### 6.3. Acessar o banco de fora da VM passa a exigir túnel SSH

O Postgres e o `ds-service` deixam de responder no IP público da VM. Para consultar o banco de fora, é preciso um túnel SSH (por exemplo, `ssh -L 5432:127.0.0.1:5432 usuario@IP_DA_VM`).

### 6.4. A 8080 continua pública

O que o ADR-005 registra sobre a 8080 (§6.1, §6.3 e §6.4) continua valendo.

### 6.5. A senha do banco continua no compose

A mudança reduz o alcance (só o loopback chega ao Postgres), mas não tira a credencial do `docker-compose.yml`, que é público. Removê-la do arquivo é trabalho em aberto.

## 7. Quando revisitar esta decisão

- Se um novo domínio ou um frontend em outra origem precisar chamar a API (§6.1).
- Se a API passar a ter autenticação por cookie ou sessão: revisar `allowCredentials` e a lista de origens.
- Se a 8080 passar a aceitar apenas o IP da `oci-rag` (ADR-005, §6.4).
- Se a senha do banco for tirada do `docker-compose.yml` (§6.5).
