package cmc.delta.domain.curriculum.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.UnitJpaRepository;
import cmc.delta.domain.curriculum.model.Unit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitRepositoryAdapterTest {

	@Test
	@DisplayName("단원 조회: 루트 단원 조회를 UnitJpaRepository로 위임")
	void findAllRootUnitsActive_delegatesToJpaRepository() {
		// given
		UnitJpaRepository unitJpaRepository = mock(UnitJpaRepository.class);
		UnitRepositoryAdapter sut = new UnitRepositoryAdapter(unitJpaRepository);

		List<Unit> expected = List.of(mock(Unit.class));
		when(unitJpaRepository.findAllRootUnitsActive()).thenReturn(expected);

		// when
		List<Unit> result = sut.findAllRootUnitsActive();

		// then
		assertThat(result).isSameAs(expected);
		verify(unitJpaRepository).findAllRootUnitsActive();
	}

	@Test
	@DisplayName("단원 조회: 자식 단원 조회를 UnitJpaRepository로 위임")
	void findAllChildUnitsActive_delegatesToJpaRepository() {
		// given
		UnitJpaRepository unitJpaRepository = mock(UnitJpaRepository.class);
		UnitRepositoryAdapter sut = new UnitRepositoryAdapter(unitJpaRepository);

		List<Unit> expected = List.of(mock(Unit.class));
		when(unitJpaRepository.findAllChildUnitsActive()).thenReturn(expected);

		// when
		List<Unit> result = sut.findAllChildUnitsActive();

		// then
		assertThat(result).isSameAs(expected);
		verify(unitJpaRepository).findAllChildUnitsActive();
	}
}
