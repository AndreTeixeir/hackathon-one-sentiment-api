# Stakeholders · Hackathon One Sentiment API

Este documento detalha quem são os principais interessados no projeto e qual a relação de cada um com o sistema.

---

## 1. Lista de stakeholders

### 1.1. Cliente - Comprador

- **Descrição:**  
  Usuário final que compra produtos e envia comentários.
- **Interesses:**
    - conseguir registrar sua opinião de forma rápida;
    - sentir que seu feedback foi recebido;
    - eventualmente ver melhorias na loja.
- **Interação com o sistema:**
    - usa a tela `comprador.html` para navegar por produtos e enviar comentários.

### 1.2. Cliente - Vendedor

- **Descrição:**  
  Dono do e-commerce (ou responsável pelas vendas) que utiliza o sistema para acompanhar feedback dos clientes.
- **Interesses:**
    - saber rapidamente se há muitos clientes insatisfeitos;
    - enxergar quais produtos estão com melhor/ pior avaliação;
    - priorizar atendimento a casos críticos.
- **Interação com o sistema:**
    - usa `vendedor.html` para:
        - cadastrar produtos;
        - ver dashboard de sentimentos;
        - consultar comentários;
        - receber notificações;
        - exportar dados.

### 1.3. Equipe de Data Science

- **Descrição:**  
  Grupo responsável pelo ciclo de dados → modelo de ML.
- **Interesses:**
    - ter dados minimamente organizados;
    - conseguir treinar, avaliar e versionar modelos;
    - expor o modelo de maneira clara para o backend consumir.
- **Interação com o sistema:**
    - cria notebooks e código de ML em `/datascience/`;
    - atua no microserviço `ml_service` (FastAPI);
    - consulta o banco (via `dataset_registro`, `modelo_ml`) quando necessário.

### 1.4. Equipe de Backend (Java)

- **Descrição:**  
  Responsável pela API principal, regras de negócio e integração com ML e DB.
- **Interesses:**
    - manter uma API clara, estável e testável;
    - ter um banco bem modelado;
    - garantir que os fluxos críticos (comentário → análise → notificação) funcionem.
- **Interação com o sistema:**
    - implementa controllers, services e repositories em `/backend`;
    - consome `/predict` no microserviço ML;
    - persiste e lê dados do PostgreSQL.

### 1.5. Equipe de Frontend

- **Descrição:**  
  Responsável pelas telas usadas por comprador e vendedor.
- **Interesses:**
    - ter endpoints estáveis para consumir;
    - saber claramente o formato do JSON de entrada/saída;
    - oferecer uma experiência simples e fluida.
- **Interação com o sistema:**
    - desenvolve `login.html`, `comprador.html`, `vendedor.html` e scripts JS;
    - usa `API_BASE_URL` configurada em `config.js`.

### 1.6. Equipe de DevOps / Infra

- **Descrição:**  
  Responsável por automação, ambientes e deploy (mesmo que simples).
- **Interesses:**
    - padronizar a forma como o sistema sobe (local/contêiner);
    - preparar scripts de inicialização;
    - evitar vazamento de segredos.
- **Interação com o sistema:**
    - atua em `docker-compose.yml` (futuro);
    - configura variáveis de ambiente;
    - pode criar pipelines de CI/CD.

### 1.7. Equipe de QA / Testes

- **Descrição:**  
  Responsável por planejar e acompanhar a qualidade (mesmo que de forma simples).
- **Interesses:**
    - garantir que requisitos principais estejam cobertos por testes;
    - registrar o que foi testado e o que não foi;
    - ter cenários claros documentados.
- **Interação com o sistema:**
    - usa `docs/test-strategy.md` e `docs/test-report.md`;
    - testa fluxos via Postman, navegador, scripts automáticos quando possível.

---

## 2. Mapa de interesse vs. influência

Visão simplificada:

| Stakeholder          | Interesse no projeto | Influência sobre decisões | Observações                             |
|----------------------|----------------------|---------------------------|-----------------------------------------|
| Cliente - Comprador  | Alto                 | Baixa/Média               | Define usabilidade do fluxo de review  |
| Cliente - Vendedor   | Muito alto           | Alta                      | Principal “cliente” do sistema          |
| Equipe de DS         | Alta                 | Alta (lado técnico)       | Define o coração do modelo de sentimento|
| Equipe de Backend    | Alta                 | Alta (lado técnico)       | Define APIs, integra tudo               |
| Equipe de Frontend   | Alta                 | Média                     | Define como o sistema é percebido       |
| Equipe de DevOps     | Média                | Média                     | Influência na forma de rodar o sistema  |
| Equipe de QA         | Alta                 | Média                     | Influência na qualidade final           |

---

## 3. RACI simplificado por área

Legenda:

- **R** – Responsible (responsável por executar)
- **A** – Accountable (dono final da decisão)
- **C** – Consulted (consultado)
- **I** – Informed (informado)

| Atividade / Entregável               | DS | Backend | Frontend | DevOps | QA | Vendedor | Comprador |
|--------------------------------------|----|---------|----------|--------|----|----------|-----------|
| Modelo de sentimento (.pkl)         | R,A| C       | I        | I      | C  | I        | I         |
| Microserviço `/predict`             | R  | C       | I        | C      | C  | I        | I         |
| API de comentários e stats          | C  | R,A     | C        | C      | C  | C        | I         |
| Tela de comprador                   | I  | C       | R,A      | I      | C  | I        | C         |
| Tela de vendedor / dashboard        | I  | C       | R,A      | I      | C  | C        | I         |
| Banco de dados / schema             | C  | R,A     | I        | C      | C  | I        | I         |
| Deploy local / contêiner            | I  | C       | C        | R,A    | I  | I        | I         |
| Estratégia de testes                | C  | C       | C        | I      | R,A| I        | I         |

---

## 4. Uso deste documento

Este arquivo serve como:

- referência rápida de “quem é quem”;
- base para entender o impacto de mudanças (ex.: mudança na API afeta Backend, Frontend, QA e, indiretamente, o Vendedor).

Se novos perfis surgirem (ex.: time de Data Engineering, time de Segurança), eles podem ser adicionados aqui.
