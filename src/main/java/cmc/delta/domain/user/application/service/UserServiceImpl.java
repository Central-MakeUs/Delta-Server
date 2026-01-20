package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.api.dto.response.UserMeData;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
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
	@Transactional
	public void withdrawAccount(Long userId) {
		User user = userJpaRepository.findById(userId)
			.orElseThrow(UserException::userNotFound);

		userJpaRepository.delete(user);

		log.info("event=user.delete userId={} result=success", userId);
	}
}

