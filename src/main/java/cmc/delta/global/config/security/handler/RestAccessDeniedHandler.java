package cmc.delta.global.config.security.handler;

import cmc.delta.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
		throws IOException {
		errorResponseWriter.write(response, ErrorCode.ACCESS_DENIED);
	}
}
