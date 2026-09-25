package com.sentimentapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimentapi.dto.request.BatchSentimentRequest;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.dto.response.BatchSentimentResponse;
import com.sentimentapi.dto.response.SentimentResponse;
import com.sentimentapi.dto.response.StatsResponse;
import com.sentimentapi.service.DsServiceClient;
import com.sentimentapi.dto.DsServiceHealth;
import com.sentimentapi.dto.DsServiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para a API de Sentimento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SentimentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DsServiceClient dsServiceClient;

    @Test
    @DisplayName("Fluxo completo: análise de sentimento e verificação de estatísticas")
    void fluxoCompletoAnaliseEStats() throws Exception {
        // Mock do DS Service
        when(dsServiceClient.predict(anyString()))
                .thenReturn(new DsServiceResponse("Positivo", 0.85));

        // 1. Enviar análise de sentimento
        SentimentRequest request = new SentimentRequest("Produto muito bom, entrega rápida!");

        MvcResult result = mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        SentimentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                SentimentResponse.class
        );

        assertThat(response.getPrevisao()).isEqualTo("Positivo");
        assertThat(response.getProbabilidade()).isEqualTo(0.85);

        // 2. Verificar estatísticas
        MvcResult statsResult = mockMvc.perform(get("/api/v1/stats"))
                .andExpect(status().isOk())
                .andReturn();

        StatsResponse stats = objectMapper.readValue(
                statsResult.getResponse().getContentAsString(),
                StatsResponse.class
        );

        assertThat(stats.getTotalAnalises()).isGreaterThanOrEqualTo(1);
        assertThat(stats.getPositivos()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Deve retornar 503 quando o DS Service devolve rótulo de sentimento desconhecido")
    void deveRetornar503ParaRotuloDesconhecido() throws Exception {
        // Rótulo fora do vocabulário que Sentimento.fromLabel reconhece — é uma
        // violação de contrato do DS Service, não um erro interno do backend,
        // então a resposta esperada é 503 (Serviço Indisponível), não 500.
        when(dsServiceClient.predict(anyString()))
                .thenReturn(new DsServiceResponse("Desconhecido", 0.5));

        SentimentRequest request = new SentimentRequest("Texto qualquer, suficientemente longo");

        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Serviço Indisponível"));
    }

    @Test
    @DisplayName("Batch processing: múltiplos textos, incluindo rótulo desconhecido do DS Service")
    void batchProcessingMultiplosTextos() throws Exception {
        // Mock do DS Service para diferentes respostas. O terceiro item simula
        // um rótulo que o DS Service pode devolver mas que Sentimento.fromLabel
        // não reconhece — antes da remoção do Neutro, isso virava "Neutro" em
        // silêncio (default do enum); agora deve aparecer como erro visível.
        when(dsServiceClient.predict("Excelente produto!"))
                .thenReturn(new DsServiceResponse("Positivo", 0.95));
        when(dsServiceClient.predict("Produto ruim, não recomendo"))
                .thenReturn(new DsServiceResponse("Negativo", 0.88));
        when(dsServiceClient.predict("Produto com rótulo desconhecido"))
                .thenReturn(new DsServiceResponse("Desconhecido", 0.50));

        BatchSentimentRequest batchRequest = BatchSentimentRequest.builder()
                .texts(List.of(
                        new SentimentRequest("Excelente produto!"),
                        new SentimentRequest("Produto ruim, não recomendo"),
                        new SentimentRequest("Produto com rótulo desconhecido")
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/sentiment/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        BatchSentimentResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BatchSentimentResponse.class
        );

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getBatchId()).isNotNull();
        assertThat(response.getResultados()).hasSize(3);

        List<BatchSentimentResponse.BatchItemResponse> resultados = response.getResultados();
        assertThat(resultados.get(0).getPrevisao()).isEqualTo("Positivo");
        assertThat(resultados.get(0).getProbabilidade()).isEqualTo(0.95);
        assertThat(resultados.get(1).getPrevisao()).isEqualTo("Negativo");
        assertThat(resultados.get(1).getProbabilidade()).isEqualTo(0.88);
        assertThat(resultados.get(2).getPrevisao()).isEqualTo("ERRO");
        assertThat(resultados.get(2).getProbabilidade()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Health check deve retornar status UP e propagar o modo do DS Service")
    void healthCheckDeveRetornarStatusUp() throws Exception {
        when(dsServiceClient.getHealth()).thenReturn(Optional.of(
                new DsServiceHealth("ok", "model", true, "abc123", null, List.of("Negativo", "Positivo"))
        ));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependencies['ds-service'].status").value("UP"))
                .andExpect(jsonPath("$.dependencies['ds-service'].mode").value("model"))
                .andExpect(jsonPath("$.dependencies['ds-service'].model_loaded").value(true));
    }

    @Test
    @DisplayName("Health check deve reportar DS Service indisponível quando ele não responde")
    void healthCheckDeveReportarDsServiceIndisponivel() throws Exception {
        when(dsServiceClient.getHealth()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependencies['ds-service'].status").value("DOWN"));
    }
}
