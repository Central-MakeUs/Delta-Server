package cmc.delta.domain.user.application.port.in.dto;

public record UserProfileImageResult(
	String storageKey,
	String viewUrl,
	Integer ttlSeconds
) {
	public static UserProfileImageResult empty() {
		return new UserProfileImageResult(null, null, null);
	}
}
