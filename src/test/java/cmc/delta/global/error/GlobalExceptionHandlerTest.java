package cmc.delta.global.error;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.error.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

	@Test
	@DisplayName("handleBusiness: 4xx면 exception message/data를 그대로 응답에 사용")
	void handleBusiness_when4xx_thenUsesExceptionMessageAndData() {
		// given
		ErrorLogWriter logWriter = mock(ErrorLogWriter.class);
		ErrorResponseFactory factory = mock(ErrorResponseFactory.class);
		GlobalExceptionHandler handler = new GlobalExceptionHandler(logWriter, factory);

		Map<String, Object> data = Map.of("reason", "bad");
		BusinessException ex = new BusinessException(ErrorCode.INVALID_REQUEST, "custom", data);

		ApiResponse<Object> expectedBody = new ApiResponse<>(
			400,
			ErrorCode.INVALID_REQUEST.code(),
			data,
			"custom");
		when(factory.create(ErrorCode.INVALID_REQUEST, "custom", data)).thenReturn(expectedBody);

		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/test");

		// when
		ResponseEntity<ApiResponse<Object>> resp = handler.handleBusiness(ex, req);

		// then
		assertThat(resp.getStatusCode()).isEqualTo(ErrorCode.INVALID_REQUEST.status());
		assertThat(resp.getBody()).isSameAs(expectedBody);
		verify(logWriter).write(ErrorCode.INVALID_REQUEST, ex, req);
	}

	@Test
	@DisplayName("handleBusiness: 5xx면 상세 메시지/데이터를 숨기고 defaultMessage로 응답")
	void handleBusiness_when5xx_thenHidesDetail() {
		// given
		ErrorLogWriter logWriter = mock(ErrorLogWriter.class);
		ErrorResponseFactory factory = mock(ErrorResponseFactory.class);
		GlobalExceptionHandler handler = new GlobalExceptionHandler(logWriter, factory);

		Map<String, Object> data = Map.of("reason", "db down");
		BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "should-not-leak", data);

		ApiResponse<Object> expectedBody = new ApiResponse<>(
			500,
			ErrorCode.INTERNAL_ERROR.code(),
			null,
			ErrorCode.INTERNAL_ERROR.defaultMessage());
		when(factory.create(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null))
			.thenReturn(expectedBody);

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");

		// when
		ResponseEntity<ApiResponse<Object>> resp = handler.handleBusiness(ex, req);

		// then
		assertThat(resp.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.status());
		assertThat(resp.getBody()).isSameAs(expectedBody);
		verify(logWriter).write(ErrorCode.INTERNAL_ERROR, ex, req);
	}
}
