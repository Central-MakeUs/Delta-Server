package cmc.delta.domain.curriculum.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.adapter.out.persistence.jpa.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.model.ProblemType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemTypeRepositoryAdapterTest {

	@Test
	@DisplayName("유형 조회: 사용자별 활성 유형 조회를 ProblemTypeJpaRepository로 위임")
	void findAllActiveForUser_delegatesToJpaRepository() {
		// given
		ProblemTypeJpaRepository jpaRepository = mock(ProblemTypeJpaRepository.class);
		ProblemTypeRepositoryAdapter sut = new ProblemTypeRepositoryAdapter(jpaRepository);

		List<ProblemType> expected = List.of(mock(ProblemType.class));
		when(jpaRepository.findAllActiveForUser(10L)).thenReturn(expected);

		// when
		List<ProblemType> result = sut.findAllActiveForUser(10L);

		// then
		assertThat(result).isSameAs(expected);
		verify(jpaRepository).findAllActiveForUser(10L);
	}
}
