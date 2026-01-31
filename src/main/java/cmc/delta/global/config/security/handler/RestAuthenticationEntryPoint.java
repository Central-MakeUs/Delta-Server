package cmc.delta.global.config.security.handler;

import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.config.security.jwt.JwtAuthenticationException;
import cmc.delta.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
		throws IOException {

		ErrorCode ec = resolveErrorCode(request, ex);

		response.setStatus(ec.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		objectMapper.writeValue(
			response.getOutputStream(),
			ApiResponses.fail(ec.status().value(), ec.code(), null, ec.defaultMessage()));
	}

	private ErrorCode resolveErrorCode(HttpServletRequest request, AuthenticationException ex) {
		if (ex instanceof JwtAuthenticationException jwtEx) {
			return jwtEx.getErrorCode();
		}

		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(authorization)) {
			return ErrorCode.TOKEN_REQUIRED;
		}
		return ErrorCode.AUTHENTICATION_FAILED;
	}
}
