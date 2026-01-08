package br.com.hackathonone.sentiment_backend.service;

import br.com.hackathonone.sentiment_backend.client.SentimentDsClient;
import br.com.hackathonone.sentiment_backend.dto.SentimentRequest;
import br.com.hackathonone.sentiment_backend.dto.SentimentResponse;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictRequest;
import br.com.hackathonone.sentiment_backend.dto.ds.DsPredictResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SentimentServiceTest {

    @Mock
    private SentimentDsClient dsClient;

    @InjectMocks
    private SentimentService service;

    @Test
    void deveAnalisarSentimentoComSucesso() {
        // 1. Arrange (Prepara o cenário)
        SentimentRequest request = new SentimentRequest();
        request.setText("Adorei o serviço");

        // Simulando a resposta do DS (Python)
        DsPredictResponse dsResponse = new DsPredictResponse();
        dsResponse.setLabel("Positivo");
        dsResponse.setProbability(0.95);

        // Diz ao Mockito: "Quando chamarem o predict, retorne dsResponse"
        Mockito.when(dsClient.predict(Mockito.any(DsPredictRequest.class))).thenReturn(dsResponse);

        // 2. Act (Executa a ação)
        SentimentResponse response = service.analyzeSentiment(request);

        // 3. Assert (Valida o resultado)
        Assertions.assertNotNull(response);
        Assertions.assertEquals("Positivo", response.getPrevisao());
        Assertions.assertEquals(0.95, response.getProbabilidade());

        // Verifica se o client foi chamado 1 vez
        Mockito.verify(dsClient, Mockito.times(1)).predict(Mockito.any(DsPredictRequest.class));
    }
}