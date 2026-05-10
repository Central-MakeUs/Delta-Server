package cmc.delta.domain.auth.adapter.in.web;

import cmc.delta.domain.auth.adapter.in.support.TokenHeaderWriter;
import cmc.delta.domain.auth.adapter.in.web.dto.request.AdminLoginRequest;
import cmc.delta.domain.auth.application.port.in.admin.AdminLoginUseCase;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "어드민 인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

	private final AdminLoginUseCase adminLoginUseCase;
	private final TokenHeaderWriter tokenHeaderWriter;

	@Operation(summary = "어드민 로그인", description = "username(email)과 password로 로그인합니다.")
	@PostMapping("/login")
	public ApiResponse<Void> login(
		@Valid @RequestBody AdminLoginRequest request,
		HttpServletResponse response) {
		TokenIssuer.IssuedTokens tokens = adminLoginUseCase.login(request.username(), request.password());
		tokenHeaderWriter.write(response, tokens);
		return ApiResponses.success(SuccessCode.OK, null);
	}
}
