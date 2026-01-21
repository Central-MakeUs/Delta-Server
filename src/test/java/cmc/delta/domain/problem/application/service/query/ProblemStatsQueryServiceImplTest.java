package cmc.delta.domain.problem.application.service.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.*;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemUnitStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemStatsQueryPort;
import java.util.List;
import org.junit.jupiter.api.*;

class ProblemStatsQueryServiceImplTest {

	private ProblemStatsQueryPort statsPort;
	private ProblemStatsQueryServiceImpl sut;

	@BeforeEach
	void setUp() {
		statsPort = mock(ProblemStatsQueryPort.class);
		sut = new ProblemStatsQueryServiceImpl(statsPort);
	}

	@Test
	@DisplayName("getUnitStats: 포트 결과를 items로 매핑한다")
	void unitStats_success() {
		// given
		ProblemStatsCondition cond = mock(ProblemStatsCondition.class);
		when(statsPort.findUnitStats(10L, cond)).thenReturn(List.of(mock(ProblemUnitStatsRow.class)));

		// when
		ProblemStatsResponse<ProblemUnitStatsItemResponse> res = sut.getUnitStats(10L, cond);

		// then
		assertThat(res).isNotNull();
	}

	@Test
	@DisplayName("getTypeStats: 포트 결과를 items로 매핑한다")
	void typeStats_success() {
		// given
		ProblemStatsCondition cond = mock(ProblemStatsCondition.class);
		when(statsPort.findTypeStats(10L, cond)).thenReturn(List.of(mock(ProblemTypeStatsRow.class)));

		// when
		ProblemStatsResponse<ProblemTypeStatsItemResponse> res = sut.getTypeStats(10L, cond);

		// then
		assertThat(res).isNotNull();
	}
}
