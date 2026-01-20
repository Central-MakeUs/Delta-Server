package cmc.delta.domain.user.adapter.in.web.dto.response;

public record UserMeData(
	Long userId,
	String email,
	String nickname
) {}
