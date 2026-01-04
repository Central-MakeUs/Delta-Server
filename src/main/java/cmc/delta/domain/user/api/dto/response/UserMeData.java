package cmc.delta.domain.user.api.dto.response;

public record UserMeData(
	Long userId,
	String email,
	String nickname
) {}
