package cmc.delta.domain.auth.adapter.out.oauth.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
	@JsonProperty("access_token")
	String accessToken,
	@JsonProperty("token_type")
	String tokenType,
	@JsonProperty("expires_in")
	int expiresIn,
	@JsonProperty("refresh_token")
	String refreshToken,
	@JsonProperty("id_token")
	String idToken) {
}
