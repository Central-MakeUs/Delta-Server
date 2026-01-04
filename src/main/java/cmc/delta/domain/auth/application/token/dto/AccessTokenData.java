package cmc.delta.domain.auth.application.token.dto;

public record AccessTokenData(String accessToken) {

	public static AccessTokenData of(String accessToken) {
		return new AccessTokenData(accessToken);
	}
}
