package cmc.delta.domain.auth.api.dto.response;

public record SocialLoginData(String email, String nickname, boolean isNewUser) {
	public static SocialLoginData of(String email, String nickname, boolean isNewUser) {
		return new SocialLoginData(email, nickname, isNewUser);
	}
}
