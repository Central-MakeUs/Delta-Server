package cmc.delta.domain.auth.application.port.in.provisioning;

import cmc.delta.domain.auth.model.SocialProvider;

public record SocialUserProvisionCommand(
	SocialProvider provider,
	String providerUserId,
	String email,
	String nickname) {
}
