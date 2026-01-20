package cmc.delta.domain.auth.adapter.out.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(@JsonProperty("access_token")
String accessToken) {
}
