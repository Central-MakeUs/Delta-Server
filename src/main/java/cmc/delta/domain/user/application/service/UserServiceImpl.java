package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserUseCase;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserUseCase {

	private final UserRepositoryPort userRepositoryPort;

	@Override
	@Transactional(readOnly = true)
	public UserMeData getMyProfile(long userId) {
		return userRepositoryPort.findById(userId)
			.map(u -> new UserMeData(u.getId(), u.getEmail(), u.getNickname()))
			.orElseThrow(UserException::userNotFound);
	}

	@Override
	public void withdrawAccount(Long userId) {
		User user = userRepositoryPort.findById(userId)
			.orElseThrow(UserException::userNotFound);

		userRepositoryPort.delete(user);

		log.info("event=user.delete userId={} result=success", userId);
	}
}
