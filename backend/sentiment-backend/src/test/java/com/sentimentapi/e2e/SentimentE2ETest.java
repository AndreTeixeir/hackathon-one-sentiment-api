package com.sentimentapi.e2e;

import com.sentimentapi.dto.request.SentimentRequest;
import com.sentimentapi.dto.response.SentimentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E de verdade: PostgreSQL real e ds-service real (buildado a partir do
 * Dockerfile do repositório, exatamente como em produção), nenhum mock.
 *
 * Este é o teste que teria pego o bug original: o ds-service nunca carregava
 * o modelo treinado porque o artefato não estava no controle de versão, e
 * o /predict sempre devolvia as probabilidades fixas do fallback (0.75/0.85).
 * Um mock de DsServiceClient nunca exercitaria essa falha — só um contêiner
 * construído de verdade a partir do Dockerfile do repositório expõe se o
 * COPY do artefato está funcionando.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SentimentE2ETest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sentimentdb_e2e")
            .withUsername("sentiment_user")
            .withPassword("sentiment_pass");

    @Container
    static final GenericContainer<?> DS_SERVICE = new GenericContainer<>(
            new ImageFromDockerfile("ds-service-e2e", false)
                    .withDockerfile(Path.of("../../ds-service/Dockerfile").toAbsolutePath().normalize()))
            .withExposedPorts(8000)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // O perfil "dev" (default de application.yml) fixa driver-class-name
        // para H2 — sem sobrescrever aqui, Hibernate tenta abrir a URL do
        // Postgres do container com o driver do H2.
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("ds.service.url",
                () -> "http://" + DS_SERVICE.getHost() + ":" + DS_SERVICE.getMappedPort(8000));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Classifica com o modelo real (Postgres + ds-service reais) e probabilidade nunca é 0.75/0.85")
    void classificaComModeloRealEProbabilidadeVariavel() {
        SentimentRequest positivo = new SentimentRequest("Atendimento excelente, resolveu meu problema rapidamente!");
        SentimentRequest negativo = new SentimentRequest("Demorou muito e veio com defeito. Péssima experiência.");

        ResponseEntity<SentimentResponse> respPositivo =
                restTemplate.postForEntity("/api/v1/sentiment", positivo, SentimentResponse.class);
        ResponseEntity<SentimentResponse> respNegativo =
                restTemplate.postForEntity("/api/v1/sentiment", negativo, SentimentResponse.class);

        assertThat(respPositivo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respNegativo.getStatusCode()).isEqualTo(HttpStatus.OK);

        SentimentResponse bodyPositivo = respPositivo.getBody();
        SentimentResponse bodyNegativo = respNegativo.getBody();
        assertThat(bodyPositivo).isNotNull();
        assertThat(bodyNegativo).isNotNull();

        assertThat(bodyPositivo.getPrevisao()).isEqualTo("Positivo");
        assertThat(bodyNegativo.getPrevisao()).isEqualTo("Negativo");

        // A asserção central: a assinatura do bug original é a API sempre devolver
        // exatamente 0.75 (Positivo) ou 0.85 (Negativo) — os valores fixos do
        // fallback heurístico. Com o modelo real carregado, isso nunca acontece.
        assertThat(bodyPositivo.getProbabilidade()).isNotIn(0.75, 0.85);
        assertThat(bodyNegativo.getProbabilidade()).isNotIn(0.75, 0.85);
    }

    @Test
    @DisplayName("GET /api/v1/health propaga mode=model do ds-service real")
    void healthPropagaModeReal() {
        @SuppressWarnings("rawtypes")
        ResponseEntity<java.util.Map> response = restTemplate.getForEntity("/api/v1/health", java.util.Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        var dependencies = (java.util.Map<String, Object>) response.getBody().get("dependencies");
        @SuppressWarnings("unchecked")
        var dsService = (java.util.Map<String, Object>) dependencies.get("ds-service");

        assertThat(dsService.get("status")).isEqualTo("UP");
        assertThat(dsService.get("mode")).isEqualTo("model");
        assertThat(dsService.get("model_loaded")).isEqualTo(true);
    }
}
