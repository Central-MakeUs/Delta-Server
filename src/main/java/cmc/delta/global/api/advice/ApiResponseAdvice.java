package cmc.delta.global.api.advice;

import cmc.delta.global.api.annotation.NoWrap;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Objects;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final int DEFAULT_HTTP_STATUS = 200;

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private static final String JSON_SUBTYPE = MediaType.APPLICATION_JSON.getSubtype();
    private static final String JSON_SUFFIX = "+" + JSON_SUBTYPE;

    private static final String[] NO_WRAP_PATH_PREFIXES = {
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars"
    };

    private final ObjectMapper objectMapper;

    public ApiResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.getContainingClass().isAnnotationPresent(NoWrap.class)) return false;
        if (Objects.requireNonNull(returnType.getMethod()).isAnnotationPresent(NoWrap.class)) return false;

        return !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        if (shouldSkipWrap(request)) return body;

        if (!isJsonLike(selectedContentType) && !(body instanceof String)) return body;

        if (body instanceof ApiResponse<?>) return body;

        int status = resolveHttpStatus(response);
        ApiResponse<Object> wrapped = ApiResponses.success(status, body);
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(JSON);
                return objectMapper.writeValueAsString(wrapped);
            } catch (Exception e) {
                return body;
            }
        }

        return wrapped;
    }

    private boolean shouldSkipWrap(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        for (String prefix : NO_WRAP_PATH_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private boolean isJsonLike(MediaType mediaType) {
        if (mediaType == null) return false;

        if (JSON.includes(mediaType)) return true;

        String subtype = mediaType.getSubtype();
        if (subtype == null || subtype.isBlank()) return false;

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
