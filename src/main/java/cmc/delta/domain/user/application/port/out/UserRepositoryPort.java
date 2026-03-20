package cmc.delta.domain.user.application.port.out;

import java.util.Optional;

import cmc.delta.domain.user.model.User;

public interface UserRepositoryPort {
	Optional<User> findById(Long id);

	User save(User user);

	User getReferenceById(Long id);

	void delete(User user);

	long count();
}