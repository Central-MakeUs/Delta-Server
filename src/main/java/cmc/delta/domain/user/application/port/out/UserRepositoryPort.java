package cmc.delta.domain.user.application.port.out;

import cmc.delta.domain.user.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
	Optional<User> findById(Long id);

	User save(User user);

	User getReferenceById(Long id);

	void delete(User user);
}
