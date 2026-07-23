package cmc.delta.global.config.security.handler;

import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** 시큐리티 필터 단계(핸들러/엔트리포인트)에서 공통 에러 응답 본문을 직렬화한다. */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

	private final ObjectMapper objectMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(
			response.getOutputStream(),
			ApiResponses.fail(errorCode.status().value(), errorCode.code(), null, errorCode.defaultMessage()));
	}
}
