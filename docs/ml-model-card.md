# Model Card · Hackathon One Sentiment API
**Projeto:** Hackathon One Sentiment API  
**Versão do documento:** 1.0  
**Data:** 28/12/2025

---

## 1. Identidade do modelo

- **Nome lógico do modelo:** `hackathon-one-sentiment-ptbr`
- **Tipo de modelo:** Classificador de texto (análise de sentimento)
- **Arquitetura:** `TF-IDF` + `LogisticRegression` (scikit-learn)
- **Tarefa:** Classificação de sentimento em português brasileiro
- **Saídas:**
    - `sentimento` ∈ {`POSITIVO`, `NEGATIVO`, `NEUTRO`}
    - `probabilidade` ∈ [0, 1]
- **Local do arquivo serializado (repositório):**  
  `datascience/ml_service/model/sentiment_model.pkl`
- **Equipe responsável:**  
  Equipe de Data Science (DS) do projeto **Hackathon One Sentiment API**

No banco de dados, este modelo deve ser registrado na tabela `modelo_ml` com algo semelhante a:

- `nome`: `hackathon-one-sentiment-ptbr`
- `versao`: `v1` (ou outra convenção definida pelo time)
- `tipo_modelo`: `LogisticRegression`
- `caminho_arquivo`: caminho completo do `.pkl` no ambiente de produção
- `f1_score`, `acuracia`: métricas preenchidas com resultados reais do treino

---

## 2. Objetivo e uso previsto

### 2.1. Problema que o modelo resolve

O modelo foi criado para:

- classificar automaticamente **comentários de clientes em português** (ex.: avaliações de produtos, feedbacks de atendimento);
- identificar se o sentimento predominante é:
    - **POSITIVO**
    - **NEGATIVO**
    - **NEUTRO** (opcional, dependendo do escopo final do dataset/modelo);
- ajudar **equipes de atendimento, marketing e operações** a:
    - priorizar respostas a comentários negativos;
    - ter uma visão agregada da satisfação ao longo do tempo;
    - identificar pontos críticos em produtos e serviços.

### 2.2. Contexto de uso dentro do projeto

No contexto do **Hackathon One Sentiment API**, o fluxo previsto é:

1. **Comprador** envia um comentário sobre um produto (texto + nota).
2. A **API Java (Spring Boot)** salva o comentário no banco (`comentario`).
3. A API faz uma requisição HTTP para o microserviço de ML (`POST /predict`), enviando o texto.
4. O microserviço de ML aplica o modelo e retorna:
```json
   {
     "label": "NEGATIVE",
     "probability": 0.82,
     "model_name": "hackathon-one-sentiment-ptbr",
     "model_version": "v1"
   }
````

5. A API grava o resultado da análise em `resultado_analise` e, se for negativo crítico, gera uma `notificacao` para o vendedor.
6. Apenas o **Vendedor** (ou equipe interna) vê os insights de sentimento no dashboard.
   O **Comprador não recebe explicitamente o rótulo do sentimento**.

### 2.3. Usos pretendidos

* Analisar sentimento de:

  * avaliações de produtos de e-commerce;
  * comentários em formulários de satisfação;
  * feedbacks de atendimento em português brasileiro.
* Apoiar:

  * priorização de tickets de suporte;
  * monitoramento de campanhas de marketing;
  * visão histórica de sentimento por produto/serviço.

### 2.4. Usos não recomendados / fora de escopo

Este modelo **não foi projetado** para:

* Tomar decisões automáticas que impactem direitos das pessoas, por exemplo:

  * concessão de crédito;
  * admissões/demissões;
  * bloqueio de contas;
  * restrição de acesso a serviços essenciais.
* Analisar:

  * textos altamente técnicos, jurídicos ou médicos;
  * textos em outros idiomas (inglês, espanhol etc.) sem retreino adequado;
  * sarcasmo complexos, ironia pesada ou humor muito contextual.
* Classificar ódio, discurso discriminatório, assédio ou outros temas sensíveis
  como tarefa específica de moderação de conteúdo.
  (Ele apenas retorna sentimento geral; não substitui um modelo especializado em segurança/moderação.)

---

## 3. Dados de treino e avaliação

> Os detalhes abaixo devem ser preenchidos com informações reais assim que o notebook de treino estiver consolidado.
> Aqui fica o “contrato” do que deve ser registrado.

### 3.1. Fontes de dados

* **Fonte principal (planejada):**
  Dataset público de sentimento em português, por exemplo:
  *Brazilian Portuguese Sentiment Analysis Datasets* (Kaggle) – contendo textos categorizados como positivos/negativos (e possivelmente neutros).

* **Possíveis fontes complementares:**

  * avaliações públicas de produtos;
  * reviews de apps (ex.: lojas de aplicativos);
  * outros datasets públicos de PT-BR com rótulos de sentimento.

### 3.2. Armazenamento e rastreabilidade

* O repositório de Data Science guarda:

  * notebooks em `datascience/ml_notebooks/`;
  * dados preparados em `datascience/data/`.
* O banco de dados possui a tabela `dataset_registro` com campos:

  * `texto`
  * `nota` (se o dataset trouxer rating 1–5)
  * `rotulo_original` (ex.: `POS`, `NEG`, `NEU`)
  * `fonte` (ex.: `Kaggle - BP Sentiment`)
  * `split` (`TRAIN`, `TEST`, `VALID`)
  * `data_importacao`
  * `id_externo` (id no dataset original, se houver)

Isso permite reconstruir quais amostras foram usadas em qual split e de onde vieram.

### 3.3. Tamanho do dataset

> Substitua os “TODO” após o treino:

* Total de exemplos: **TODO**
* Treino (`TRAIN`): **TODO**
* Validação (`VALID`): **TODO**
* Teste (`TEST`): **TODO**

### 3.4. Pré-processamento aplicado

Passos típicos de pré-processamento (a serem confirmados no notebook):

* normalização para minúsculas (`lowercase`);
* remoção opcional de:

  * pontuação;
  * dígitos;
  * múltiplos espaços;
* tokenização padrão (scikit-learn);
* remoção eventual de stopwords em PT-BR (a confirmar);
* vetorização com **TF-IDF**:

  * uni-gramas (palavras simples) e, possivelmente, bi-gramas;
  * limite mínimo de frequência (ex.: `min_df`);
  * limite máximo (ex.: `max_df` para remover palavras muito comuns).

---

## 4. Detalhes de treinamento

### 4.1. Ambiente de treinamento

* Linguagem: **Python 3.x**
* Principais bibliotecas:

  * `pandas`
  * `numpy`
  * `scikit-learn`
  * `joblib`
* Ambiente:

  * Jupyter Notebook (Colab / VS Code)
* Local do notebook:

  * `datascience/ml_notebooks/sentiment_model.ipynb` (nome sugerido)

### 4.2. Arquitetura do modelo

* **Pipeline scikit-learn** típico:

  ```python
  from sklearn.feature_extraction.text import TfidfVectorizer
  from sklearn.linear_model import LogisticRegression
  from sklearn.pipeline import Pipeline

  pipeline = Pipeline([
      ("tfidf", TfidfVectorizer(
          # configurar idioma, n-grams, min_df/max_df etc.
      )),
      ("clf", LogisticRegression(
          # C, penalty, max_iter, class_weight, etc.
      ))
  ])
  ```

* O pipeline completo é serializado com `joblib.dump(pipeline, "sentiment_model.pkl")`
  e usado diretamente no microserviço.

### 4.3. Hiperparâmetros (exemplo de configuração-alvo)

> Os valores abaixo são **referência**. A equipe DS deve registrar os valores finais usados.

* `TfidfVectorizer`:

  * `ngram_range`: `(1, 2)` (uni e bi-gramas)
  * `min_df`: valor a definir (ex.: 2 ou 5)
  * `max_df`: valor a definir (ex.: 0.8 ou 0.9)
  * `sublinear_tf`: `True` (a confirmar)
* `LogisticRegression`:

  * `C`: valor pós-tuning (ex.: 1.0, 2.0…)
  * `penalty`: `"l2"`
  * `solver`: `"liblinear"` ou `"saga"` (dependendo do conjunto e tamanho)
  * `max_iter`: valor suficiente para convergir (ex.: 1000)

### 4.4. Procedimento de treinamento

Fluxo sugerido:

1. Carregar dados rotulados em PT-BR.
2. Dividir em `TRAIN`, `VALID`, `TEST` (ou usar `train_test_split` + cross-validation).
3. Fazer tuning simples de hiperparâmetros (ex.: `GridSearchCV` ou `RandomizedSearchCV`).
4. Treinar modelo final usando:

   * todos os dados de treino,
   * melhores hiperparâmetros encontrados.
5. Avaliar no conjunto de teste.
6. Registrar métricas e anotar na tabela `modelo_ml`.
7. Exportar pipeline final com `joblib.dump`.

---

## 5. Avaliação e métricas

### 5.1. Tarefa de avaliação

* Tarefa: **classificação de sentimento** em 2 ou 3 classes (dependendo do dataset final):

  * binário: `POSITIVO` vs `NEGATIVO`
  * ou trinário: `POSITIVO` / `NEUTRO` / `NEGATIVO`

### 5.2. Métricas recomendadas

* **Acurácia (Accuracy)**
* **Precisão (Precision)** – por classe e macro
* **Recall** – por classe e macro
* **F1-score** – por classe e macro
* **Matriz de confusão**

### 5.3. Resultados esperados (template a ser preenchido)

Preencher a tabela a seguir com os valores reais obtidos no conjunto de teste:

| Métrica  | Valor global | Positivo | Negativo | Neutro (se aplicável) |
| -------- | -----------: | -------: | -------: | --------------------: |
| Acurácia |         TODO |        — |        — |                     — |
| Precisão |         TODO |     TODO |     TODO |                  TODO |
| Recall   |         TODO |     TODO |     TODO |                  TODO |
| F1-score |         TODO |     TODO |     TODO |                  TODO |

Além da tabela, é recomendado salvar a **matriz de confusão** no notebook e, se possível, anexar como imagem em `docs/` ou no próprio notebook.

---

## 6. Comportamento do modelo e limitações

### 6.1. Idioma e domínio

* O modelo foi treinado com **textos em português brasileiro**.
* Ele tende a funcionar melhor em:

  * comentários de produtos;
  * avaliações de experiência de compra;
  * frases relativamente curtas a médias.

**Limitações:**

* Pode ter queda de desempenho em:

  * textos em português europeu, mistos (PT-BR + EN), ou totalmente em outros idiomas;
  * jargões muito específicos de nichos pouco representados no dataset;
  * textos muito longos (parágrafos extensos) ou muito curtos (1–2 palavras).

### 6.2. Sarcasmo, ironia e contexto

* O modelo **não entende de forma robusta**:

  * sarcasmo complexo (“Nossa, chegou quebrado, que maravilha…”);
  * ironias onde o tom se inverte;
  * piadas internas que dependem de contexto externo.

Ele toma decisão apenas com base nas palavras presentes e na forma como foram vistas no dataset de treino.

### 6.3. Emojis, gírias e abreviações

* Emojis podem influenciar, mas o efeito depende de como aparecem no dataset.
* Gírias, abreviações, erros de ortografia e internetês podem:

  * tanto ajudar (se presentes no treino),
  * quanto prejudicar (se raros ou usados de forma diferente).

### 6.4. Probabilidades e limiares

* A `probabilidade` retornada pelo modelo vem do `predict_proba` da Regressão Logística.
* Ela **não é uma garantia absoluta** de confiança; é uma estimativa baseada nos dados de treino.
* Para marcar algo como “crítico”, o sistema pode usar um limiar extra:

  * ex.: `NEGATIVO` com probabilidade > `0.8` → `eh_critico = TRUE`.
* Esse limiar deve ser ajustado pela equipe com base em testes reais.

---

## 7. Considerações éticas e riscos

### 7.1. Papel do modelo no sistema

* O modelo **não toma decisões finais** sozinho.
* Ele é usado para:

  * marcar comentários potencialmente problemáticos;
  * alimentar um dashboard para o Vendedor;
  * auxiliar na priorização de atendimento.

A decisão de resposta, ação comercial ou qualquer medida sobre o cliente **é humana**.

### 7.2. Possíveis vieses

O modelo reflete o que vê no dataset. Logo:

* Se o dataset contiver:

  * predominância de um tipo de produto (ex.: tecnologia) → pode funcionar pior em outros domínios (ex.: serviços de saúde).
  * certa forma de escrever (região, faixa etária) → pode ter mais dificuldade em textos muito diferentes disso.

Por isso é importante:

* documentar bem as fontes dos dados;
* atualizar o modelo periodicamente com exemplos mais diversos;
* não usar este modelo para decisões sensíveis sobre pessoas.

### 7.3. Transparência para o usuário final

* O Comprador não recebe o “rótulo de sentimento” explicitamente; para ele, o sistema apenas confirma “comentário recebido”.
* O uso do modelo é **interno**, voltado para análise do Vendedor e da empresa.

Se, no futuro, o sistema passar a expor o rótulo ao cliente, é importante:

* deixar claro que se trata de análise automática;
* explicar que podem ocorrer erros.

---

## 8. Integração com o Hackathon One Sentiment API

### 8.1. Interface do microserviço de ML

Contrato sugerido (a ser documentado em `docs/contrato-api-ml.md`):

**Requisição:**

```http
POST /predict
Content-Type: application/json
```

```json
{
  "text": "Exemplo de comentário em português."
}
```

**Resposta:**

```json
{
  "label": "NEGATIVE",
  "probability": 0.82,
  "model_name": "hackathon-one-sentiment-ptbr",
  "model_version": "v1"
}
```

### 8.2. Mapeamento para o backend Java

* `label` → enum `Sentimento` (POSITIVE/NEGATIVE/NEUTRO)
* `probability` → campo `probabilidade` em `resultado_analise`
* `model_name`/`model_version` → utilizados para:

  * identificar/registrar `modelo_ml`;
  * rastrear qual versão do modelo gerou cada resultado.

A API Java:

1. recebe o texto de comentário;
2. chama o ML (`POST /predict`);
3. converte `label` para `Sentimento`;
4. grava em `resultado_analise`;
5. decide se `eh_critico` deve ser `TRUE` (ex.: NEGATIVO + prob alta);
6. se crítico → cria `notificacao` associada.

---

## 9. Versionamento e manutenção

### 9.1. Convenção de versão

Sugestão:

* `v1`, `v2`, `v3`… (simples, incremental)
  ou
* `v1.0.0`, `v1.1.0`, seguindo semântica de versão.

Cada versão do modelo deve:

* ter um arquivo `.pkl` com nome coerente (ex.: `sentiment_model_v2.pkl`);
* ter um registro correspondente na tabela `modelo_ml`:

  * `nome` = `hackathon-one-sentiment-ptbr`
  * `versao` = `v2`
  * `f1_score`, `acuracia` atualizados
  * `ativo` = `TRUE` (somente para a versão atualmente em produção)

### 9.2. Atualização do modelo em produção

Passos recomendados:

1. Treinar nova versão (ex.: `v2`) com dataset atualizado.
2. Avaliar e comparar métricas com a versão anterior.
3. Registrar nova versão em `modelo_ml` (com `ativo = FALSE` inicialmente).
4. Atualizar o arquivo `.pkl` no ambiente do ML service.
5. Fazer deploy de forma controlada (por exemplo, em ambiente de teste).
6. Quando validado:

   * marcar `ativo = FALSE` para a versão antiga;
   * marcar `ativo = TRUE` para a nova.
7. Documentar a mudança em:

   * `modelo_ml` (campos de métricas);
   * `CHANGELOG.md` (se mantido);
   * opcionalmente, um ADR específico (decisão de troca de modelo).

---

## 10. Reprodutibilidade

Para reproduzir o treinamento do modelo:

1. Abrir o notebook em `datascience/ml_notebooks/sentiment_model.ipynb`.
2. Baixar/carregar o dataset necessário (ou reutilizar o existente em `datascience/data/`).
3. Executar as células de:

   * carregamento de dados;
   * pré-processamento;
   * divisão em `TRAIN/VALID/TEST`;
   * treino e tuning da Regressão Logística;
   * avaliação (com métricas).
4. Atualizar qualquer parâmetro necessário.
5. Executar a célula de exportação (`joblib.dump`).
6. Substituir o arquivo em `datascience/ml_service/model/` (com cuidado para não quebrar a produção).
7. Atualizar o registro em `modelo_ml` no banco.

---

## 11. Checklist de qualidade do modelo

Antes de colocar uma nova versão do modelo em produção, verificar:

* [ ] Dataset documentado (fonte, tamanho, split).
* [ ] Notebook com:

  * [ ] EDA mínima (verificação de classes, balanceamento, exemplos).
  * [ ] Treino com pipeline claro (TF-IDF + Logistic Regression).
  * [ ] Métricas em teste registradas.
* [ ] `modelo_ml` atualizado no banco (nome, versão, métricas).
* [ ] Arquivo `.pkl` presente e carregando sem erro no ML service.
* [ ] Contrato `/predict` testado (com exemplos positivos/negativos/neutros).
* [ ] Integração com backend testada (comentário → resultado_analise → notificacao).
* [ ] Documentação deste `ml-model-card.md` atualizada (especialmente métricas e versão).
