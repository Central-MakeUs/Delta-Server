package cmc.delta.domain.user.application.port.out;

import cmc.delta.domain.user.model.User;

public interface UserRepositoryPort {
	User getReferenceById(Long id);
}
