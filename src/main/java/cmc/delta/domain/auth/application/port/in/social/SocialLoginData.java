package cmc.delta.domain.auth.application.port.in.social;

public record SocialLoginData(String email, String nickname, boolean isNewUser) {
}
