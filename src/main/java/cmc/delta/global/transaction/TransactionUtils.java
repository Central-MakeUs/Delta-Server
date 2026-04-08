package cmc.delta.global.transaction;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtils {

	private TransactionUtils() {
	}

	/**
	 * 현재 트랜잭션이 커밋된 후 runnable을 실행한다.
	 * 활성 트랜잭션이 없으면 즉시 실행한다.
	 */
	public static void afterCommit(Runnable runnable) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			runnable.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				runnable.run();
			}
		});
	}

	/**
	 * 현재 트랜잭션이 커밋된 후 runnable을 실행한다.
	 * 활성 트랜잭션이 없으면 실행하지 않는다.
	 */
	public static void afterCommitIfActive(Runnable runnable) {
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
