package cmc.delta.domain.user.adapter.in.dto.request;

public record UserOnboardingRequest(
	String nickname,
	boolean termsAgreed) {
}
