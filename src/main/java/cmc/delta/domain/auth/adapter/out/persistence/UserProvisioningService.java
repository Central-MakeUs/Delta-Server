package cmc.delta.domain.auth.adapter.out.persistence;

import cmc.delta.domain.user.model.User;

public interface UserProvisioningService {
	ProvisioningResult provisionSocialUser(SocialUserProvisionCommand command);

	record ProvisioningResult(User user, boolean isNewUser) {}
}
