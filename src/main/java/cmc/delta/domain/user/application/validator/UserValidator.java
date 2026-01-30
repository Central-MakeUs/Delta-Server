package cmc.delta.domain.user.application.validator;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.application.exception.UserException;

@Component
public class UserValidator {

	public void validateProvision(SocialUserProvisionCommand command) {
		if (command == null || command.provider() == null) {
			throw UserException.invalidRequest();
		}
		if (!StringUtils.hasText(command.providerUserId())) {
			throw UserException.invalidRequest();
		}
	}

    public void validate(UserOnboardingRequest request) {
        if (request == null) {
            throw UserException.invalidRequest();
        }
        if (!StringUtils.hasText(request.nickname())) {
            throw UserException.invalidRequest();
        }
        LocalDate birthDate = request.birthDate();
        if (birthDate == null) {
            throw UserException.invalidRequest();
        }
		// 미래 생년월일 방지 (원치 않으면 제거)
		if (birthDate.isAfter(LocalDate.now())) {
			throw UserException.invalidRequest();
		}
		if (!request.termsAgreed()) {
			throw UserException.invalidRequest();
		}
	}

    public void validate(UserNameUpdateRequest request) {
        if (request == null) {
            throw UserException.invalidRequest();
        }
        if (!StringUtils.hasText(request.name())) {
            throw UserException.invalidRequest();
        }
    }

    public void validateNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw UserException.invalidRequest();
        }
        // optionally: length/char validation
    }
}
