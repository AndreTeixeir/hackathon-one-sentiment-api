# Project Charter · Hackathon One Sentiment API

## 1. Informações gerais

- **Nome do projeto:** Hackathon One Sentiment API
- **Versão do documento:** 1.0 (rascunho inicial)
- **Data:** _(preencher)_
- **Sponsor / Responsável de negócio:** _(preencher, ex.: time de atendimento/marketing)_
- **Responsável técnico:** _(preencher, ex.: líder de backend ou DS)_

---

## 2. Justificativa

Empresas de e-commerce recebem diariamente grandes volumes de comentários de clientes. Ler tudo manualmente é inviável, especialmente para equipes enxutas.

Este projeto visa demonstrar, em formato de MVP:

- a viabilidade técnica de analisar automaticamente o sentimento de comentários em português;
- a integração entre equipes de Back-end, Data Science, Frontend e DevOps;
- uma arquitetura modular, escalável e bem documentada, digna de apresentação para empresas.

---

## 3. Objetivos do projeto

### 3.1. Objetivo de negócio

Permitir que um vendedor de e-commerce:

- acompanhe rapidamente o “clima” das avaliações dos seus produtos;
- identifique comentários negativos críticos;
- tenha acesso a um painel com estatísticas e possibilidade de exportar os dados.

### 3.2. Objetivo técnico

Construir um sistema integrado com:

- API Java (Spring Boot);
- microserviço de ML em Python (FastAPI);
- base PostgreSQL;
- frontend Web simples;
- documentação completa em `docs/`.

---

## 4. Escopo de alto nível

### 4.1. Escopo incluído

- Treinamento de um modelo de ML simples (TF-IDF + Regressão Logística).
- Exposição de endpoint `/predict` no microserviço de ML.
- Criação de API REST em Java para:
    - receber comentários;
    - chamar o ML;
    - registrar resultados em banco;
    - gerar estatísticas;
    - cuidar de notificações e exportação.
- Frontend com três telas:
    - login (escolha de perfil);
    - comprador (lista de produtos e envio de comentários);
    - vendedor (dashboard e notificações).
- Documentação em markdown:

    - requisitos;
    - arquitetura;
    - banco de dados;
    - frontend;
    - testes;
    - segurança;
    - ADRs.

### 4.2. Escopo excluído

- Login com autenticação real (senha, JWT, roles).
- Garantias de alta disponibilidade e escalabilidade extrema.
- Pipeline de CI/CD completo com deploy automático em cloud (pode ser esboçado, não necessariamente implementado).
- Ferramentas avançadas de monitoramento (Prometheus, Grafana etc.).

---

## 5. Stakeholders e papéis

> Detalhamento completo em `docs/stakeholders.md`. Abaixo, resumo.

- **Cliente - Comprador**
    - Papel: envia comentários sobre produtos.
- **Cliente - Vendedor**
    - Papel: acompanha feedback e decide ações a partir das análises.
- **Equipe de Data Science**
    - Papel: coleta e prepara dados, treina o modelo, publica o `.pkl` e o microserviço.
- **Equipe de Backend**
    - Papel: implementa API, integra com ML, persiste dados, aplica regras de negócio.
- **Equipe de Frontend**
    - Papel: cria as telas para comprador e vendedor, consumindo a API.
- **Equipe de DevOps/Infra**
    - Papel: automatiza execução, prepara scripts de deploy, cuida de variáveis sensíveis.
- **Equipe de QA**
    - Papel: planeja e executa testes; valida requisitos.

---

## 6. Entregáveis principais

- **Código-fonte:**
    - backend em `/backend`;
    - microserviço ML em `/datascience/ml_service`;
    - frontend em `/frontend/web`.

- **Banco de dados:**
    - `ddl/schema-postgres.sql` com todas as tabelas.

- **Documentação:**
    - `docs/requisitos.md`
    - `docs/arquitetura.md`
    - `docs/database.md`
    - `docs/frontend.md`
    - `docs/test-strategy.md`
    - `docs/test-report.md`
    - `docs/security.md`
    - `docs/devops-deploy.md`
    - `docs/ml-model-card.md`
    - `docs/dataset.md`
    - `docs/traceability-matrix.md`
    - `docs/coding-standards.md`
    - `docs/roadmap.md`
    - `docs/threat-model.md`
    - `docs/runbook.md`
    - `docs/stakeholders.md`
    - ADRs em `docs/adr/`

---

## 7. Premissas e restrições

### 7.1. Premissas

- Participantes têm acesso a:
    - ambiente com Java 17, Python 3, Postgres;
    - ferramentas básicas de desenvolvimento (IDE, Git, etc.).
- Dataset em português está disponível publicamente e pode ser usado no contexto educacional.
- A equipe tem tempo limitado (formato hackathon), logo:
    - foco em MVP bem recortado;
    - profundidade moderada em testes automatizados.

### 7.2. Restrições

- Tecnologias principais já definidas (Java 17, Spring, Python, FastAPI, Postgres).
- Prazo de entrega compatível com formato hackathon (curto).
- Sem orçamento para infraestrutura complexa (cloud paga, grandes clusters, etc.).

---

## 8. Riscos principais (alto nível)

- **Modelo de ML com desempenho abaixo do esperado**
    - Mitigação: usar modelo simples e estável (TF-IDF + LogReg), com métricas claras.

- **Falta de tempo para testes completos**
    - Mitigação: priorizar testes básicos em fluxos críticos e registrar pendências em `docs/test-report.md`.

- **Complexidade excessiva da solução**
    - Mitigação: manter continuamente o foco na visão de MVP (ver `docs/project-vision.md`).

---

## 9. Aprovação (conceito)

> Em um ambiente real, esta seção receberia nomes/assinaturas.  
> Aqui, ela serve de lembrete de que a visão e o escopo foram combinados.

- **Responsável de negócio:**  
  Nome: ___________________  Data: ___/___/____

- **Responsável técnico:**  
  Nome: ___________________  Data: ___/___/____
