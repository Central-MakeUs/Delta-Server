package cmc.delta.domain.auth.api;

import cmc.delta.domain.auth.api.dto.KakaoLoginData;
import cmc.delta.domain.auth.api.dto.KakaoLoginRequest;
import cmc.delta.domain.auth.application.AuthHeaderConstants;
import cmc.delta.domain.auth.application.KakaoAuthService;
import cmc.delta.domain.auth.application.port.TokenIssuer;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/** 카카오 인가코드로 로그인하고, 발급 토큰을 응답 헤더로 내려주는 컨트롤러. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class KakaoAuthController {

    private final KakaoAuthService kakaoAuthService;

    @PostMapping("/kakao")
    public KakaoLoginData login(@Valid @RequestBody KakaoLoginRequest request, HttpServletResponse response) {
        KakaoAuthService.LoginResult result = kakaoAuthService.loginWithCode(request.code());

        TokenIssuer.IssuedTokens tokens = result.tokens();
        response.setHeader(HttpHeaders.AUTHORIZATION, tokens.authorizationHeaderValue());

        if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
            response.setHeader(AuthHeaderConstants.REFRESH_TOKEN_HEADER, tokens.refreshToken());
        }

        // 필요 시: 브라우저에서 Authorization/X-Refresh-Token을 읽을 수 있도록 노출
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, AuthHeaderConstants.EXPOSE_HEADERS_VALUE);

        return result.data(); // ✅ DTO만 반환 -> ApiResponseAdvice가 래핑
    }
}
