package cmc.delta.domain.user.adapter.in.worker;

import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.user.adapter.out.persistence.jpa.UserJpaRepository;
import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPurgeWorker {

	private final Clock clock;
	private final TransactionTemplate workerTxTemplate;

	private final UserJpaRepository userJpaRepository;
	private final UserRepositoryPort userRepositoryPort;

	private final ProblemJpaRepository problemJpaRepository;
	private final AssetJpaRepository assetJpaRepository;
	private final StoragePort storagePort;

	public int runBatch(int batchSize, int retentionDays) {
		Instant cutoff = Instant.now(clock).minus(Duration.ofDays(retentionDays));
		List<Long> userIds = userJpaRepository.findIdsByStatusAndWithdrawnAtBefore(
			UserStatus.WITHDRAWN,
			cutoff,
			PageRequest.of(0, batchSize));

		for (Long userId : userIds) {
			purgeOneBestEffort(userId, cutoff);
		}
		return userIds.size();
	}

	private void purgeOneBestEffort(Long userId, Instant cutoff) {
		try {
			purgeOne(userId, cutoff);
		} catch (Exception e) {
			log.error("event=user.purge_failed userId={}", userId, e);
		}
	}

	private void purgeOne(Long userId, Instant cutoff) {
		Set<String> keysToDelete = collectStorageKeys(userId);

		workerTxTemplate.executeWithoutResult(status -> {
			Optional<User> optional = userRepositoryPort.findById(userId);
			if (optional.isEmpty()) {
				return;
			}
			User user = optional.get();
			if (user.getStatus() != UserStatus.WITHDRAWN) {
				return;
			}
			Instant withdrawnAt = user.getWithdrawnAt();
			if (withdrawnAt == null || !withdrawnAt.isBefore(cutoff)) {
				return;
			}

			userRepositoryPort.delete(user);
			afterCommit(() -> deleteStorageKeysBestEffort(userId, keysToDelete));
			log.info("event=user.purge_db_deleted userId={} keys={}", userId, keysToDelete.size());
		});
	}

	private Set<String> collectStorageKeys(Long userId) {
		LinkedHashSet<String> keys = new LinkedHashSet<>();

		userRepositoryPort.findById(userId)
			.map(User::getProfileImageStorageKey)
			.ifPresent(keys::add);

		keys.addAll(problemJpaRepository.findOriginalStorageKeysByUserId(userId));
		keys.addAll(assetJpaRepository.findStorageKeysByUserId(userId));

		keys.removeIf(k -> k == null || k.isBlank());
		return keys;
	}

	private void deleteStorageKeysBestEffort(Long userId, Set<String> keys) {
		for (String key : keys) {
			try {
				storagePort.deleteImage(key);
			} catch (Exception e) {
				log.warn("event=user.purge_s3_delete_failed userId={} storageKey={}", userId, key, e);
			}
		}
		log.info("event=user.purge_s3_deleted userId={} count={}", userId, keys.size());
	}

	private void afterCommit(Runnable runnable) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				runnable.run();
			}
		});
	}
}
