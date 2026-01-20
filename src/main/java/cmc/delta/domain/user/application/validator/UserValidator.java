package cmc.delta.domain.user.application.validator;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.application.exception.UserException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
}
