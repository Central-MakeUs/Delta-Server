package cmc.delta.domain.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(@NotBlank
String code) {
}
