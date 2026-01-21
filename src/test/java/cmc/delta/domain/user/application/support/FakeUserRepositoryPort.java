package cmc.delta.domain.user.application.support;

import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class FakeUserRepositoryPort implements UserRepositoryPort {

	private final Map<Long, User> store = new HashMap<>();
	private final AtomicLong seq = new AtomicLong(0);

	private int deleteCallCount = 0;
	private final List<Long> deletedIds = new ArrayList<>();

	public static FakeUserRepositoryPort create() {
		return new FakeUserRepositoryPort();
	}

	private FakeUserRepositoryPort() {}

	@Override
	public Optional<User> findById(Long id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public User save(User user) {
		if (user.getId() == null) {
			setId(user, seq.incrementAndGet());
		}
		store.put(user.getId(), user);
		return user;
	}

	@Override
	public User getReferenceById(Long id) {
		User user = store.get(id);
		if (user == null) {
			throw new EntityNotFoundException("User not found. id=" + id);
		}
		return user;
	}

	@Override
	public void delete(User user) {
		deleteCallCount += 1;
		if (user == null || user.getId() == null) {
			return;
		}
		deletedIds.add(user.getId());
		store.remove(user.getId());
	}

	public int deleteCallCount() {
		return deleteCallCount;
	}

	public List<Long> deletedIds() {
		return Collections.unmodifiableList(deletedIds);
	}

	private void setId(User user, long id) {
		try {
			Field field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (Exception e) {
			throw new IllegalStateException("테스트용 id 세팅 실패", e);
		}
	}
}
