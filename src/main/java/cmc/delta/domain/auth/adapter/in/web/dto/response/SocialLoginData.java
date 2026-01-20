package cmc.delta.domain.auth.adapter.in.web.dto.response;

public record SocialLoginData(String email, String nickname, boolean isNewUser) {
}
