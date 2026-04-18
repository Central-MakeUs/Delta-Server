package cmc.delta.domain.user.application.port.out;

import java.util.Optional;

import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.model.User;

public interface UserRepositoryPort {
	Optional<User> findById(Long id);

	User save(User user);

	User getReferenceById(Long id);

	void delete(User user);

	long count();

	default User findActiveById(Long id) {
		User user = findById(id).orElseThrow(UserException::userNotFound);
		if (user.isWithdrawn()) {
			throw UserException.userWithdrawn();
		}
		return user;
	}
}
