package cmc.delta.domain.problem.adapter.in.worker.support.lock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ScanUnlockerTest {

	@Test
	@DisplayName("best-effort unlock: 트랜잭션 템플릿 내에서 unlock을 호출")
	void unlockBestEffort_runsUnlockInTransaction() {
		// given
		TransactionTemplate tx = mock(TransactionTemplate.class);
		ScanWorkRepository repo = mock(ScanWorkRepository.class);
		ScanUnlocker sut = new ScanUnlocker(tx, repo);

		doAnswer(inv -> {
			Consumer<TransactionStatus> cb = inv.getArgument(0);
			cb.accept(mock(TransactionStatus.class));
			return null;
		}).when(tx).executeWithoutResult(any());

		// when
		sut.unlockBestEffort(10L, "w1", "t1");

		// then
		verify(repo).unlock(10L, "w1", "t1");
	}

	@Test
	@DisplayName("best-effort unlock: 내부 예외가 발생해도 외부로 전파하지 않음")
	void unlockBestEffort_whenThrows_thenSwallow() {
		// given
		TransactionTemplate tx = mock(TransactionTemplate.class);
		ScanWorkRepository repo = mock(ScanWorkRepository.class);
		ScanUnlocker sut = new ScanUnlocker(tx, repo);

		doThrow(new RuntimeException("tx failed")).when(tx).executeWithoutResult(any());

		// when/then
		assertThatCode(() -> sut.unlockBestEffort(10L, "w1", "t1"))
			.doesNotThrowAnyException();
	}
}
