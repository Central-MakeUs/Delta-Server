package cmc.delta.domain.auth.adapter.out.persistence;

import cmc.delta.domain.auth.model.SocialProvider;

public record SocialUserProvisionCommand(
	SocialProvider provider,
	String providerUserId,
	String email,
	String nickname
) {}
