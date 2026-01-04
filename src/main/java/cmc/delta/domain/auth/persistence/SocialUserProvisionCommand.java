package cmc.delta.domain.auth.persistence;

import cmc.delta.domain.auth.model.SocialProvider;

public record SocialUserProvisionCommand(
	SocialProvider provider,
	String providerUserId,
	String email,
	String nickname
) {}
