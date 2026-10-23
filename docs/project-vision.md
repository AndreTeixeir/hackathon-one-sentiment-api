# Visão do Produto · Hackathon One Sentiment API

## 1. Contexto

Empresas que atuam com vendas online recebem diariamente dezenas ou centenas de comentários e avaliações em seus produtos. Ler tudo manualmente é caro, demorado e pouco prático — especialmente quando a equipe é pequena.

Ao mesmo tempo, deixar de ouvir o cliente significa:

- perder oportunidades de melhorar o produto;
- demorar para responder reclamações sérias;
- ter a reputação impactada sem perceber.

O **Hackathon One Sentiment API** nasce exatamente nesse cenário: um protótipo funcional de análise de sentimentos focado em comentários de clientes, com objetivo de mostrar na prática como Back-end, Data Science e Frontend podem trabalhar juntos.

---

## 2. Problema de negócio

**Problema principal:**  
Empresas não conseguem acompanhar de forma eficiente o “clima” das avaliações dos clientes (positivas/negativas), nem priorizar rapidamente quais comentários exigem atenção urgente.

Desdobramentos:

- feedback negativo “passa batido”;
- vendedor responde tarde ou nem responde;
- não existe visão consolidada de como está a satisfação por produto;
- não há um jeito simples de exportar os dados para análise posterior.

---

## 3. Público-alvo

- **Pequenas e médias empresas de e-commerce**  
  que vendem produtos online (próprios ou via marketplace) e recebem avaliações em texto.

- **Times de atendimento ao cliente / operação**  
  que precisam de uma visão rápida de:

    - quais comentários são elogios;
    - quais são reclamações;
    - quais são mais críticos e precisam ser tratados primeiro.

- **Times técnicos (Dev / DS) em ambiente de hackathon ou estudo**  
  que querem um exemplo concreto de integração entre:

    - modelo de ML em Python,
    - API Java com Spring,
    - frontend simples,
    - banco relacional.

---

## 4. Objetivos do produto

### 4.1. Objetivo geral

Entregar uma API e uma interface simples que:

- recebam comentários de compradores;
- apliquem um modelo de Machine Learning para classificar o sentimento;
- registrem o resultado em banco;
- ofereçam ao vendedor um painel com:

    - estatísticas de sentimentos,
    - comentários críticos,
    - notificações,
    - exportação em JSON.

### 4.2. Objetivos específicos

- Demonstrar o fluxo completo **Comprador → Comentário → ML → Vendedor**.
- Permitir que várias equipes trabalhem em paralelo (DS, Backend, Frontend, DevOps).
- Servir como portfólio técnico organizado e bem documentado.

---

## 5. Escopo do produto

### 5.1. O que está dentro do escopo (MVP)

- Receber comentários com texto + nota, por produto.
- Classificar o sentimento via modelo de ML treinado em PT-BR.
- Persistir comentários, resultados de análise e notificações.
- Interface diferenciada para:
    - Cliente - Comprador (envio de comentário);
    - Cliente - Vendedor (dashboard de sentimentos e notificações).
- Notificação de comentários **negativos críticos** para o vendedor.
- Exportação dos dados em JSON.
- Documentação técnica completa (requisitos, arquitetura, banco, testes, segurança).

### 5.2. O que está fora do escopo (no MVP)

- Autenticação e autorização robustas (login real, JWT, etc.).
- Multi-tenant avançado (múltimas lojas, múltiplos ambientes).
- Modelo de ML altamente sofisticado ou de produção (deep learning, etc.).
- Interface super elaborada (gráficos complexos, animações avançadas).
- Mecanismos de fila/streaming (Kafka, RabbitMQ) — podem ser mencionados como evolução.

---

## 6. Benefícios esperados

### 6.1. Para o negócio

- Visão rápida da “temperatura” dos feedbacks (positivo/negativo/neutro).
- Capacidade de priorizar atendimento aos casos mais críticos.
- Base de dados organizada para alimentar relatórios externos.

### 6.2. Para a equipe técnica

- Exemplo real de:
    - API REST em Java 17 + Spring Boot;
    - integração com microserviço de ML em Python (FastAPI);
    - uso de banco PostgreSQL com esquema bem modelado;
    - frontend simples, mas alinhado ao domínio.
- Material de portfólio com:
    - documentação completa;
    - diagramas;
    - fluxo ponta a ponta.

---

## 7. Critérios de sucesso

O projeto é considerado bem-sucedido se:

1. É possível:
    - cadastrar um vendedor,
    - cadastrar um produto,
    - enviar um comentário como comprador,
    - ver o comentário refletido no dashboard do vendedor com sentimento analisado.
2. Comentários negativos críticos geram notificações para o vendedor.
3. O vendedor consegue exportar um JSON com comentários e classificações.
4. A documentação em `docs/` descreve claramente:
    - requisitos,
    - arquitetura,
    - dados,
    - testes,
    - segurança,
    - decisões arquiteturais (ADRs).
5. Qualquer pessoa técnica, lendo o repositório, consegue rodar o sistema localmente seguindo a documentação.

---

## 8. Riscos e limitações

- **Dados de treino limitados:**  
  O modelo de sentimento pode não generalizar para todos os domínios (ex.: política, humor, gírias muito específicas).

- **Recursos de infraestrutura restritos:**  
  Em cenários de hackathon, máquinas locais e ambientes gratuitos podem limitar experimentos mais pesados.

- **Escopo de segurança:**  
  O projeto não pretende ser uma solução pronta para produção, mas um MVP bem desenhado — ainda assim, boas práticas básicas são aplicadas.

---

Este documento é a “bússola” do projeto: tudo o que for desenvolvido deve ter ligação com esta visão.  
Mudanças grandes de objetivo ou escopo devem ser refletidas aqui.
