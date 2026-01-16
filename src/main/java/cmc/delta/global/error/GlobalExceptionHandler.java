package cmc.delta.global.error;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.error.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String DATA_KEY_SUPPORTED_METHODS = "supportedMethods";

	private final ErrorLogWriter errorLogWriter;
	private final ErrorResponseFactory errorResponseFactory;

	public GlobalExceptionHandler(
		ErrorLogWriter errorLogWriter, ErrorResponseFactory errorResponseFactory) {
		this.errorLogWriter = errorLogWriter;
		this.errorResponseFactory = errorResponseFactory;
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusiness(
		BusinessException ex, HttpServletRequest request) {
		ErrorCode ec = ex.getErrorCode();
		boolean hideDetail = shouldHideDetail(ec);
		String clientMessage = hideDetail ? ec.defaultMessage() : ex.getMessage();
		Object clientData = hideDetail ? null : ex.getData();

		ApiResponse<Object> body = errorResponseFactory.create(ec, clientMessage, clientData);
		return logAndRespond(ec, ex, request, body);
	}


	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleValidation(
		MethodArgumentNotValidException ex, HttpServletRequest request) {
		ErrorCode ec = ErrorCode.INVALID_REQUEST;
		ApiResponse<Object> body = errorResponseFactory.validationError(ec, ex);
		return logAndRespond(ec, ex, request, body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
		ConstraintViolationException ex, HttpServletRequest request) {
		ErrorCode ec = ErrorCode.INVALID_REQUEST;
		ApiResponse<Object> body = errorResponseFactory.constraintViolationError(ec, ex);
		return logAndRespond(ec, ex, request, body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Object>> handleNotReadable(
		HttpMessageNotReadableException ex, HttpServletRequest request) {
		ErrorCode ec = ErrorCode.INVALID_REQUEST;
		ApiResponse<Object> body = errorResponseFactory.notReadableBody(ec);
		return logAndRespond(ec, ex, request, body);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Object>> handleMethodNotAllowed(
		HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		ErrorCode ec = ErrorCode.METHOD_NOT_ALLOWED;

		ApiResponse<Object> body = errorResponseFactory.create(
			ec,
			ec.defaultMessage(),
			Map.of(DATA_KEY_SUPPORTED_METHODS, ex.getSupportedHttpMethods()));
		return logAndRespond(ec, ex, request, body);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
		NoResourceFoundException ex, HttpServletRequest request) {
		ErrorCode ec = ErrorCode.RESOURCE_NOT_FOUND;
		ApiResponse<Object> body = errorResponseFactory.defaultError(ec);
		return logAndRespond(ec, ex, request, body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleUnknown(
		Exception ex, HttpServletRequest request) {
		ErrorCode ec = ErrorCode.INTERNAL_ERROR;
		ApiResponse<Object> body = errorResponseFactory.defaultError(ec);
		return logAndRespond(ec, ex, request, body);
	}

	private boolean shouldHideDetail(ErrorCode ec) {
		return ec.status().is5xxServerError();
	}

	private ResponseEntity<ApiResponse<Object>> logAndRespond(
		ErrorCode errorCode,
		Exception exception,
		HttpServletRequest request,
		ApiResponse<Object> body) {
		errorLogWriter.write(errorCode, exception, request);
		return ResponseEntity.status(errorCode.status()).body(body);
	}
}
