package com.sentimentapi.domain.enums;

/**
 * Enum representando os tipos de sentimento classificados pelo modelo de ML.
 *
 * O modelo é binário (ver ADR-004) — não existe classe Neutro. Um rótulo
 * não reconhecido é um erro de integração (mudança de vocabulário do
 * DS Service, por exemplo) e deve falhar de forma visível, não ser
 * absorvido silenciosamente por um valor default.
 */
public enum Sentimento {
    POSITIVO("Positivo"),
    NEGATIVO("Negativo");

    private final String label;

    Sentimento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Converte uma string de label para o enum correspondente.
     * Aceita variações como "Positivo", "POSITIVO", "POSITIVE", etc.
     *
     * @throws IllegalArgumentException se o label for nulo, vazio ou não reconhecido
     */
    public static Sentimento fromLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Label de sentimento ausente");
        }

        String normalized = label.trim().toUpperCase();

        return switch (normalized) {
            case "POSITIVO", "POSITIVE", "POS", "1" -> POSITIVO;
            case "NEGATIVO", "NEGATIVE", "NEG", "0" -> NEGATIVO;
            default -> throw new IllegalArgumentException("Rótulo de sentimento desconhecido: '" + label + "'");
        };
    }
}
