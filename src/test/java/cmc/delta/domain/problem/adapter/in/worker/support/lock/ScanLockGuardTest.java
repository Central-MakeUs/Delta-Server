package cmc.delta.domain.problem.adapter.in.worker.support.lock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.out.persistence.scan.worker.ScanWorkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScanLockGuardTest {

	@Test
	@DisplayName("락 확인: existsLockedBy가 null이 아니면 owned=true")
	void isOwned_whenExistsNotNull_thenTrue() {
		// given
		ScanWorkRepository repo = mock(ScanWorkRepository.class);
		ScanLockGuard sut = new ScanLockGuard(repo);
		when(repo.existsLockedBy(10L, "w1", "t1")).thenReturn(1);

		// when
		boolean owned = sut.isOwned(10L, "w1", "t1");

		// then
		assertThat(owned).isTrue();
	}

	@Test
	@DisplayName("락 확인: existsLockedBy가 null이면 owned=false")
	void isOwned_whenExistsNull_thenFalse() {
		// given
		ScanWorkRepository repo = mock(ScanWorkRepository.class);
		ScanLockGuard sut = new ScanLockGuard(repo);
		when(repo.existsLockedBy(10L, "w1", "t1")).thenReturn(null);

		// when
		boolean owned = sut.isOwned(10L, "w1", "t1");

		// then
		assertThat(owned).isFalse();
	}
}
