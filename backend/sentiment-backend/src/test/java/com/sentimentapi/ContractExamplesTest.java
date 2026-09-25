package com.sentimentapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentimentapi.dto.DsServiceResponse;
import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.service.DsServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de contrato: consome os payloads reais de contracts/examples/,
 * o único ponto do repositório que os define, e que nenhum teste lia até
 * esta frente. Dois níveis de verificação por arquivo:
 * 1. O JSON deserializa como SentimentRequest com texto não vazio — se o
 *    campo mudar de nome no DTO, isto falha (o campo viria null).
 * 2. O mesmo payload, batendo em /api/v1/sentiment de verdade, devolve o
 *    rótulo que o nome do arquivo promete.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ContractExamplesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DsServiceClient dsServiceClient;

    @Test
    @DisplayName("contracts/examples/positive.json é um SentimentRequest válido e classifica como Positivo")
    void positiveExampleContract() throws Exception {
        assertExampleContract("positive.json", "Positivo", 0.93);
    }

    @Test
    @DisplayName("contracts/examples/negative.json é um SentimentRequest válido e classifica como Negativo")
    void negativeExampleContract() throws Exception {
        assertExampleContract("negative.json", "Negativo", 0.87);
    }

    private void assertExampleContract(String filename, String expectedLabel, double mockedProbability) throws Exception {
        String json = Files.readString(contractsExamplesDir().resolve(filename));

        SentimentRequest request = objectMapper.readValue(json, SentimentRequest.class);
        assertThat(request.getText()).isNotBlank();

        when(dsServiceClient.predict(anyString()))
                .thenReturn(new DsServiceResponse(expectedLabel, mockedProbability));

        mockMvc.perform(post("/api/v1/sentiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previsao").value(expectedLabel));
    }

    /**
     * contracts/examples/ vive na raiz do repositório, fora do módulo Maven —
     * sobe a partir do diretório de trabalho até encontrar a pasta, em vez de
     * assumir um relativo fixo (que quebraria dependendo de onde `mvn test`
     * é invocado).
     */
    private static Path contractsExamplesDir() throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("contracts/examples");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IOException("contracts/examples não encontrado subindo a partir de "
                + Paths.get("").toAbsolutePath());
    }
}
