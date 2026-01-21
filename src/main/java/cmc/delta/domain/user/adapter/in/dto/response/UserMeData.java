package cmc.delta.domain.user.adapter.in.dto.response;

public record UserMeData(
	Long userId,
	String email,
	String nickname
) {}
