package cmc.delta.domain.user.application.validator;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.application.exception.UserException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UserValidator {

	public void validateProvision(SocialUserProvisionCommand command) {
		requireNotNull(command);
		requireNotNull(command.provider());
		requireText(command.providerUserId());
	}

	public void validate(UserOnboardingRequest request) {
		requireNotNull(request);
		requireText(request.nickname());
		require(request.termsAgreed());
	}

	public void validate(UserNameUpdateRequest request) {
		requireNotNull(request);
		requireText(request.name());
	}

	public void validate(UserNicknameUpdateRequest request) {
		requireNotNull(request);
		requireText(request.nickname());
	}

	public void validateNickname(String nickname) {
		requireText(nickname);
	}

	private void requireNotNull(Object value) {
		require(value != null);
	}

	private void requireText(String value) {
		require(StringUtils.hasText(value));
	}

	private void require(boolean condition) {
		if (!condition) {
			throw UserException.invalidRequest();
		}
	}
}
