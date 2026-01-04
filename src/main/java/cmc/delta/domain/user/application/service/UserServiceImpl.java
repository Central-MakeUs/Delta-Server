package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.api.dto.response.UserMeData;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserJpaRepository userJpaRepository;

	@Override
	@Transactional(readOnly = true)
	public UserMeData getMyProfile(long userId) {
		return userJpaRepository.findById(userId)
			.map(u -> new UserMeData(u.getId(), u.getEmail(), u.getNickname()))
			.orElseThrow(UserException::userNotFound);
	}

	@Override
	public void withdrawAccount(Long userId) {
		User user = userJpaRepository.findById(userId).orElseThrow(UserException::userNotFound);
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
		user.withdraw();
	}
}

