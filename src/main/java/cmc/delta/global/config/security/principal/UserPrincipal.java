package cmc.delta.global.config.security.principal;

public record UserPrincipal(
        Long userId,
        String role
) {
}
