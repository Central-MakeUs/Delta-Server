package cmc.delta.domain.auth.application.port.out;

public interface UserProvisioningPort {
	ProvisioningResult provisionSocialUser(SocialUserProvisionCommand command);

	record SocialUserProvisionCommand(
		String provider,
		String providerUserId,
		String email,
		String nickname
	) {}

	record ProvisioningResult(
		long userId,
		boolean isNewUser
	) {}
}
