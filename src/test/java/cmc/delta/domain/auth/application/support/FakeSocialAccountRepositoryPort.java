package cmc.delta.domain.auth.application.support;

import cmc.delta.domain.auth.application.port.out.SocialAccountRepositoryPort;
import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DataIntegrityViolationException;

public final class FakeSocialAccountRepositoryPort implements SocialAccountRepositoryPort {

	private final Map<String, SocialAccount> store = new HashMap<>();
	private final AtomicLong seq = new AtomicLong(0);

	private boolean duplicateOnNextSave = false;
	private SocialAccount existingAfterDuplicate = null;

	public static FakeSocialAccountRepositoryPort create() {
		return new FakeSocialAccountRepositoryPort();
	}

	private FakeSocialAccountRepositoryPort() {}

	@Override
	public Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId) {
		return Optional.ofNullable(store.get(key(provider, providerUserId)));
	}

	@Override
	public Optional<SocialAccount> findByUser(cmc.delta.domain.user.model.User user) {
		if (user == null || user.getId() == null)
			return Optional.empty();
		return store.values().stream()
			.filter(a -> a.getUser() != null && user.getId().equals(a.getUser().getId()))
			.findFirst();
	}

	@Override
	public SocialAccount save(SocialAccount account) {
		if (account == null) {
			throw new IllegalArgumentException("account is null");
		}

		if (duplicateOnNextSave) {
			duplicateOnNextSave = false;
			if (existingAfterDuplicate != null) {
				put(existingAfterDuplicate);
			}
			throw new DataIntegrityViolationException("duplicate");
		}

		String key = key(account.getProvider(), account.getProviderUserId());
		if (store.containsKey(key)) {
			throw new DataIntegrityViolationException("duplicate");
		}

		if (account.getId() == null) {
			setId(account, seq.incrementAndGet());
		}
		store.put(key, account);
		return account;
	}

	public void put(SocialAccount account) {
		if (account.getId() == null) {
			setId(account, seq.incrementAndGet());
		}
		store.put(key(account.getProvider(), account.getProviderUserId()), account);
	}

	public void triggerDuplicateOnNextSave(SocialAccount existingAfterDuplicate) {
		this.duplicateOnNextSave = true;
		this.existingAfterDuplicate = existingAfterDuplicate;
	}

	private String key(SocialProvider provider, String providerUserId) {
		String p = (provider == null) ? "null" : provider.name();
		String pid = (providerUserId == null) ? "null" : providerUserId;
		return p + ":" + pid;
	}

	private void setId(SocialAccount account, long id) {
		try {
			Field field = SocialAccount.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(account, id);
		} catch (Exception e) {
			throw new IllegalStateException("테스트용 id 세팅 실패", e);
		}
	}
}
