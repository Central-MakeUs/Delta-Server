package cmc.delta.domain.curriculum.adapter.out.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemTypeLoadJpaAdapterTest {

	@Test
	@DisplayName("유형 조회: typeId로 조회를 ProblemTypeJpaRepository로 위임")
	void findById_delegatesToJpaRepository() {
		// given
		ProblemTypeJpaRepository jpaRepository = mock(ProblemTypeJpaRepository.class);
		ProblemTypeLoadJpaAdapter sut = new ProblemTypeLoadJpaAdapter(jpaRepository);

		ProblemType type = mock(ProblemType.class);
		when(jpaRepository.findById("T1")).thenReturn(Optional.of(type));

		// when
		Optional<ProblemType> result = sut.findById("T1");

		// then
		assertThat(result).containsSame(type);
		verify(jpaRepository).findById("T1");
	}
}
