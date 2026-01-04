package cmc.delta.global.config.security.handler;

import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
		throws IOException {

		ErrorCode ec = ErrorCode.ACCESS_DENIED;

		response.setStatus(ec.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		objectMapper.writeValue(
			response.getOutputStream(),
			ApiResponses.fail(ec.status().value(), ec.code(), null, ec.defaultMessage())
		);
	}
}
