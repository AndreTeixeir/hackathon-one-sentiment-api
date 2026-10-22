# Marco 1: Integração Backend <-> Data Science

## Objetivo
Implementar o endpoint público `/sentiment` e integrá-lo ao serviço de Data Science via HTTP.

## Alterações Realizadas
* **DTOs**: Criação de `SentimentRequest/Response` e `DsPredictRequest/Response`.
* **Feign Client**: Configuração no Main (`@EnableFeignClients`) e interface `SentimentDsClient`.
* **Lógica**: Implementação do `SentimentService` para orquestração.
* **API**: `SentimentController` expondo POST `/sentiment`.

## Validação
Execução via cURL com sucesso:
- Request: `{"text": "Teste Positivo"}`
- Response: `{"previsao": "Positivo", "probabilidade": 0.85}`