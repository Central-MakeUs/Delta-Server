package cmc.delta.domain.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(@NotBlank
String code) {
}
