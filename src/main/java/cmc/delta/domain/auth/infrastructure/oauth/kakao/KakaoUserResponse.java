package cmc.delta.domain.auth.infrastructure.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserResponse(long id, @JsonProperty("kakao_account")
KakaoAccount kakaoAccount) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record KakaoAccount(String email, Profile profile) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Profile(String nickname) {
	}
}
