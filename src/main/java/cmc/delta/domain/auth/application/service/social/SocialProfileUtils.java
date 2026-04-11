package cmc.delta.domain.auth.application.service.social;

import cmc.delta.domain.auth.application.exception.SocialAuthException;
import org.springframework.util.StringUtils;

final class SocialProfileUtils {

	private SocialProfileUtils() {
	}

	static String requireProvided(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw SocialAuthException.invalidRequest(message);
		}
		return value;
	}
}
