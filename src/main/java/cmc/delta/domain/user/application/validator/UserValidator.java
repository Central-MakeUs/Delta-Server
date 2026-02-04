package cmc.delta.domain.user.application.validator;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.application.exception.UserException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserValidator {

	private final Clock clock;

	public void validateProvision(SocialUserProvisionCommand command) {
		requireNotNull(command);
		requireNotNull(command.provider());
		requireText(command.providerUserId());
	}

	public void validate(UserOnboardingRequest request) {
		requireNotNull(request);
		requireText(request.nickname());
		LocalDate birthDate = request.birthDate();
		requireNotNull(birthDate);
		// 미래 생년월일 방지 (원치 않으면 제거)
		require(!birthDate.isAfter(LocalDate.now(clock)));
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
