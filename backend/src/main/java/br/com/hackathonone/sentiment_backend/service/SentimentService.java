package br.com.hackathonone.sentiment_backend.service;

import br.com.hackathonone.sentiment_backend.client.SentimentDsClient;
import br.com.hackathonone.sentiment_backend.dto.SentimentRequest;
import br.com.hackathonone.sentiment_backend.dto.SentimentResponse;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictRequest;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentimentService {

    private final SentimentDsClient dsClient;

    public SentimentResponse analyzeSentiment(SentimentRequest request) {
        log.info("Iniciando análise de sentimento para texto: {}", request.getText());

        // 1. Monta request para o DS
        DsPredictRequest dsRequest = new DsPredictRequest(request.getText());

        // 2. Chama o DS (Feign)
        DsPredictResponse dsResponse = dsClient.predict(dsRequest);
        log.info("Resposta do DS recebida: Label={}, Prob={}", dsResponse.getLabel(), dsResponse.getProbability());

        // 3. Mapeia para o contrato público (label -> previsao, probability -> probabilidade)
        return SentimentResponse.builder()
                .previsao(dsResponse.getLabel())
                .probabilidade(dsResponse.getProbability())
                .build();
    }
}