package com.sentimentapi.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.controller.SentimentController;
import com.sentimentapi.service.SentimentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Garante que as respostas de erro não carregam texto vindo de exceção
 * (endereço interno, credencial, detalhe de driver), mas que o detalhe
 * completo continua no log.
 */
@WebMvcTest(SentimentController.class)
class GlobalExceptionHandlerTest {

    private static final String MARCADOR = "http://10.99.99.99:8000/predict-marcador-interno";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SentimentService sentimentService;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void anexaAppender() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void removeAppender() {
        logger.detachAppender(logs);
        logs.stop();
    }

    private String corpoValido() throws Exception {
        return objectMapper.writeValueAsString(new SentimentRequest("Produto excelente, recomendo a todos"));
    }

    @Test
    @DisplayName("DsServiceException: a resposta 503 não contém o texto da exceção e o detalhe fica no log")
    void dsServiceExceptionNaoVazaDetalhe() throws Exception {
        when(sentimentService.analisar(any(SentimentRequest.class)))
                .thenThrow(new DsServiceException(
                        "Falha na comunicação com o serviço de ML: I/O error on POST request for \""
                                + MARCADOR + "\": Connection refused",
                        new RuntimeException("causa com " + MARCADOR)));

        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Serviço Indisponível"))
                .andExpect(jsonPath("$.message").value("Erro ao comunicar com o serviço de análise de sentimento"))
                .andExpect(jsonPath("$.path").value("/api/v1/sentiment"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString(MARCADOR))));

        assertThat(logs.list)
                .anySatisfy(evento -> {
                    assertThat(evento.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(evento.getFormattedMessage()).contains(MARCADOR);
                    assertThat(evento.getThrowableProxy()).isNotNull();
                });
    }

    @Test
    @DisplayName("Exceção genérica: a resposta 500 não contém o texto da exceção e o detalhe fica no log")
    void excecaoGenericaNaoVazaDetalhe() throws Exception {
        when(sentimentService.analisar(any(SentimentRequest.class)))
                .thenThrow(new IllegalStateException("could not connect to " + MARCADOR + " password=segredo"));

        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Erro Interno"))
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado no servidor"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(content().string(not(containsString(MARCADOR))))
                .andExpect(content().string(not(containsString("segredo"))));

        assertThat(logs.list)
                .anySatisfy(evento -> {
                    assertThat(evento.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(evento.getFormattedMessage()).contains(MARCADOR);
                    assertThat(evento.getThrowableProxy()).isNotNull();
                });
    }

    @Test
    @DisplayName("Validação de campo continua devolvendo a mensagem do campo, escrita para o usuário")
    void validacaoContinuaComMensagemDoCampo() throws Exception {
        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SentimentRequest("ab"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erro de Validação"))
                .andExpect(jsonPath("$.message").value("Os dados enviados são inválidos"))
                .andExpect(jsonPath("$.details[0]").value("O campo 'text' deve ter entre 3 e 2000 caracteres"));
    }
}
