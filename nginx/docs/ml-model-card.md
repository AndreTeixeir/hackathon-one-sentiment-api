# Model Card · Hackathon One Sentiment API
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 2.0  
**Data:** 19/09/2026  
**Escopo:** Modelo de classificação de sentimento em uso na branch `main`

---

> **Nota sobre esta revisão.** A versão 1.0 deste documento descrevia uma arquitetura planejada (dataset Kaggle, tabela `modelo_ml`, contrato `/predict` com `model_name`/`model_version`) que nunca foi implementada na `main` — era um template preenchido com suposições, não com o que o time efetivamente construiu. Esta revisão substitui as seções por descrição do sistema real, verificado por execução (ver ADR-004). Onde uma métrica não foi medida, o documento diz isso explicitamente, em vez de manter um `TODO` como se fosse pendência trivial.

## 1. Identidade do modelo

- **Arquitetura:** `TfidfVectorizer` + `LogisticRegression` (scikit-learn), combinados num único `sklearn.Pipeline`.
- **Tarefa:** classificação binária de sentimento em português brasileiro.
- **Saídas:**
    - `label` ∈ {`Positivo`, `Negativo`} — **não existe classe Neutro** (ver §5.1 e ADR-004).
    - `probability` ∈ [0, 1] — probabilidade da classe prevista, vinda de `predict_proba`.
- **Artefato serializado:** `ds-service/models/sentiment.joblib`, versionado no repositório (54 KB). Gerado por `datascience/build_pipeline.py` a partir de `datascience/tfidf_vectorizer.pkl` + `datascience/sentiment_model.pkl`.
- **Identificação de versão em uso:** não há um nome/versão semântica atribuídos ao modelo — o `/health` do `ds-service` expõe `model_version` como o **SHA-256 do arquivo `.joblib`**, verificável independentemente com `sha256sum` e estável enquanto o artefato não mudar. Não existe tabela `modelo_ml` nem qualquer outro registro de versionamento no banco nesta implementação.
- **Equipe responsável:** equipe de Data Science do Hackathon One (ver `datascience/README_DS.md` para o histórico de decisões).

## 2. Objetivo e uso previsto

### 2.1. Problema que o modelo resolve

Classifica um texto em português como `Positivo` ou `Negativo`, com uma probabilidade associada. É o motor de inferência por trás de `POST /api/v1/sentiment` e `POST /api/v1/sentiment/batch`.

### 2.2. Fluxo real de integração

1. Um cliente HTTP chama `POST /api/v1/sentiment` no backend Spring Boot, com `{"text": "..."}`.
2. O backend (`DsServiceClient`) chama `POST /predict` no `ds-service` (FastAPI), com o mesmo texto.
3. O `ds-service` roda o `Pipeline` (ou o fallback heurístico, se o modelo não estiver carregado — ver §6.5) e devolve `{"label": "Positivo"|"Negativo", "probability": 0.0-1.0}`.
4. O backend mapeia `label` para o enum `Sentimento` (`Sentimento.fromLabel`) e persiste o resultado na tabela `analise_resultado` (entidade JPA `AnaliseResultado`, coluna `sentimento` com `CHECK (sentimento IN ('POSITIVO', 'NEGATIVO'))`).
5. A resposta (`previsao`, `probabilidade`) volta diretamente ao chamador de `/api/v1/sentiment` — **não há distinção de papel "Comprador" vs "Vendedor"** nesta implementação; quem chama o endpoint recebe a classificação na própria resposta HTTP. (Essa distinção existe como decisão de produto registrada no ADR-002, mas pertence a uma arquitetura de backend diferente da que está em `main` — ver a ressalva de pacotes divergentes no `CLAUDE.md` do projeto.)

Não há tabela `comentario`, `notificacao` ou `modelo_ml` na implementação atual — essas entidades pertencem ao schema descrito em `ddl/schema-postgres.sql`, que diverge da entidade JPA realmente usada (`AnaliseResultado`/`analise_resultado`). Essa divergência de schema é conhecida e está fora do escopo desta correção.

### 2.3. Usos pretendidos

Classificar comentários em texto livre, em português, no domínio de **avaliações de parque de diversões** (ver §3.1 — é o domínio real de treino, não e-commerce ou redes sociais).

### 2.4. Usos não recomendados / fora de escopo

- Decisões automáticas que afetem direitos de pessoas (crédito, admissão/demissão, bloqueio de acesso).
- Textos fora do domínio de treino sem validação prévia (jurídico, médico, técnico).
- Textos em outro idioma que não português.
- Moderação de conteúdo (discurso de ódio, assédio) — o modelo classifica sentimento geral, não é especializado nisso.
- Qualquer inferência que dependa de reconhecer sarcasmo complexo ou ironia — não testado, não confiável (ver §6.2).

---

## 3. Dados de treino e avaliação

### 3.1. Fonte de dados — real, não planejada

**Dataset:** `datascience/hopi_hari (1).csv` — **4.044 avaliações do parque de diversões Hopi Hari**, com colunas `comentario`, `nota` (1 a 5) e `data`. Não é um dataset de e-commerce nem de redes sociais — é avaliação de experiência em parque temático (filas, brinquedos, estrutura, atendimento).

### 3.2. Rotulagem (binarização)

Decisão da equipe de DS, documentada no vídeo de demonstração do projeto (1:51) e implementada em `datascience/Hackathon_One_Nb1.ipynb`:

- `nota` ≤ 3 → classe `0` (Negativo) — **2.157 registros**
- `nota` ≥ 4 → classe `1` (Positivo) — **1.887 registros**

Não existe classe intermediária no dataset original nem no rótulo derivado.

### 3.3. Tamanho e split

- Total: 4.044 registros.
- Split treino/teste: `Hackathon_One_Nb2.ipynb` usa `train_test_split(test_size=0.2, random_state=42, stratify=y)` — mas **as métricas dessa execução específica não foram salvas em lugar nenhum do repositório** (nem no notebook versionado, nem em arquivo à parte). Não há como recuperá-las sem re-executar o notebook. Ver §5.3.

### 3.4. Pré-processamento aplicado no treino

`Hackathon_One_Nb1.ipynb`, célula 18, função `limpar_texto()`: minúsculas, remove tudo que não seja letra (incluindo acentuadas: `áàâãéèêíïóôõöúçñ`) ou espaço, colapsa espaços múltiplos. Aplicada uma vez sobre o dataset bruto, salva em `datascience/dataset_sentimento_limpo.csv` — é sobre esse CSV já limpo que o `TfidfVectorizer` é treinado (`Hackathon_One_Nb2.ipynb`).

**O serviço de inferência (`ds-service`) NÃO replica essa limpeza antes de vetorizar.** Isso foi medido, não presumido: rodando o `Pipeline` sobre os 4.044 registros originais, comparando texto bruto contra texto processado com `limpar_texto()`, a acurácia foi 87,86% (bruto) contra 87,81% (limpo) — diferença de 0,05 ponto percentual, 1,4% de predições divergentes, sem viés de direção. O `TfidfVectorizer` (`lowercase=True` por padrão, tokenizador que já ignora pontuação) absorve a maior parte do que a limpeza customizada fazia; a fresta remanescente (fusão de palavras hifenizadas) é um efeito estreito, não sistemático. **Conclusão: replicar `limpar_texto()` no serviço não é necessário.**

### 3.5. Vetorização (TF-IDF)

Parâmetros reais usados em `Hackathon_One_Nb2.ipynb`:

```python
TfidfVectorizer(
    max_features=1000,
    min_df=2,
    max_df=0.95,
    ngram_range=(1, 2),
    stop_words=stopwords.words('portuguese'),  # via nltk
)
```

Vocabulário final: 1.000 features, incluindo 168 tokens com acento — relevante para a limitação descrita em §6.1.

---

## 4. Detalhes de treinamento

### 4.1. Ambiente

- Python 3, Google Colab.
- Bibliotecas: `pandas`, `numpy`, `scikit-learn` **1.6.1**, `nltk` (stopwords), `joblib`.
- Notebooks: `datascience/Hackathon_One_Nb1.ipynb` (limpeza + rotulagem) → `datascience/Hackathon_One_Nb2.ipynb` (vetorização + treino + serialização).

### 4.2. Classificador

```python
LogisticRegression(
    max_iter=1000,
    solver='lbfgs',
)
```
(`penalty='l2'` é o default do scikit-learn — não foi customizado.)

### 4.3. Combinação em Pipeline único (etapa desta correção, não do notebook original)

O notebook original serializa `tfidf_vectorizer.pkl` e `sentiment_model.pkl` como **dois objetos separados**. `datascience/build_pipeline.py` os combina num único `sklearn.Pipeline` e salva em `ds-service/models/sentiment.joblib`, para que o serviço não precise saber que existe uma etapa de vetorização separada. Verificado por execução: o artefato combinado carrega com scikit-learn 1.6.1 sem `InconsistentVersionWarning`, e reproduz exatamente as mesmas probabilidades dos dois objetos originais.

**scikit-learn 1.6.1 é a versão de treino, e precisa ser a versão de serving.** Testado nos dois sentidos: com scikit-learn 1.5.2 (versão que estava pinada no `ds-service/requirements.txt` antes desta correção), o carregamento emite 4 `InconsistentVersionWarning` (um por estimador do Pipeline); com 1.6.1, nenhum.

---

## 5. Avaliação e métricas

### 5.1. Tarefa — binária, não trinária

A versão anterior deste documento especulava "2 ou 3 classes, dependendo do dataset final". **É definitivamente binária.** A alternativa de derivar "Neutro" de uma faixa de probabilidade (0,40 ≤ p ≤ 0,60) foi avaliada e descartada — medição sobre os 4.044 registros:

| | |
|---|---|
| Registros na faixa 0,40–0,60 | 569 de 4.044 (14,1%) |
| Rótulo verdadeiro dos capturados | 314 positivos / 255 negativos |
| % de registros nota 4 capturados pela faixa | 23,2% |
| % de registros nota 3 capturados pela faixa (nota 3 é o proxy mais próximo de "neutro" no dataset) | 19,5% |
| Acurácia do modelo dentro da faixa | 61,9% |
| Acurácia do modelo fora da faixa | 92,1% |

A faixa captura proporcionalmente **mais** notas 4 (claramente positivas) do que notas 3, e incide justamente onde o modelo erra mais. Ou seja: a faixa de baixa confiança não corresponde a texto neutro — corresponde à zona de erro do classificador. Rotulá-la como "Neutro" apresentaria um erro do modelo como se fosse uma categoria de produto.

### 5.2. Métricas — não medidas nesta revisão

**Acurácia, precisão, recall e F1-score não são reportados aqui.** O split de treino/teste original (`Hackathon_One_Nb2.ipynb`, `random_state=42`) nunca teve seu resultado salvo — nem no próprio notebook versionado (as células de avaliação existem, mas a saída não foi persistida), nem em arquivo separado. Qualquer número que fosse calculado agora, re-executando o notebook ou avaliando sobre o dataset inteiro, seria **in-sample** (o modelo já viu esses dados no treino) e enganoso se apresentado como métrica de generalização.

**Para obter métricas reais:** re-executar `Hackathon_One_Nb2.ipynb` do zero (o split é determinístico via `random_state=42`), capturar a saída de `classification_report` sobre o conjunto de teste, e atualizar esta seção com os valores reais e a data da medição.

---

## 6. Comportamento do modelo e limitações

### 6.1. Sensibilidade a acentuação — limitação verificada, não teórica

O vocabulário TF-IDF contém 168 tokens acentuados. Texto sem acento perde essas features. Medido:

| Texto | Probabilidade (positivo) | Classificação |
|---|---|---|
| `"produto péssimo, veio com defeito e atrasou"` | 0,178 | Negativo (correto) |
| `"produto pessimo, veio com defeito e atrasou"` (mesma frase, sem acentos) | 0,536 | Positivo (**incorreto**) |

**Não normalizar acentos só na inferência** — o vocabulário treinado *tem* acentos; normalizar de um lado só pioraria o resultado. A correção exigiria consistência entre treino e inferência (reprocessar o dataset e retreinar), fora do escopo desta correção. Fica registrada como limitação conhecida, a ser considerada na documentação voltada ao usuário (README).

### 6.2. Domínio de treino — parque de diversões, não e-commerce

O modelo foi treinado exclusivamente com avaliações do parque Hopi Hari — filas, brinquedos, estrutura, atendimento. Não há nenhuma avaliação de e-commerce, aplicativo ou rede social no dataset de treino. Desempenho fora desse domínio não foi medido e é esperado ser inferior.

### 6.3. Sarcasmo, ironia e contexto

Sem mudança em relação à avaliação original: o modelo decide com base nas palavras presentes e em como apareceram no treino, não interpreta sarcasmo ou ironia de forma robusta.

### 6.4. Probabilidades

`probability` vem de `predict_proba` da Regressão Logística — é uma estimativa baseada nos dados de treino, não uma garantia de confiança. Não existe, na implementação atual, nenhum limiar adicional (tipo "crítico") aplicado sobre essa probabilidade.

### 6.5. Modo de degradação — explícito, não silencioso

Se o artefato `.joblib` não existir ou falhar ao carregar (arquivo ausente ou corrompido), o `ds-service` cai num fallback heurístico de 7 palavras-chave (`ruim`, `péssim`, `horr`, `defeito`, `demor`, `atras`, `não recomendo` → Negativo com p=0,85; qualquer outro texto → Positivo com p=0,75). **Isso é uma decisão de degradação graciosa, mantida de propósito** — o que mudou nesta correção é que o modo deixou de ser silencioso: `/health` do `ds-service` expõe `mode` (`"model"`/`"fallback"`), `model_loaded`, `model_version` e `fallback_reason`, e a inicialização em modo fallback gera um log `WARNING`. Antes desta correção, o fallback era permanente e indistinguível do modelo real de fora — era exatamente esse o problema que a revisão técnica original identificou (ver ADR-004).

---

## 7. Considerações éticas e riscos

Sem mudança material da versão anterior: o modelo não toma decisão final sozinha, reflete os vieses do dataset de treino (domínio único — parque de diversões — e período limitado de coleta), e não deve ser usado para decisões sensíveis sobre pessoas.

A distinção "Comprador não vê o rótulo" (ADR-002) é uma decisão de produto de uma arquitetura de backend diferente da que está em `main`. Na API pública desta implementação (`POST /api/v1/sentiment`), quem chama o endpoint recebe a classificação diretamente na resposta HTTP — não há papel de usuário que a oculte.

---

## 8. Reprodutibilidade

1. `datascience/Hackathon_One_Nb1.ipynb` — carrega `hopi_hari (1).csv`, aplica `limpar_texto()`, aplica a regra de binarização, salva `dataset_sentimento_limpo.csv`.
2. `datascience/Hackathon_One_Nb2.ipynb` — carrega o CSV limpo, treina `TfidfVectorizer` + `LogisticRegression`, salva `tfidf_vectorizer.pkl` e `sentiment_model.pkl`.
3. `datascience/build_pipeline.py` — combina os dois num `sklearn.Pipeline`, salva `ds-service/models/sentiment.joblib`. Requer scikit-learn 1.6.1 no ambiente que roda o script (mesma versão do treino).
4. Reiniciar o `ds-service` (ou o container) para carregar o novo artefato.

`datascience/Hackathon_One_Nb3.ipynb` é um experimento histórico (tentativa de classificação ternária com Neutro, descartada por restrição de tempo do hackathon — ver `datascience/README_DS.md` e ADR-004). Não faz parte do pipeline de produção.

---

## 9. Checklist de qualidade do modelo

Antes de trocar o artefato em produção:

- [ ] Artefato gerado por `datascience/build_pipeline.py`, carregando sem `InconsistentVersionWarning` na versão de scikit-learn pinada em `ds-service/requirements.txt`.
- [ ] `sha256sum` do artefato registrado (é o `model_version` que aparecerá em `/health`).
- [ ] Teste manual com pelo menos um texto claramente positivo e um claramente negativo, confirmando probabilidade variável (não os valores fixos do fallback, 0.75/0.85).
- [ ] `/health` do `ds-service` reportando `mode: "model"`.
- [ ] Se as métricas de avaliação foram recalculadas, atualizar a §5.2 com os valores e a data.
