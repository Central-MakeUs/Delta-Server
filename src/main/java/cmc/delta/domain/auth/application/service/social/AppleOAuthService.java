package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleIdTokenVerifier;
import cmc.delta.domain.auth.adapter.out.oauth.apple.AppleOAuthClient;
import cmc.delta.domain.auth.application.exception.SocialAuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleOAuthService {

	private final AppleOAuthClient appleOAuthClient;
	private final AppleIdTokenVerifier appleIdTokenVerifier;
	private final ObjectMapper objectMapper;

	public SocialUserInfo fetchUserInfoByCode(String code, String userJson) {
		AppleIdTokenVerifier.AppleIdClaims verified = exchangeAndVerify(code);
		String providerUserId = requireProviderUserId(verified);
		Optional<AppleUserFromForm> form = parseUserForm(userJson);

		return new SocialUserInfo(providerUserId, resolveEmail(form, verified), resolveNickname(form));
	}

	private AppleIdTokenVerifier.AppleIdClaims exchangeAndVerify(String code) {
		AppleOAuthClient.AppleTokenResponse token = appleOAuthClient.exchangeCode(code);
		return appleIdTokenVerifier.verifyAndExtract(token.idToken());
	}

	private String requireProviderUserId(AppleIdTokenVerifier.AppleIdClaims verified) {
		String providerUserId = verified.sub();
		if (!StringUtils.hasText(providerUserId)) {
			throw SocialAuthException.authenticationFailed("애플 id_token(sub)이 비어있습니다.");
		}
		return providerUserId;
	}

	/** 재로그인 시 form의 email이 비어있을 수 있으므로 id_token의 email로 보완한다. */
	private String resolveEmail(Optional<AppleUserFromForm> form, AppleIdTokenVerifier.AppleIdClaims verified) {
		return firstNonBlank(form.map(AppleUserFromForm::email).orElse(null), verified.email());
	}

	private String resolveNickname(Optional<AppleUserFromForm> form) {
		return form.map(AppleUserFromForm::name)
			.map(name -> buildName(name.lastName(), name.firstName()))
			.orElse(null);
	}

	private Optional<AppleUserFromForm> parseUserForm(String userJson) {
		if (!StringUtils.hasText(userJson)) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(userJson, AppleUserFromForm.class));
		} catch (Exception e) {
			log.warn("애플 user JSON 파싱 실패", e);
			throw SocialAuthException.invalidRequest("애플 user 파싱에 실패했습니다.");
		}
	}

	private String buildName(String lastName, String firstName) {
		String merged = (trimToEmpty(lastName) + trimToEmpty(firstName)).trim();
		return merged.isEmpty() ? null : merged;
	}

	private String trimToEmpty(String value) {
		return value == null ? "" : value.trim();
	}

	private String firstNonBlank(String preferred, String fallback) {
		if (StringUtils.hasText(preferred)) {
			return preferred.trim();
		}
		return StringUtils.hasText(fallback) ? fallback.trim() : null;
	}

	// 애플 form_post의 user JSON 구조
	public static class AppleUserFromForm {
		private String email;
		private Name name;

		public String email() {
			return email;
		}

		public Name name() {
			return name;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public static class Name {
			private String firstName;
			private String lastName;

			public String firstName() {
				return firstName;
			}

			public String lastName() {
				return lastName;
			}

			public void setFirstName(String firstName) {
				this.firstName = firstName;
			}

			public void setLastName(String lastName) {
				this.lastName = lastName;
			}
		}
	}
}
