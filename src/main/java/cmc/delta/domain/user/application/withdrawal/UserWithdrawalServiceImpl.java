package cmc.delta.domain.user.application.withdrawal;

import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawalServiceImpl implements UserWithdrawalService {

	private final UserJpaRepository userJpaRepository;

	@Override
	@Transactional
	public void withdraw(Long userId) {
		User user = userJpaRepository.findById(userId).orElseThrow(UserException::userNotFound);
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
		user.withdraw();
	}
}
