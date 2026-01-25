package cmc.delta.global.api.advice;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.global.api.annotation.NoWrap;
import cmc.delta.global.api.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiResponseAdviceTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final ApiResponseAdvice advice = new ApiResponseAdvice(objectMapper);

	@Test
	@DisplayName("supports: ApiResponse를 반환하면 wrapping 대상이 아니다")
	void supports_whenReturnTypeIsApiResponse_thenFalse() throws Exception {
		// given
		MethodParameter returnType = returnType(TestController.class, "alreadyWrapped");

		// when
		boolean supports = advice.supports(returnType, null);

		// then
		assertThat(supports).isFalse();
	}

	@Test
	@DisplayName("supports: @NoWrap이 있으면 wrapping 대상이 아니다")
	void supports_whenAnnotatedNoWrap_thenFalse() throws Exception {
		// given
		MethodParameter methodNoWrap = returnType(TestController.class, "methodNoWrap");
		MethodParameter typeNoWrap = returnType(TypeNoWrapController.class, "ok");

		// when
		boolean supportsMethod = advice.supports(methodNoWrap, null);
		boolean supportsType = advice.supports(typeNoWrap, null);

		// then
		assertThat(supportsMethod).isFalse();
		assertThat(supportsType).isFalse();
	}

	@Test
	@DisplayName("beforeBodyWrite: JSON 응답이면 ApiResponse로 감싼다")
	void beforeBodyWrite_whenJson_thenWraps() throws Exception {
		// given
		Map<String, Integer> body = Map.of("a", 1);
		MethodParameter returnType = returnType(TestController.class, "ok");
		MockHttpServletRequest servletReq = new MockHttpServletRequest("GET", "/api/v1/test");
		ServerHttpRequest request = new ServletServerHttpRequest(servletReq);

		MockHttpServletResponse servlet = new MockHttpServletResponse();
		servlet.setStatus(201);
		ServerHttpResponse response = new ServletServerHttpResponse(servlet);

		// when
		Object out = advice.beforeBodyWrite(
			body,
			returnType,
			MediaType.APPLICATION_JSON,
			null,
			request,
			response);

		// then
		assertThat(out).isInstanceOf(ApiResponse.class);
		ApiResponse<?> wrapped = (ApiResponse<?>)out;
		assertThat(wrapped.status()).isEqualTo(201);
		assertThat(wrapped.code()).isEqualTo("S_201");
		assertThat(wrapped.data()).isEqualTo(body);
	}

	@Test
	@DisplayName("beforeBodyWrite: String body는 JSON string으로 감싼다")
	void beforeBodyWrite_whenStringBody_thenReturnsJsonString() throws Exception {
		// given
		String body = "ok";
		MethodParameter returnType = returnType(TestController.class, "ok");
		MockHttpServletRequest servletReq = new MockHttpServletRequest("GET", "/api/v1/test");
		ServerHttpRequest request = new ServletServerHttpRequest(servletReq);

		MockHttpServletResponse servlet = new MockHttpServletResponse();
		servlet.setStatus(200);
		ServletServerHttpResponse response = new ServletServerHttpResponse(servlet);

		// when
		Object out = advice.beforeBodyWrite(
			body,
			returnType,
			MediaType.TEXT_PLAIN,
			null,
			request,
			response);

		// then
		assertThat(out).isInstanceOf(String.class);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

		JsonNode node = objectMapper.readTree((String)out);
		assertThat(node.get("status").asInt()).isEqualTo(200);
		assertThat(node.get("code").asText()).isEqualTo("S_200");
		assertThat(node.get("data").asText()).isEqualTo("ok");
	}

	@Test
	@DisplayName("beforeBodyWrite: swagger 관련 경로면 wrapping을 skip한다")
	void beforeBodyWrite_whenSwaggerPath_thenSkipsWrap() throws Exception {
		// given
		Map<String, Integer> body = Map.of("a", 1);
		MethodParameter returnType = returnType(TestController.class, "ok");
		MockHttpServletRequest servletReq = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
		ServerHttpRequest request = new ServletServerHttpRequest(servletReq);
		ServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());

		// when
		Object out = advice.beforeBodyWrite(
			body,
			returnType,
			MediaType.APPLICATION_JSON,
			null,
			request,
			response);

		// then
		assertThat(out).isSameAs(body);
	}

	private static MethodParameter returnType(Class<?> clazz, String methodName) throws Exception {
		Method method = clazz.getDeclaredMethod(methodName);
		return new MethodParameter(method, -1);
	}

	private static class TestController {
		public String ok() {
			return "ok";
		}

		public ApiResponse<String> alreadyWrapped() {
			return new ApiResponse<>(200, "S_200", "ok", "message");
		}

		@NoWrap
		public String methodNoWrap() {
			return "no";
		}
	}

	@NoWrap
	private static class TypeNoWrapController {
		public String ok() {
			return "ok";
		}
	}
}
