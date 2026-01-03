package cmc.delta.domain.auth.api.dto;

public record KakaoLoginData(
        String email,
        String nickname,
        boolean isNewUser
) {
    public static KakaoLoginData of(String email, String nickname, boolean isNewUser) {
        return new KakaoLoginData(email, nickname, isNewUser);
    }
}
