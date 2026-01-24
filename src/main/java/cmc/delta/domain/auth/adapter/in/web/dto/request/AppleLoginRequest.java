package cmc.delta.domain.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
	@NotBlank String code
) {}
