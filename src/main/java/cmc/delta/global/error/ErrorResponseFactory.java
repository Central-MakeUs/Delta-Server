package cmc.delta.global.error;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import jakarta.validation.ConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ErrorResponseFactory {

    private static final String DATA_KEY_FIELD_ERRORS = "fieldErrors";
    private static final String DATA_KEY_VIOLATIONS = "violations";

    private static final String FALLBACK_INVALID_MESSAGE = "invalid";
    private static final String MESSAGE_NOT_READABLE_BODY = "요청 본문(JSON)을 확인해주세요.";

    public ApiResponse<Object> create(ErrorCode errorCode, String message, Object data) {
        return ApiResponses.fail(errorCode.status().value(), errorCode.code(), data, message);
    }

    public ApiResponse<Object> defaultError(ErrorCode errorCode) {
        return create(errorCode, errorCode.defaultMessage(), null);
    }

    public ApiResponse<Object> errorWithMessage(ErrorCode errorCode, String message) {
        return create(errorCode, message, null);
    }

    public ApiResponse<Object> notReadableBody(ErrorCode errorCode) {
        return create(errorCode, MESSAGE_NOT_READABLE_BODY, null);
    }

    public ApiResponse<Object> validationError(ErrorCode errorCode, MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = extractFieldErrors(ex);
        return create(errorCode, errorCode.defaultMessage(), Map.of(DATA_KEY_FIELD_ERRORS, fieldErrors));
    }

    public ApiResponse<Object> constraintViolationError(ErrorCode errorCode, ConstraintViolationException ex) {
        Map<String, String> violations = extractViolations(ex);
        return create(errorCode, errorCode.defaultMessage(), Map.of(DATA_KEY_VIOLATIONS, violations));
    }

    private Map<String, String> extractFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        this::resolveFieldErrorMessage,
                        (a, b) -> a
                ));
    }

    private String resolveFieldErrorMessage(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        return (message == null || message.isBlank()) ? FALLBACK_INVALID_MESSAGE : message;
    }

    private Map<String, String> extractViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a
                ));
    }
}
