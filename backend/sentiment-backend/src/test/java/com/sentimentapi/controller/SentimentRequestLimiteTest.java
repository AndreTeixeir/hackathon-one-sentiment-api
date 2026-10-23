package com.sentimentapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimentapi.dto.request.BatchSentimentRequest;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.dto.response.BatchSentimentResponse;
import com.sentimentapi.dto.response.SentimentResponse;
import com.sentimentapi.service.SentimentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Limite de 2.000 caracteres do campo {@code text}, no envio simples e em
 * cada item do lote.
 */
@WebMvcTest(SentimentController.class)
class SentimentRequestLimiteTest {

    private static final String MENSAGEM_DO_CAMPO = "O campo 'text' deve ter entre 3 e 2000 caracteres";

    private static final String TEXTO_NO_LIMITE = "a".repeat(2000);
    private static final String TEXTO_ACIMA_DO_LIMITE = "a".repeat(2001);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SentimentService sentimentService;

    @Test
    @DisplayName("Envio simples: texto de 2.000 caracteres é aceito")
    void envioSimplesAceitaTextoNoLimite() throws Exception {
        when(sentimentService.analisar(any(SentimentRequest.class)))
                .thenReturn(SentimentResponse.builder().previsao("Positivo").probabilidade(0.9).build());

        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SentimentRequest(TEXTO_NO_LIMITE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previsao").value("Positivo"));
    }

    @Test
    @DisplayName("Envio simples: texto de 2.001 caracteres é recusado com 400 e a mensagem do campo")
    void envioSimplesRecusaTextoAcimaDoLimite() throws Exception {
        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SentimentRequest(TEXTO_ACIMA_DO_LIMITE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Validação"))
                .andExpect(jsonPath("$.details[0]").value(MENSAGEM_DO_CAMPO));

        verify(sentimentService, never()).analisar(any(SentimentRequest.class));
    }

    @Test
    @DisplayName("Lote: item de 2.000 caracteres é aceito")
    void loteAceitaItemNoLimite() throws Exception {
        when(sentimentService.analisarBatch(any(BatchSentimentRequest.class)))
                .thenReturn(BatchSentimentResponse.builder().batchId("lote-teste").total(2).build());

        mockMvc.perform(post("/api/v1/sentiment/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loteDe("Texto curto e válido", TEXTO_NO_LIMITE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch_id").value("lote-teste"));
    }

    @Test
    @DisplayName("Lote: um item de 2.001 caracteres recusa o lote com 400 e a mensagem do campo")
    void loteRecusaItemAcimaDoLimite() throws Exception {
        mockMvc.perform(post("/api/v1/sentiment/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                loteDe("Texto curto e válido", TEXTO_ACIMA_DO_LIMITE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Validação"))
                .andExpect(jsonPath("$.details[0]").value(MENSAGEM_DO_CAMPO));

        verify(sentimentService, never()).analisarBatch(any(BatchSentimentRequest.class));
    }

    private static BatchSentimentRequest loteDe(String... textos) {
        return BatchSentimentRequest.builder()
                .texts(Arrays.stream(textos).map(SentimentRequest::new).toList())
                .build();
    }
}
