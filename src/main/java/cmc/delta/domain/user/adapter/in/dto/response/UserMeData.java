package cmc.delta.domain.user.adapter.in.dto.response;

import cmc.delta.domain.auth.model.SocialProvider;

public record UserMeData(
	Long userId,
	String email,
	String nickname,
	SocialProvider oauthProvider) {
}
