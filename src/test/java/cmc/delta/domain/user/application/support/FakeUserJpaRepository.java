package cmc.delta.domain.user.application.support;

import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.persistence.UserJpaRepository;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class FakeUserJpaRepository {

	private FakeUserJpaRepository() {}

	public static UserJpaRepository create() {
		return new Handler().proxy();
	}

	private static final class Handler implements InvocationHandler {

		private final Map<Long, User> store = new HashMap<>();
		private final AtomicLong seq = new AtomicLong(0);

		UserJpaRepository proxy() {
			return (UserJpaRepository) Proxy.newProxyInstance(
				UserJpaRepository.class.getClassLoader(),
				new Class<?>[] { UserJpaRepository.class },
				this
			);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Object 메서드는 기본 처리
			if (method.getDeclaringClass() == Object.class) {
				return handleObjectMethod(proxy, method, args);
			}

			return switch (method.getName()) {
				case "findById" -> findById((Long) args[0]);
				case "save" -> save((User) args[0]);
				case "flush" -> null; // no-op
				default -> throw new UnsupportedOperationException("FakeUserJpaRepository 미지원 메서드: " + method);
			};
		}

		private Optional<User> findById(Long id) {
			return Optional.ofNullable(store.get(id));
		}

		private User save(User user) {
			if (user.getId() == null) {
				setId(user, seq.incrementAndGet());
			}
			store.put(user.getId(), user);
			return user;
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

		private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
			return switch (method.getName()) {
				case "toString" -> "FakeUserJpaRepositoryProxy";
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				default -> throw new UnsupportedOperationException(method.toString());
			};
		}
	}
}
