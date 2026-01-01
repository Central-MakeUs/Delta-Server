package cmc.delta.global.error;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.error.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode ec = ex.getErrorCode();
        logByLevel(ec, ex, request);

        int status = ec.status().value();
        ApiResponse<Object> body = ApiResponses.fail(status, ec.code(), ex.getData(), ex.getMessage());
        return ResponseEntity.status(ec.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        logByLevel(ec, ex, request);

        Map<String, String> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                FieldError::getField,
                                fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                                (a, b) -> a
                        ));

        int status = ec.status().value();
        ApiResponse<Object> body = ApiResponses.fail(
                status,
                ec.code(),
                Map.of("fieldErrors", fieldErrors),
                ec.defaultMessage()
        );
        return ResponseEntity.status(ec.status()).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        logByLevel(ec, ex, request);

        Map<String, String> violations =
                ex.getConstraintViolations().stream()
                        .collect(Collectors.toMap(
                                v -> v.getPropertyPath().toString(),
                                v -> v.getMessage(),
                                (a, b) -> a
                        ));

        int status = ec.status().value();
        ApiResponse<Object> body = ApiResponses.fail(
                status,
                ec.code(),
                Map.of("violations", violations),
                ec.defaultMessage()
        );
        return ResponseEntity.status(ec.status()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorCode ec = ErrorCode.INVALID_REQUEST;
        logByLevel(ec, ex, request);

        int status = ec.status().value();
        ApiResponse<Object> body = ApiResponses.fail(status, ec.code(), null, "요청 본문(JSON)을 확인해주세요.");
        return ResponseEntity.status(ec.status()).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorCode ec = ErrorCode.METHOD_NOT_ALLOWED;
        logByLevel(ec, ex, request);

        int status = ec.status().value();
        ApiResponse<Object> body = ApiResponses.fail(
                status,
                ec.code(),
                Map.of("supportedMethods", ex.getSupportedHttpMethods()),
                ec.defaultMessage()
        );
        return ResponseEntity.status(ec.status()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex, HttpServletRequest request) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        logByLevel(ec, ex, request);

        int status = ec.status().value();
        ApiResponse<Object> body = ApiResponses.fail(status, ec.code(), null, ec.defaultMessage());
        return ResponseEntity.status(ec.status()).body(body);
    }

    private void logByLevel(ErrorCode ec, Exception ex, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        switch (ec.logLevel()) {
            case TRACE -> log.trace("[{} {}] code={} msg={}", method, path, ec.code(), ex.getMessage(), ex);
            case DEBUG -> log.debug("[{} {}] code={} msg={}", method, path, ec.code(), ex.getMessage(), ex);
            case INFO  -> log.info ("[{} {}] code={} msg={}", method, path, ec.code(), ex.getMessage());
            case WARN  -> log.warn ("[{} {}] code={} msg={}", method, path, ec.code(), ex.getMessage());
            case ERROR -> log.error("[{} {}] code={} msg={}", method, path, ec.code(), ex.getMessage(), ex);
        }
    }
}
