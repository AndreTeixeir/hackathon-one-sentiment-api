# ADR-005 – Domínio e HTTPS via Caddy de outra VM (oci-rag)

**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 29/09/2026
**Status:** Aprovado  
**Escopo:** Publicação de um domínio próprio com HTTPS para a API, sem alterar a VM de produção do projeto

---

## 1. Contexto

A API rodava apenas em `http://152.67.61.11:8080` — IP direto, sem TLS. Isso bloqueia recursos que exigem contexto seguro no navegador (o "Try it out" do Swagger UI, por exemplo, sofre de mixed content ao rodar numa página HTTPS contra um backend HTTP) e não é uma URL apresentável para portfólio.

Restrições do ambiente:

- O domínio `andreteixeira.dev.br` já existe no Registro.br, com DNS gerenciado lá mesmo.
- A VM de produção deste projeto (`sentiment-api-server`, `VM.Standard.E2.1.Micro`) tem 1 GB de RAM e já opera com swap — pouca folga para rodar um proxy TLS (Caddy/Nginx) e gerenciar certificados localmente.
- O limite de VMs gratuitas da conta OCI já está esgotado — não é possível provisionar uma VM nova só para isso.
- Existe outra VM na mesma conta, `rag-multiempresa` ("oci-rag", do projeto SabIA), Ubuntu 24.04 ARM64, 2 OCPU / 12 GB, com folga de memória (~8% em uso) e já rodando Caddy como serviço systemd para outros subdomínios.

## 2. Problema

Como publicar a API sob HTTPS e um domínio próprio sem:
1. sobrecarregar a VM de 1 GB do projeto;
2. consumir mais uma VM gratuita (indisponível);
3. depender de um serviço terceiro desnecessário para algo que o DNS atual já resolve.

## 3. Decisão

**Um subdomínio por projeto, usando o Caddy da `oci-rag` como porta de entrada HTTPS de vários projetos da conta.**

- Registro A no Registro.br: `sentiment.andreteixeira.dev.br` → `137.131.249.111` (IP público da `oci-rag`).
- Bloco adicionado ao `Caddyfile` da `oci-rag` (`/etc/caddy/Caddyfile`, fora deste repositório):

  ```
  sentiment.andreteixeira.dev.br {
      reverse_proxy 152.67.61.11:8080
  }
  ```

- Certificado emitido automaticamente pelo Caddy via Let's Encrypt, desafio **TLS-ALPN-01** (não exige abrir a porta 80 na VM do sentiment, nem qualquer mudança nela), com renovação automática.
- A VM do sentiment não teve nenhuma mudança de infraestrutura: o Caddy da `oci-rag` termina o TLS e repassa a requisição em HTTP puro, pela rede pública, para `152.67.61.11:8080`.
- O acesso direto `http://152.67.61.11:8080` foi mantido ativo por decisão do André (ver §6).
- Ajuste necessário no backend (Spring Boot), registrado no compose e não no `application-prod.yml` para não exigir rebuild de imagem na VM de 1 GB: `SERVER_FORWARDHEADERSSTRATEGY=framework`, para que o springdoc gere `servers[0].url` a partir dos cabeçalhos `X-Forwarded-Proto`/`X-Forwarded-Host` enviados pelo Caddy, em vez do protocolo/host da conexão interna (HTTP, IP direto).

Testado: `https://sentiment.andreteixeira.dev.br/api/v1/health` responde 200 com `mode: model`; página estática e análise de sentimento funcionando através do domínio.

## 4. Alternativas consideradas

### 4.1. HTTPS na própria VM do sentiment (proxy no `docker-compose.yml`)

**Descrição:** adicionar um serviço Caddy ou Nginx ao compose do próprio projeto, rodando na VM de 1 GB.

**Rejeitada:** a VM já opera no limite de memória (usa swap). Rodar mais um processo (proxy + renovação de certificado) competiria por RAM com o backend, o `ds-service` e o Postgres. Também exigiria abrir a porta 80 nessa VM para o desafio HTTP-01 do Let's Encrypt (ou TLS-ALPN-01 na própria 8080/443), mudança de infraestrutura maior do que reaproveitar um proxy já existente.

### 4.2. Cloudflare (proxy/DNS/certificado)

**Descrição:** apontar o domínio para o Cloudflare e usar o certificado e o proxy reverso dele.

**Rejeitada:** desnecessária. O DNS do Registro.br já resolve o domínio sem custo ou camada extra; o Cloudflare acrescentaria uma dependência de terceiro (e um proxy adicional na frente do já existente na `oci-rag`) sem resolver nenhum problema que o Caddy já não resolvesse.

### 4.3. Plataformas gerenciadas (Render e similares) para projetos futuros

**Descrição:** hospedar o backend inteiro numa plataforma com HTTPS embutido, em vez de OCI + Caddy.

**Avaliada para uso futuro, não para este projeto agora:** no plano gratuito do Render, o serviço desliga após 15 minutos sem acesso e leva cerca de 1 minuto para voltar (cold start), e o Postgres gratuito expira em 30 dias (https://render.com/docs/free). Para uma demo de portfólio que precisa responder a qualquer momento, isso é pior do que a VM sempre ativa da OCI. Fica registrado como opção a avaliar para projetos novos, não como substituição do que já está no ar.

### 4.4. Domínio escolhido

**Decisão escolhida (subdomínio + Caddy da `oci-rag`):** zero custo adicional, zero VM nova, zero mudança na VM de produção do projeto além de uma variável de ambiente, reaproveitando um proxy e uma automação de certificado que já existiam e já funcionam para outros subdomínios da mesma conta.

## 5. Impactos da decisão

### 5.1. Fora deste repositório

DNS no Registro.br (registro A) e `Caddyfile` da `oci-rag` — nenhum dos dois é versionado neste projeto; este ADR é o registro da configuração.

### 5.2. Neste repositório

- `docker-compose.yml`: variável `SERVER_FORWARDHEADERSSTRATEGY: framework` no serviço `backend`.
- `README.md`: URLs de demonstração trocadas do IP para o domínio.
- `CLAUDE.md`: domínio registrado como endereço de produção, com nota de que o IP direto continua respondendo.

## 6. Consequências (curto e longo prazo)

### 6.1. Acoplamento a outra VM

Se a `oci-rag` cair ou seu Caddy parar, o domínio para de responder — mesmo com a VM do sentiment saudável. O IP direto (`http://152.67.61.11:8080`) segue como caminho alternativo, por decisão explícita do André de mantê-lo público.

### 6.2. Tráfego em HTTP na rede pública, entre proxy e backend

O trecho `Caddy (oci-rag) → 152.67.61.11:8080` trafega em HTTP puro por rede pública (não é loopback nem rede privada da OCI) — o TLS existe apenas entre o cliente final e o Caddy.

### 6.3. Superfície de forjar `X-Forwarded-*`

Com `forward-headers-strategy: framework`, o Spring confia nos cabeçalhos `X-Forwarded-Proto`/`X-Forwarded-Host` de qualquer origem — e como a porta 8080 continua aberta ao público (§6.1), um cliente pode acessá-la direto e forjar esses cabeçalhos. O efeito prático hoje é limitado: eles só alimentam URLs geradas (como `servers[0].url` do springdoc); nenhuma autenticação, autorização ou redirecionamento do backend depende deles.

### 6.4. Endurecimento possível, adiado

Restringir a porta 8080 para aceitar apenas o IP da `oci-rag` eliminaria a superfície de forja do item anterior. Adiado por decisão do André de manter o IP direto acessível publicamente (§3).

## 7. Quando revisitar esta decisão

- Se o IP direto (`152.67.61.11:8080`) deixar de precisar ficar público, para então restringi-lo ao IP da `oci-rag` (§6.4).
- Se a `oci-rag` for desativada, migrada ou tiver seu Caddy reconfigurado — o domínio deste projeto depende diretamente dela.
- Se o volume de tráfego ou a natureza dos dados exigir criptografia também no trecho Caddy→backend (§6.2), hoje em HTTP puro.
- Se um projeto novo justificar reavaliar uma plataforma gerenciada (§4.3) em vez de OCI + Caddy.
