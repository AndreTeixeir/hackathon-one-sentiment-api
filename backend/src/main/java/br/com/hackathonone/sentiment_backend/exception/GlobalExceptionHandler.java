package br.com.hackathonone.sentiment_backend.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Tratamento para Erro de Validação (Ex: texto vazio, < 3 chars)
    // Ocorre quando o @Valid do Controller falha.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");

        // Pega a mensagem amigável que definimos no DTO (ex: "O texto deve ter no mínimo 3 caracteres")
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Erro de validação";

        body.put("message", message);
        body.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(body);
    }

    // 2. Tratamento para Erro de Comunicação com DS (Feign)
    // Ocorre quando o serviço Python está fora do ar ou dá erro 500.
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "External Service Error");
        body.put("message", "O serviço de Inteligência Artificial está indisponível no momento.");
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    // 3. Tratamento Genérico (Catch-all)
    // Para qualquer outro erro que não previmos (NullPointerException, etc).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "Ocorreu um erro inesperado no servidor.");
        body.put("path", request.getRequestURI());

        return ResponseEntity.internalServerError().body(body);
    }
}