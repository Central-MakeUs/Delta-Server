package cmc.delta.domain.problem.adapter.in.web.problem;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemStatsRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemStatsResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemUnitStatsItemResponse;
import cmc.delta.domain.problem.application.port.in.problem.ProblemStatsUseCase;
import cmc.delta.domain.problem.application.support.query.ProblemStatsConditionFactory;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemStatsCondition;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProblemStatsControllerTest {

	@Mock ProblemStatsUseCase statsUseCase;
	@Mock ProblemStatsConditionFactory statsConditionFactory;

	private ProblemStatsController sut;

	@BeforeEach
	void setUp() {
		sut = new ProblemStatsController(statsUseCase, statsConditionFactory);
	}

	@Test
	@DisplayName("getMyUnitStats: condition 생성 후 usecase에 위임한다")
	void unitStats_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemStatsRequest query = mock(ProblemStatsRequest.class);
		ProblemStatsCondition condition = mock(ProblemStatsCondition.class);

		when(statsConditionFactory.from(query)).thenReturn(condition);
		when(statsUseCase.getUnitStats(10L, condition)).thenReturn(mock(ProblemStatsResponse.class));

		// when
		ApiResponse<ProblemStatsResponse<ProblemUnitStatsItemResponse>> res = sut.getMyUnitStats(principal, query);

		// then
		assertThat(res).isNotNull();
		verify(statsUseCase).getUnitStats(10L, condition);
	}

	@Test
	@DisplayName("getMyTypeStats: condition 생성 후 usecase에 위임한다")
	void typeStats_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemStatsRequest query = mock(ProblemStatsRequest.class);
		ProblemStatsCondition condition = mock(ProblemStatsCondition.class);

		when(statsConditionFactory.from(query)).thenReturn(condition);
		when(statsUseCase.getTypeStats(10L, condition)).thenReturn(mock(ProblemStatsResponse.class));

		// when
		ApiResponse<ProblemStatsResponse<ProblemTypeStatsItemResponse>> res = sut.getMyTypeStats(principal, query);

		// then
		assertThat(res).isNotNull();
		verify(statsUseCase).getTypeStats(10L, condition);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
