package cmc.delta.domain.auth.application.port.out;

import cmc.delta.domain.user.model.User;
import java.util.Optional;

public interface AdminUserQueryPort {

	Optional<User> findAdminByUsername(String username);
}
