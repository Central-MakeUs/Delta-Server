package cmc.delta.domain.auth.application.port.in.provisioning;

import cmc.delta.domain.user.model.enums.UserRole;

public interface UserProvisioningUseCase {
	ProvisioningResult provisionSocialUser(SocialUserProvisionCommand command);

	record ProvisioningResult(long userId, String email, String nickname, UserRole role, boolean isNewUser) {
	}
}
