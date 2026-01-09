# ADR-002 – Comprador não visualiza o sentimento previsto pelo modelo

**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025
**Status:** Aprovado  
**Escopo:** Comportamento da interface do Comprador e exposição de resultados de ML

---

## 1. Contexto

O sistema recebe comentários de **Clientes - Compradores** sobre produtos e realiza análise de sentimento usando um modelo de Machine Learning.

Do lado de negócio, os objetivos principais são:

- ajudar o **Vendedor** a priorizar comentários negativos;
- fornecer um **dashboard** com estatísticas de sentimento;
- registrar os resultados em banco para futuras análises.

Surgiu a dúvida: **o Comprador deve ou não ver o sentimento que o modelo atribuiu ao comentário que ele acabou de enviar?**

Algumas ideias iniciais sugeriam exibir algo como:

> “Seu comentário foi classificado como NEGATIVO (0.82).”

Ou:

> “Obrigado, percebemos que seu comentário é POSITIVO!”

Depois de analisar o contexto, os riscos e o propósito do MVP, foi necessário decidir o comportamento padrão.

---

## 2. Problema

Expor o rótulo de sentimento diretamente para o Comprador traz alguns riscos:

- O modelo pode errar (falso positivo/negativo) e isso pode causar estranheza:
    - “Eu elogiei e ele disse que é negativo?”
    - “Eu critiquei e ele disse que é positivo?”
- O foco do projeto é facilitar a **vida do Vendedor** e da empresa, não criar uma experiência “gamificada” para o Comprador.
- Explicar para o usuário final “como e por que” o sentimento foi calculado pode exigir textos longos ou UI mais complexa.
- Em termos de UX, o Comprador quer:
    - conseguir enviar o comentário,
    - ter a sensação de que foi ouvido,
    - eventualmente receber resposta ou solução, mas **não necessariamente saber como o sistema classificou internamente o texto**.

Do ponto de vista de segurança e ética:

- O modelo é uma ferramenta interna de apoio à decisão para o corpo interno da empresa.
- Expor a classificação diretamente para o cliente pode dar a impressão de que:
    - o sistema está julgando o sentimento da pessoa,
    - ou que a resposta da empresa será automatizada com base nisso, o que não é o caso no MVP.

---

## 3. Decisão

O **Cliente - Comprador NÃO verá o sentimento previsto** pelo modelo na interface.

### 3.1. Comportamento adotado

- Após enviar o comentário, o Comprador recebe apenas uma mensagem genérica do tipo:

  > “Seu comentário foi recebido. Obrigado pelo feedback!”

- A análise de sentimento ocorre internamente:
    - o resultado é gravado em `resultado_analise`;
    - se for um caso crítico (ex.: NEGATIVO com alta probabilidade), é gerada uma `notificacao` para o Vendedor;
    - o Comprador não tem visibilidade do rótulo, probabilidade ou da existência dessa classificação automática.

---

## 4. Alternativas consideradas

### 4.1. Exibir o sentimento para o Comprador (não escolhido)

**Descrição:**

- A UI mostraria algo como:
    - “Seu comentário foi classificado como POSITIVO (0.93)”.

**Prós:**

- Transparência sobre o funcionamento interno.
- Efeito “curioso” ou “divertido” para o usuário (pode parecer uma feature de IA).
- Poderia ser usado como elemento de marketing (“utilizamos IA para entender você!”).

**Contras:**

- Risco de frustração quando o modelo errar:
    - Percepção de “sistema burro” ou “me entendeu errado”.
- Desvia do foco do MVP:
    - O problema de negócio é priorizar atendimento, não entreter o cliente.
- Complica a UX:
    - obrigaria a explicar em algum lugar que se trata de uma previsão estatística, sujeita a erro.
- Pode pressionar o time a discutir fairness/bias/explicabilidade com clientes, o que foge do escopo do hackathon.

### 4.2. Expor apenas uma classificação “suavizada” (ex.: “seu comentário será priorizado”) (não escolhido)

**Descrição:**

- Ao invés de mostrar “NEGATIVO”, exibir mensagens do tipo:
    - “Seu comentário será encaminhado com prioridade para nossa equipe”.

**Prós:**

- Não expõe diretamente o rótulo técnico (positivo/negativo/neutro).
- Reforça a sensação de cuidado com o cliente.

**Contras:**

- Ainda depende do output do modelo.
- Se o modelo não marcar como crítico e o cliente achar que o comentário era grave, pode causar sensação de descaso.
- Pode criar expectativas não condizentes com a capacidade do time interno.

### 4.3. Não exibir o sentimento (decisão escolhida)

**Descrição:**

- O Comprador não tem nenhuma indicação sobre o resultado da classificação.
- A única resposta é uma confirmação de recebimento do comentário.

**Prós:**

- Interface simples e clara.
- Foco total na necessidade do Vendedor/empresa.
- Menos riscos de mal-entendidos com o cliente.
- Evita “prometer” algo que não existe (por exemplo, atendimento automático baseado em sentimento).

**Contras:**

- Menos “efeito uau” de IA para o usuário final.
- Menos transparência sobre o uso do modelo (embora esse detalhe possa ser explicado em termos e políticas internas).

---

## 5. Impactos da decisão

### 5.1. Na interface do Comprador

- O fluxo de UX fica enxuto:
    - o comprador vê produtos;
    - escolhe um produto;
    - envia avaliação (nota + texto);
    - recebe confirmação simples.
- Não há necessidade de:
    - exibir probabilidade,
    - explicar comportamento do modelo,
    - lidar com casos de discordância (“eu não concordo com esse rótulo”).

### 5.2. No backend e no banco

- A API continua registrando:
    - sentimento,
    - probabilidade,
    - flag `eh_critico`,
    - possível notificação para o vendedor.
- Esses dados são consumidos pela **interface do Vendedor** e pelo dashboard, não pela interface do Comprador.

### 5.3. Em testes e documentação

- Os testes de front relacionados ao Comprador verificam apenas:
    - que o comentário foi enviado com sucesso;
    - que a mensagem de confirmação aparece.
- A presença de `resultado_analise` no banco é validada via testes de backend e consultas do dashboard, não na UI do Comprador.

---

## 6. Consequências (curto e longo prazo)

### 6.1. Benefícios imediatos

- Menos código de interface e menos casos de borda a tratar.
- Menos espaço para discussão sobre “por que o modelo disse X e não Y”.
- O time consegue concentrar esforços em:
    - fluxo interno (comentário → análise → dashboard → notificação),
    - estabilidade e qualidade técnica.

### 6.2. Evolução futura possível

Se em algum momento o time decidir expor o resultado ao Comprador, isso pode ser feito de forma controlada, por exemplo:

- adicionando um “modo laboratório” para exibir o sentimento apenas em ambiente de testes;
- introduzindo textos de aviso, explicando que é uma análise automática sujeita a erro.

Mas essa é uma decisão futura, que só deve ser tomada se fizer sentido para o produto e for acompanhada de uma boa comunicação com o usuário final.

---

## 7. Quando revisitar esta decisão

Essa decisão pode ser revisitada se:

- o foco do produto mudar para uma experiência “educativa” ou “gamificada” com o cliente final;
- houver demanda explícita do cliente da solução (empresa) em mostrar transparência de classificação;
- o modelo atingir um nível de qualidade muito alto em domínios específicos e houver valor claro em exibir o resultado para o Comprador.

Enquanto o projeto estiver na fase de MVP descrita no hackathon, manteremos o Comprador **sem acesso direto** ao sentimento previsto pelo modelo.