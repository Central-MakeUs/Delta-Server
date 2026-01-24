package cmc.delta.domain.auth.application.service.social;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleIdTokenVerifier;
import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleOAuthClient;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppleOAuthService {

	private final AppleOAuthClient appleOAuthClient;
	private final AppleIdTokenVerifier appleIdTokenVerifier;
	private final ObjectMapper objectMapper;

	public AppleUserInfo fetchUserInfoByCode(String code, String userJson) {
		AppleOAuthClient.AppleTokenResponse token = appleOAuthClient.exchangeCode(code);

		AppleIdTokenVerifier.AppleIdClaims verified =
			appleIdTokenVerifier.verifyAndExtract(token.idToken());

		String providerUserId = verified.sub();
		if (!StringUtils.hasText(providerUserId)) {
			throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "애플 id_token(sub)이 비어있습니다.");
		}

		AppleUserFromForm form = parseUserJsonOrNull(userJson);

		String email = firstNonBlank(
			form == null ? null : form.email(),
			verified.email()
		);

		String nickname = null;
		if (form != null && form.name() != null) {
			nickname = buildName(form.name().lastName(), form.name().firstName());
		}

		return new AppleUserInfo(providerUserId, email, nickname);
	}

	private AppleUserFromForm parseUserJsonOrNull(String userJson) {
		if (!StringUtils.hasText(userJson)) {
			return null;
		}
		try {
			return objectMapper.readValue(userJson, AppleUserFromForm.class);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "애플 user 파싱에 실패했습니다.");
		}
	}

	private String buildName(String lastName, String firstName) {
		String ln = lastName == null ? "" : lastName.trim();
		String fn = firstName == null ? "" : firstName.trim();
		String merged = (ln + fn).trim();
		return merged.isEmpty() ? null : merged;
	}

	private String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) return a.trim();
		if (StringUtils.hasText(b)) return b.trim();
		return null;
	}

	public record AppleUserInfo(String providerUserId, String email, String nickname) {}

	// 애플 form_post의 user JSON 구조
	public static class AppleUserFromForm {
		private String email;
		private Name name;

		public String email() { return email; }
		public Name name() { return name; }

		public void setEmail(String email) { this.email = email; }
		public void setName(Name name) { this.name = name; }

		public static class Name {
			private String firstName;
			private String lastName;

			public String firstName() { return firstName; }
			public String lastName() { return lastName; }

			public void setFirstName(String firstName) { this.firstName = firstName; }
			public void setLastName(String lastName) { this.lastName = lastName; }
		}
	}
}
