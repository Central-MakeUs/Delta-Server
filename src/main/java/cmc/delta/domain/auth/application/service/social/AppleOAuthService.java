package cmc.delta.domain.auth.application.service.social;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleIdTokenVerifier;
import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleIdTokenVerifier.AppleIdClaims;
import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleOAuthClient;
import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleOAuthClient.AppleTokenResponse;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppleOAuthService {

	private final AppleOAuthClient appleOAuthClient;
	private final AppleIdTokenVerifier appleIdTokenVerifier;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AppleUserInfo fetchUserInfoByCode(String code, String userJson) {
		AppleTokenResponse token = appleOAuthClient.exchangeCode(code);

		AppleIdClaims claims = appleIdTokenVerifier.verifyAndExtract(token.idToken());

		String sub = claims.sub();
		String email = claims.email();
		String nickname = extractNameAsNickname(userJson);

		if (!StringUtils.hasText(sub)) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 sub가 비어있습니다.");
		}
		if (!StringUtils.hasText(email)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 이메일 제공 동의가 필요합니다.");
		}

		// name은 최초 1회만 올 수 있음. 신규 생성 정책상 필수면 여기서 강제.
		if (!StringUtils.hasText(nickname)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 이름 제공 동의(최초 1회)가 필요합니다.");
		}

		return new AppleUserInfo(sub, email, nickname);
	}

	private String extractNameAsNickname(String userJson) {
		if (!StringUtils.hasText(userJson)) return null;
		try {
			JsonNode root = objectMapper.readTree(userJson);
			JsonNode name = root.get("name");
			if (name == null) return null;

			String firstName = textOrNull(name.get("firstName"));
			String lastName = textOrNull(name.get("lastName"));

			String full = "";
			if (StringUtils.hasText(lastName)) full += lastName;
			if (StringUtils.hasText(firstName)) full += firstName;

			return StringUtils.hasText(full) ? full : null;
		} catch (Exception e) {
			return null;
		}
	}

	private String textOrNull(JsonNode node) {
		if (node == null || node.isNull()) return null;
		String v = node.asText();
		return StringUtils.hasText(v) ? v : null;
	}

	public record AppleUserInfo(String providerUserId, String email, String nickname) {}
}
