# ADR-004 – Carregamento do modelo treinado, fallback observável e contrato binário de sentimento

**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 19/09/2026
**Status:** Aprovado  
**Escopo:** Correção do `ds-service` para servir o modelo de ML real, observabilidade do modo de operação, e remoção da classe "Neutro" do contrato de sentimento

---

## 1. Contexto

Uma revisão técnica (`nginx/docs/revisao-tecnica-2026-09.md`) identificou que o serviço **nunca carregou o modelo de Machine Learning treinado** — operava permanentemente em um fallback heurístico de 7 palavras-chave, com probabilidades fixas (0.75 para "Positivo", 0.85 para "Negativo"), sem que nada no sistema sinalizasse esse estado. Confirmado por execução real, tanto localmente quanto contra a instância de produção na OCI.

**Causa-raiz:** o `.gitignore` do projeto ignorava `*.pkl` e `*.joblib` sob o comentário `"decida depois"` — uma decisão de versionamento nunca tomada. Sem o artefato no controle de versão, nenhuma implantação a partir de um clone limpo jamais teve o modelo disponível.

**Histórico do "Neutro" (informado pelo autor do projeto):** durante o hackathon, a equipe tentou implementar uma terceira classe de sentimento (Neutro). Houve dificuldade técnica e, pelo prazo curto, a classe foi descartada — mas a remoção foi aplicada **apenas no frontend** (o botão e o card de estatística foram ocultados na página web); o **backend manteve o contrato de três classes** (`enum Sentimento` com `NEUTRO`, `contracts/examples/neutral.json`, README). O vídeo de demonstração do projeto documenta a regra de binarização adotada (nota ≥ 4 → Positivo, nota ≤ 3 → Negativo, em 1:51) e trata "mais classes de sentimento" explicitamente como trabalho futuro (5:42) — o produto entregue e demonstrado sempre foi binário; o contrato do backend é que ficou desalinhado com o que foi de fato construído e apresentado.

## 2. Problema

Quatro decisões técnicas precisavam ser tomadas para corrigir o sistema, mantendo coerência entre o que o modelo real produz, o que a API promete, e o que a documentação descreve:

1. Como fazer o artefato do modelo chegar ao ambiente do `ds-service` de forma que sobreviva a um clone limpo.
2. Como tornar visível, de fora, se o serviço está classificando com o modelo real ou em modo de degradação.
3. O que fazer com a classe "Neutro", dado que o modelo é binário e nunca a produziu.
4. Como tratar, no contrato HTTP, um rótulo de sentimento que o backend não reconheça.

## 3. Decisão

### 3.1. Versionar o artefato do modelo no repositório

Os artefatos de treino (`TfidfVectorizer` + `LogisticRegression`) foram combinados num único `sklearn.Pipeline` (`datascience/build_pipeline.py`) e o resultado (`ds-service/models/sentiment.joblib`, 54 KB) passou a ser commitado. A regra do `.gitignore` foi removida.

### 3.2. Carregar o modelo real; manter o fallback, mas explícito e observável

`ds-service/app/model.py` passa a carregar o `Pipeline`. O fallback heurístico **continua existindo** — degradação graciosa é uma decisão boa — mas deixa de ser silencioso: `GET /health` do `ds-service` expõe `mode` (`"model"`/`"fallback"`), `model_loaded`, `model_version` (SHA-256 do artefato) e `fallback_reason`; a inicialização em modo fallback gera log `WARNING`. O backend propaga isso em `GET /api/v1/health`.

### 3.3. Remover a classe "Neutro" do contrato

**Medição, não suposição.** A alternativa de derivar "Neutro" de uma faixa de probabilidade (0,40 ≤ p ≤ 0,60) foi avaliada primeiro, executando o modelo sobre os 4.044 registros do dataset de treino:

| | |
|---|---|
| Registros que cairiam na faixa (virariam "Neutro") | 569 de 4.044 (14,1%) |
| Acurácia do modelo **dentro** da faixa | 61,9% |
| Acurácia do modelo **fora** da faixa | 92,1% |
| % de registros nota 4 (positivos) capturados pela faixa | 23,2% |
| % de registros nota 3 (proxy mais próximo de "neutro" no dataset) capturados | 19,5% |
| Exemplo canônico de neutro do próprio projeto (`contracts/examples/neutral.json`, "Recebi o produto. Ainda vou testar melhor.") | p(positivo) = 0,82 → classificaria como Positivo, não Neutro |

A faixa captura proporcionalmente mais notas 4 (claramente positivas) do que notas 3, e incide onde o modelo mais erra. **Ela encontra a zona de erro do classificador, não texto neutro.** Rotulá-la como "Neutro" apresentaria um erro do modelo como se fosse uma categoria de produto — a mesma família de problema do fallback silencioso que esta correção existe para eliminar.

Decisão: remover `NEUTRO` do enum `Sentimento`, do `contracts/examples/`, do frontend, do DDL e de toda a documentação de contrato — completando, no restante do sistema, a remoção que a equipe já havia decidido e feito parcialmente no frontend.

### 3.4. Alinhar a versão do scikit-learn

`ds-service/requirements.txt`: `scikit-learn` `1.5.2` → `1.6.1` (versão usada no treino). Confirmado por execução: com 1.5.2, o carregamento do artefato emite 4 `InconsistentVersionWarning` (um por estimador do `Pipeline`); com 1.6.1, nenhum.

### 3.5. `fromLabel()` deixa de ter default silencioso

Antes, `Sentimento.fromLabel()` devolvia `NEUTRO` para `null`, `""` ou qualquer rótulo desconhecido — o mecanismo exato que faria um erro de mapeamento se manifestar como "tudo virou Neutro", sem exceção nem log. Agora lança `IllegalArgumentException`. No endpoint singular, isso é reempacotado como `DsServiceException` (503 "Serviço Indisponível" — é uma violação de contrato do serviço upstream, não um erro interno do backend); no batch, o `catch` já existente converte o item em `"ERRO"`, logado em nível `ERROR`.

## 4. Alternativas consideradas

### 4.1. Para a classe "Neutro"

- **Faixa de probabilidade (não escolhida).** Preservaria o contrato de 3 classes sem retreino, mas a medição em §3.3 mostra que ela captura a zona de erro do modelo, não texto neutro.
- **Binário + campo de confiança explícito (não escolhida).** Entregaria o valor real que a faixa tentava entregar (sinalizar incerteza) sem chamar isso de sentimento. Descartada por custo/benefício: mesma superfície de mudança que a remoção completa, mais um campo novo no contrato, sem necessidade concreta identificada para este MVP.
- **Retreinar com 3 classes (não escolhida, fora de escopo).** Tecnicamente viável — a coluna `nota` (1–5) sobrevive no dataset limpo — mas exigiria redefinir os cortes de rótulo, inspecionar manualmente amostras de nota 3 (que na amostra examinada são reclamações, não texto neutro) e revalidar métricas. Trabalho de data science à parte.
- **Remover o Neutro (escolhida).** Sustentada pela medição e pelo histórico: a equipe já havia decidido remover a classe; o vídeo de demonstração e o produto entregue sempre foram binários.

### 4.2. Para o artefato do modelo

- **Git LFS / storage externo (não escolhida).** Os artefatos somam 54 KB — infraestrutura adicional sem ganho real nesse tamanho.
- **Versionar diretamente no repositório (escolhida).** Mesmo padrão já usado para `datascience/*.pkl`.

### 4.3. Para o rótulo de sentimento desconhecido no contrato HTTP

- **500 "Erro Interno" (comportamento anterior à revisão desta decisão, não escolhida).** Semanticamente incorreto — a causa é uma violação de contrato do serviço upstream, não um bug do backend.
- **502 Bad Gateway (avaliada e descartada).** Tecnicamente mais precisa (RFC 7231: resposta inválida de um servidor upstream), mas nenhum endpoint documenta esse código no Swagger atual — introduzi-lo exigiria expandir o contrato publicado sem que nenhum consumidor da API esteja preparado para recebê-lo.
- **503 "Serviço Indisponível", via `DsServiceException` (escolhida).** Reaproveita o handler e a mensagem já documentados nos dois endpoints (`@ApiResponse` 503).

## 5. Impactos da decisão

### 5.1. No `ds-service` (Python)

`model.py` carrega o `Pipeline`, mapeia `0`/`1` explicitamente para `"Negativo"`/`"Positivo"` (em vez de vazar os valores crus das classes), expõe `mode`/`model_loaded`/`model_version`/`fallback_reason`. `requirements.txt` com `scikit-learn==1.6.1`. `Dockerfile` sem alteração — o `COPY models ./models` passou a ter origem válida assim que o artefato foi versionado.

### 5.2. No backend (Java)

`Sentimento` sem `NEUTRO`. `StatsService`/`StatsResponse` sem os campos `neutros`/`percentual_neutros` — `/api/v1/stats` vai de 10 para 8 chaves. `DsServiceClient.isHealthy()` (booleano) vira `getHealth()` (`Optional<DsServiceHealth>`), propagado por `HealthController` em `/api/v1/health`. `SentimentService.analisar()` reempacota rótulo desconhecido como `DsServiceException`.

### 5.3. No frontend

`static/index.html` reconciliado com a versão em produção (que já tinha o Neutro oculto, mas nunca commitado — ver §6.3) e com remoção completa (não parcial) do Neutro: CSS, botão de exemplo, card de estatística, chave do objeto de exemplos, binding JS, ternário de emoji.

### 5.4. Na documentação

README (raiz e backend), model card, `database.md`, `requisitos.md` corrigidos para descrever o sistema real — modelo binário, domínio de treino (Hopi Hari), modo de degradação observável.

## 6. Consequências (curto e longo prazo)

### 6.1. Benefícios imediatos

A API passa a classificar com o modelo real (probabilidades variáveis, não mais 0.75/0.85 fixos). O modo de operação é observável de fora via `/health`, nos dois serviços. O contrato de sentimento corresponde ao que o modelo de fato produz. Um erro de mapeamento futuro (mudança de vocabulário do DS Service, por exemplo) aparece como exceção logada, não como um valor que passa despercebido.

### 6.2. Limitações conhecidas

- **Sensibilidade a acentuação.** O vocabulário TF-IDF contém 168 tokens acentuados. Medido: `"produto péssimo, veio com defeito e atrasou"` → Negativo, p(positivo)=0,178 (correto); a mesma frase sem acentos, `"produto pessimo..."` → Positivo, p(positivo)=0,536 (**incorreto**). Não normalizar acentos só na inferência — pioraria o resultado, já que o vocabulário treinado tem acentos. Correção exigiria consistência entre treino e inferência, fora de escopo.
- **Domínio de treino.** 4.044 avaliações do parque de diversões Hopi Hari — filas, brinquedos, estrutura. Não é e-commerce nem rede social. Acurácia fora desse domínio não foi medida e é esperada ser inferior.
- **Métricas de avaliação não medidas nesta revisão.** O split de treino/teste original (`Hackathon_One_Nb2.ipynb`, determinístico via `random_state=42`) nunca teve seu resultado salvo no repositório. Reportar um número agora exigiria reexecutar o notebook; um número calculado sobre o dataset inteiro seria in-sample e enganoso.
- **Medição de skew de pré-processamento — não é métrica de desempenho.** Antes de decidir se `build_pipeline.py` deveria replicar a função `limpar_texto()` do notebook de treino, comparei (pareado, in-sample, sobre os mesmos 4.044 registros) a acurácia do `Pipeline` sobre texto bruto (87,86%) contra texto pré-processado como no treino (87,81%) — 1,4% de predições divergentes, sem viés de direção. Conclusão: o `TfidfVectorizer` já absorve a maior parte do que a limpeza customizada fazia; replicá-la no serviço não é necessário. Esse número mede *diferença de pré-processamento*, não *qualidade do modelo* — não deve ser citado como métrica de desempenho.

### 6.3. Drift e dívida conhecida

- **`index.html` em produção tem CSS e ajustes (rodapé, links, cor) que nunca foram commitados** — indica edição direta na VM. Reconciliado nesta correção (produção → repositório), mas fica a pergunta de origem em aberto.
- **Auditoria de divergência VM × repositório, feita inteiramente por HTTP** (`etapa-04b-auditoria-drift-producao.md`): contrato OpenAPI e comportamento de `/api/v1/stats`/`/api/v1/health` de produção idênticos aos gerados pela `main`. **Isso prova identidade de interface, não de implementação** — uma mudança em lógica interna (limiar, palavra-chave do fallback, regra de negócio dentro de um service) produziria o mesmo contrato OpenAPI e não seria detectada por este método. Verificação completa (arquivos não expostos por HTTP: `docker-compose.yml` real, `.env` real, código do `ds-service` rodando na VM, Dockerfiles, logs) depende de acesso à VM.
- **Recuperação de acesso à VM está em andamento por outra frente (esforço "Bastion"), mas não está resolvida.** Registrado aqui como pendência em aberto, não como concluída — enquanto isso não acontecer, esta correção pode ser mesclada, mas não implantada.
- **`datascience/app.py` é um segundo serviço FastAPI morto**, paralelo ao `ds-service/`, com contrato incompatível (`{"sentiment_prediction": 0|1}`, sem `probability`, sem `label`). Não removido — dívida conhecida, fora de escopo desta correção.
- **`/api/v1/comentarios`** (endpoint que não existe na implementação) é referenciado em 11 documentos além do README (`traceability-matrix.md`, `test-report.md`, `test-strategy.md`, `roadmap.md`, `security.md`, `arquitetura.md`, `database.md`, `runbook.md`, `requisitos.md`, e histórico em `ADR-001`/`revisao-tecnica-2026-09.md`). Corrigido apenas no README nesta entrega; varrer os demais é trabalho seguinte.

## 7. Quando revisitar esta decisão

- Se o time decidir investir em retreinar com uma classe Neutro real (rótulos revisados manualmente, não derivados de nota ou de faixa de probabilidade).
- Se o acesso à VM for restaurado e a auditoria completa (arquivos não expostos por HTTP) revelar drift adicional além do `index.html`.
- Se o domínio de uso da API se expandir para além de avaliações de parque de diversões — a limitação de domínio (§6.2) deixaria de ser hipotética e passaria a exigir validação real.
- Se scikit-learn liberar uma versão que quebre compatibilidade com os artefatos serializados em 1.6.1 — exigiria reexportar o `Pipeline`.
