package cmc.delta.domain.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String code
) {}
