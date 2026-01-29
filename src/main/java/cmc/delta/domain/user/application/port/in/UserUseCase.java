package cmc.delta.domain.user.application.port.in;

import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;

public interface UserUseCase {
	UserMeData getMyProfile(long userId);

	void withdrawAccount(Long userId);

	void completeOnboarding(long userId, UserOnboardingRequest request);

	void updateMyName(long userId, UserNameUpdateRequest request);
}
