package com.sentimentapi.exception;

import com.sentimentapi.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler global para tratamento de exceções da API.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Erro de Validação")
                .message("Os dados enviados são inválidos")
                .path(request.getRequestURI())
                .details(details)
                .build();

        log.warn("Erro de validação: {}", details);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DsServiceException.class)
    public ResponseEntity<ErrorResponse> handleDsServiceException(
            DsServiceException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Serviço Indisponível")
                .message("Erro ao comunicar com o serviço de análise de sentimento")
                .path(request.getRequestURI())
                .details(List.of())
                .build();

        // A mensagem da exceção pode conter o endereço interno do ds-service:
        // o detalhe fica só no log, nunca na resposta.
        log.error("Erro no DS Service: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Erro Interno")
                .message("Ocorreu um erro inesperado no servidor")
                .path(request.getRequestURI())
                .details(List.of())
                .build();

        // Idem: o detalhe fica só no log.
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(error);
    }
}
