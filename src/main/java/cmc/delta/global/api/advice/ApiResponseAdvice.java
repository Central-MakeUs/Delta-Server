package cmc.delta.global.api.advice;

import cmc.delta.global.api.annotation.NoWrap;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

	private static final int DEFAULT_HTTP_STATUS = 200;

	private static final MediaType JSON = MediaType.APPLICATION_JSON;

	private static final String JSON_SUBTYPE = MediaType.APPLICATION_JSON.getSubtype();
	private static final String JSON_SUFFIX = "+" + JSON_SUBTYPE;

	private static final String[] NO_WRAP_PATH_PREFIXES = {
		"/v3/api-docs", "/swagger-ui", "/swagger-resources", "/webjars"
	};

	private final ObjectMapper objectMapper;

	public ApiResponseAdvice(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean supports(
		MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		if (returnType.getContainingClass().isAnnotationPresent(NoWrap.class))
			return false;
		if (Objects.requireNonNull(returnType.getMethod()).isAnnotationPresent(NoWrap.class))
			return false;

		return !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public Object beforeBodyWrite(
		Object body,
		MethodParameter returnType,
		MediaType selectedContentType,
		Class<? extends HttpMessageConverter<?>> selectedConverterType,
		ServerHttpRequest request,
		ServerHttpResponse response) {

		if (!shouldWrap(body, selectedContentType, request)) {
			return body;
		}

		ApiResponse<Object> wrapped = ApiResponses.success(resolveHttpStatus(response), body);
		if (body instanceof String) {
			return serializeForStringConverter(wrapped, body, request, response);
		}
		return wrapped;
	}

	private boolean shouldWrap(Object body, MediaType selectedContentType, ServerHttpRequest request) {
		if (shouldSkipWrap(request)) {
			return false;
		}
		if (!isJsonLike(selectedContentType) && !(body instanceof String)) {
			return false;
		}
		return !(body instanceof ApiResponse<?>);
	}

	/**
	 * String 반환 컨트롤러는 StringHttpMessageConverter가 처리하므로
	 * 래퍼 객체를 그대로 반환하면 직렬화가 깨진다. 여기서 직접 JSON 문자열로 만들어 돌려준다.
	 */
	private Object serializeForStringConverter(
		ApiResponse<Object> wrapped, Object originalBody, ServerHttpRequest request, ServerHttpResponse response) {
		try {
			response.getHeaders().setContentType(JSON);
			return objectMapper.writeValueAsString(wrapped);
		} catch (Exception e) {
			log.warn("ApiResponse 문자열 응답 래핑 실패. 원본 그대로 반환 path={}", request.getURI().getPath(), e);
			return originalBody;
		}
	}

	private boolean shouldSkipWrap(ServerHttpRequest request) {
		String path = request.getURI().getPath();
		for (String prefix : NO_WRAP_PATH_PREFIXES) {
			if (path.startsWith(prefix))
				return true;
		}
		return false;
	}

	private boolean isJsonLike(MediaType mediaType) {
		if (mediaType == null)
			return false;

		if (JSON.includes(mediaType))
			return true;

		String subtype = mediaType.getSubtype();
		if (subtype == null || subtype.isBlank())
			return false;

		String lower = subtype.toLowerCase();
		return lower.equals(JSON_SUBTYPE) || lower.endsWith(JSON_SUFFIX);
	}

	private int resolveHttpStatus(ServerHttpResponse response) {
		if (response instanceof ServletServerHttpResponse servletResponse) {
			return servletResponse.getServletResponse().getStatus();
		}
		return DEFAULT_HTTP_STATUS;
	}
}
