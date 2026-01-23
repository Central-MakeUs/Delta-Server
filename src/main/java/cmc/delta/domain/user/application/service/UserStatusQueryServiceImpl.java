package cmc.delta.domain.user.application.service;

import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.port.in.UserStatusQuery;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatusQueryServiceImpl implements UserStatusQuery {

	private final UserRepositoryPort userRepositoryPort;

	@Override
	@Transactional(readOnly = true)
	public UserStatus getStatus(long userId) {
		return userRepositoryPort.findById(userId)
			.map(u -> u.getStatus())
			.orElseThrow(UserException::userNotFound);
	}
}
