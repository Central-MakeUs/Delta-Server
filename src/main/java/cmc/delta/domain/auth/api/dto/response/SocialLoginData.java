package cmc.delta.domain.auth.api.dto.response;

public record SocialLoginData(String email, String nickname, boolean isNewUser) {
}
