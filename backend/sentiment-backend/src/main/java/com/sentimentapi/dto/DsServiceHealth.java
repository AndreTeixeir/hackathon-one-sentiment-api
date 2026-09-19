package com.sentimentapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para mapear a resposta de /health do microserviço de Data Science.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DsServiceHealth {

    @JsonProperty("status")
    private String status;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("model_loaded")
    private Boolean modelLoaded;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("fallback_reason")
    private String fallbackReason;

    @JsonProperty("classes")
    private List<String> classes;
}
