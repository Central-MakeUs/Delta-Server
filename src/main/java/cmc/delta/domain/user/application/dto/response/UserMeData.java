package cmc.delta.domain.user.application.dto.response;

public record UserMeData(
	Long userId,
	String email,
	String nickname
) {}
