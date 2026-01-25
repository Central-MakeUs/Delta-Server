package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class FakeUserRepositoryPort implements UserRepositoryPort {

	private final Map<Long, User> store = new HashMap<>();
	private long seq = 0L;

	public FakeUserRepositoryPort() {}

	public User put(long id, User user) {
		ReflectionIds.setId(user, id);
		store.put(id, user);
		seq = Math.max(seq, id);
		return user;
	}

	@Override
	public Optional<User> findById(Long id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public User save(User user) {
		if (user.getId() == null) {
			long id = ++seq;
			ReflectionIds.setId(user, id);
		}
		store.put(user.getId(), user);
		return user;
	}

	@Override
	public User getReferenceById(Long id) {
		User user = store.get(id);
		if (user == null)
			throw new IllegalStateException("user not found id=" + id);
		return user;
	}

	@Override
	public void delete(User user) {
		if (user == null || user.getId() == null)
			return;
		store.remove(user.getId());
	}
}
