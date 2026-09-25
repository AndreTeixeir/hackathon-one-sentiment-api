package com.sentimentapi.controller;

import com.sentimentapi.dto.DsServiceHealth;
import com.sentimentapi.service.DsServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para verificação de saúde da aplicação.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Endpoints para verificação de saúde")
public class HealthController {

    private final DsServiceClient dsServiceClient;

    /**
     * Verifica a saúde da aplicação e suas dependências.
     */
    @GetMapping("/health")
    @Operation(
            summary = "Verificar saúde da aplicação",
            description = "Retorna o status da aplicação e de suas dependências (DS Service)"
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "sentiment-backend");

        Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("ds-service", buildDsServiceStatus());
        health.put("dependencies", dependencies);

        return ResponseEntity.ok(health);
    }

    /**
     * Propaga o modo de operação do DS Service (modelo real ou fallback
     * heurístico) para quem consulta /api/v1/health — sem isso não há como
     * saber de fora se a API está classificando de verdade.
     */
    private Map<String, Object> buildDsServiceStatus() {
        Optional<DsServiceHealth> dsHealth = dsServiceClient.getHealth();
        Map<String, Object> status = new HashMap<>();

        if (dsHealth.isEmpty()) {
            status.put("status", "DOWN");
            return status;
        }

        DsServiceHealth h = dsHealth.get();
        status.put("status", "UP");
        status.put("mode", h.getMode());
        status.put("model_loaded", h.getModelLoaded());
        status.put("model_version", h.getModelVersion());
        status.put("fallback_reason", h.getFallbackReason());
        return status;
    }
}
