# Descrição Geral

## Setor de negócio

Atendimento ao cliente / Marketing / Operações — empresas que coletam opiniões de clientes (avaliações, comentários em redes sociais, pesquisas de satisfação) e querem entender rapidamente se o sentimento é positivo, neutro ou negativo.

## Descrição do projeto

Criar uma API simples que recebe textos (comentários, avaliações ou tweets), aplica um modelo de Data Science para classificar o sentimento (Atrasado / Pontual → neste caso: Positivo / Neutro / Negativo ou binário Positivo / Negativo) e retorna o resultado em formato JSON, permitindo que aplicações consumam essa predição automaticamente.

## Necessidade do cliente (explicação não técnica)

Um cliente (empresa) recebe muitos comentários e não consegue ler tudo manualmente. Ele quer:

* saber rapidamente se os clientes estão reclamando ou elogiando;

* priorizar respostas a comentários negativos;

* medir a satisfação ao longo do tempo.

Esse projeto oferece uma solução automática para classificar mensagens e gerar informações acionáveis.

## Validação de mercado

Analisar sentimento é útil para:

* acelerar atendimento ao cliente (identificar urgências);

* monitorar campanhas de marketing;

* comparar a imagem da marca ao longo do tempo.

Mesmo uma solução simples (modelo básico) tem valor: empresas pequenas e médias usam ferramentas similares para entender feedbacks sem equipe dedicada.

## Expectativa para este hackathon

**Público:** alunos sem experiência profissional na área de tecnologia, que estudaram Back-end (Java, Spring, REST, persistência) e Data Science (Python, Pandas, scikit-learn, notebooks).

**Objetivo:** entregar um MVP funcional que demonstre integração entre DS e Back-end: um notebook com o modelo + uma API que carrega esse modelo e responde a requisições.

**Escopo recomendado:** classificação binária (Positivo / Negativo) ou trinária (Positivo / Neutro / Negativo) com um modelo simples — por exemplo, usar TF-IDF (uma técnica que transforma o texto em números, mostrando quais palavras são mais importantes) junto com Regressão Logística (um modelo de aprendizado de máquina que aprende a diferenciar sentimentos).

## Entregáveis desejados

### Notebook (Jupyter/Colab) do time de Data Science contendo:

* Exploração e limpeza dos dados (EDA);

* Transformação dos textos em números com TF-IDF;

* Treinamento de modelo supervisionado (ex.: Logistic Regression, Naive Bayes);

* Métricas de desempenho (Acurácia, Precisão, Recall, F1-score);

* Serialização do modelo (joblib/pickle).

### Aplicação Back-End (preferencialmente Spring Boot em Java):

* API que consome o modelo (diretamente ou chamando o microserviço DS) e expõe endpoint /sentiment;

* Endpoint que recebe informações e retorna a previsão do modelo;

* Logs e tratamento de erros.

### Documentação mínima (README):

* Como executar o modelo e a API;

* Exemplos de requisição e resposta (JSON);

* Dependências e versões das ferramentas.

### Demonstração funcional (Apresentação curta):

* Mostrar a API em ação (via Postman, cURL ou interface simples);

* Explicar como o modelo chega à previsão.

**Funcionalidades exigidas (MVP):** O serviço deve expor um endpoint que retorna a classificação do sentimento e a probabilidade associada a essa classificação. Exemplo: POST /sentiment — aceita JSON com campo text e retorna: { "previsao": "Positivo", "probabilidade": 0.87 }

**Modelo treinado e carregável:** o back-end deve conseguir usar o modelo (carregando arquivo) ou fazer uma requisição a um microserviço DS que implemente a predição.

**Validação de input:** checar se text existe e tem comprimento mínimo; retornar erro amigável em caso contrário.

**Resposta clara:** label (+ probabilidade em 0–1) e mensagem de erro quando aplicável.

**Exemplos de uso:** Postman/cURL com 3 exemplos reais (positivo, neutro, negativo).

**README** explicando como rodar (passos simples) e como testar o endpoint.

### Funcionalidades opcionais

**Endpoint:** Endpoint GET /stats com estatísticas simples (percentual de positivos/negativos nos últimos X comentários).

**Persistência:** salvar requisições e previsões em banco (H2 ou Postgres) para análises posteriores.

**Explicabilidade básica:** retornar as palavras mais influentes na predição (ex.: "top features": ["ótimo", "atendimento"]).

**Interface simples:** Interface simples (Streamlit / página web) para testar texto livremente.

**Batch processing:** endpoint para enviar vários textos em CSV e receber previsões em lote.

**Versão multilingue:** Versão multilingue (Português + Espanhol) ou opção para trocar o threshold de probabilidade.

**Containerização:** Containerização com Docker e docker-compose para subir DS + BE juntos.

**Testes automatizados:** alguns testes unitários e um teste de integração simples.

**Orientações técnicas para alunos:** Recomendamos cuidado quando da utilização limitada das instâncias fornecidas pelos serviços always free da OCI, para não acarretar em gastos adicionais.

### Time de Data Science

* Cada equipe deve escolher ou montar seu próprio conjunto de dados de comentários, avaliações ou postagens que possam ser usados para análise de sentimento (ex.: reviews públicos, tweets, avaliações de produtos etc.).

* use Python, Pandas para ler/limpar dados;

* crie um modelo simples (TF-IDF + LogisticRegression do scikit-learn);

* salve o pipeline e o modelo com joblib.dump.

* Coloque tudo em um notebook bem comentado.

### Time de Back-End

* Crie uma API REST (em Java com Spring Boot).

* Implementar um endpoint (ex: /sentiment ) que recebe a avaliação e retorna o sentimento

* Integrar o modelo de Data Science:

* Via microserviço Python (FastAPI/Flask), ou

* Carregando o modelo exportado (ONNX, para times Java avançados).

* Validar entradas e retornar respostas JSON consistentes.

### Contrato de integração (definido entre DS e BE)

Recomendamos definir desde o início o formato JSON de entrada e saída. Segue um exemplo:

```` json
{
"text": "…"
}
→
{
"previsao":"Positivo",
"probabilidade":0.9
}
````