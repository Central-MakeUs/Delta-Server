package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.adapter.in.web.dto.response.UserMeData;

public interface UserService {
	UserMeData getMyProfile(long userId);
	void withdrawAccount(Long userId);
}
