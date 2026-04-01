package cmc.delta.domain.auth.adapter.out.oauth.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserResponse(
	String id,
	String email,
	@JsonProperty("verified_email")
	boolean verifiedEmail,
	String name) {
}
