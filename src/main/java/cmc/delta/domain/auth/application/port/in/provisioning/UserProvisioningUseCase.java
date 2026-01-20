package cmc.delta.domain.auth.application.port.in.provisioning;

public interface UserProvisioningUseCase {
	ProvisioningResult provisionSocialUser(SocialUserProvisionCommand command);
	record ProvisioningResult(long userId, boolean isNewUser) {}
}
