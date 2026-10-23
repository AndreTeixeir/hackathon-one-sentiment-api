package com.sentimentapi.config;

import com.sentimentapi.dto.DsServiceResponse;
import com.sentimentapi.service.DsServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do CORS restrito. Usam o contexto completo e o {@code application.yml}
 * real, então também provam que a lista de origens da configuração é lida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CorsConfigTest {

    private static final String ORIGEM_DEMO = "https://sentiment.andreteixeira.dev.br";
    private static final String ORIGEM_LOCAL = "http://localhost:8080";
    private static final String ORIGEM_NAO_LISTADA = "https://exemplo.com";

    private static final String CORPO = "{\"text\":\"Produto excelente, recomendo a todos\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DsServiceClient dsServiceClient;

    @ParameterizedTest(name = "preflight com origem listada ({0}) é aceito")
    @ValueSource(strings = {ORIGEM_DEMO, ORIGEM_LOCAL})
    @DisplayName("Preflight OPTIONS com origem listada recebe Access-Control-Allow-Origin com a origem")
    void preflightComOrigemListadaEhAceito(String origem) throws Exception {
        mockMvc.perform(options("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, origem)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origem))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsStringIgnoringCase("Content-Type")))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @ParameterizedTest(name = "POST com origem listada ({0}) recebe o cabeçalho")
    @ValueSource(strings = {ORIGEM_DEMO, ORIGEM_LOCAL})
    @DisplayName("POST com origem listada passa e recebe Access-Control-Allow-Origin com a origem")
    void postComOrigemListadaEhAceito(String origem) throws Exception {
        when(dsServiceClient.predict(anyString())).thenReturn(new DsServiceResponse("Positivo", 0.9));

        mockMvc.perform(post("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, origem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origem))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("Preflight com origem não listada é recusado com 403 e sem Access-Control-Allow-Origin")
    void preflightComOrigemNaoListadaEhRecusado() throws Exception {
        mockMvc.perform(options("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, ORIGEM_NAO_LISTADA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("POST com origem não listada é recusado com 403, sem o cabeçalho e sem chegar ao ds-service")
    void postComOrigemNaoListadaEhRecusado() throws Exception {
        mockMvc.perform(post("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, ORIGEM_NAO_LISTADA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        verify(dsServiceClient, never()).predict(anyString());
    }

    @Test
    @DisplayName("Requisição sem Origin (curl) segue funcionando e não recebe cabeçalhos de CORS")
    void requisicaoSemOriginSegueFuncionando() throws Exception {
        when(dsServiceClient.predict(anyString())).thenReturn(new DsServiceResponse("Positivo", 0.9));

        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Origin igual ao host da requisição (acesso direto pelo IP) não é CORS e segue funcionando")
    void origemIgualAoHostDaRequisicaoSegueFuncionando() throws Exception {
        when(dsServiceClient.predict(anyString())).thenReturn(new DsServiceResponse("Positivo", 0.9));

        mockMvc.perform(post("/api/v1/sentiment")
                        .with(request -> {
                            request.setScheme("http");
                            request.setServerName("152.67.61.11");
                            request.setServerPort(8080);
                            return request;
                        })
                        .header(HttpHeaders.ORIGIN, "http://152.67.61.11:8080")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Preflight com método que a API não usa é recusado mesmo com origem listada")
    void preflightComMetodoNaoUsadoEhRecusado() throws Exception {
        mockMvc.perform(options("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEMO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Preflight com cabeçalho fora da lista (Authorization) é recusado mesmo com origem listada")
    void preflightComCabecalhoForaDaListaEhRecusado() throws Exception {
        mockMvc.perform(options("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEMO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("A recusa não vaza a lista de origens permitidas")
    void recusaNaoVazaListaDeOrigens() throws Exception {
        mockMvc.perform(options("/api/v1/sentiment")
                        .header(HttpHeaders.ORIGIN, ORIGEM_NAO_LISTADA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString(ORIGEM_DEMO))));
    }
}
