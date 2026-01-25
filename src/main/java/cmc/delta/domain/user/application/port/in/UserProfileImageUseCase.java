package cmc.delta.domain.user.application.port.in;

import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;

public interface UserProfileImageUseCase {
	UserProfileImageResult uploadMyProfileImage(Long userId, ProfileImageUploadCommand command);

	UserProfileImageResult getMyProfileImage(Long userId);

	void deleteMyProfileImage(Long userId);
}
