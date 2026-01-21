package cmc.delta.domain.problem.adapter.in.web.problem;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemCompleteRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemUpdateRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.problem.ProblemCommandUseCase;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.in.problem.command.UpdateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.support.query.ProblemListConditionFactory;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.config.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProblemControllerTest {

	@Mock ProblemCommandUseCase problemCommandUseCase;
	@Mock ProblemQueryUseCase problemQueryUseCase;
	@Mock ProblemListConditionFactory conditionFactory;

	private ProblemController sut;

	@BeforeEach
	void setUp() {
		sut = new ProblemController(problemCommandUseCase, problemQueryUseCase, conditionFactory);
	}

	@Test
	@DisplayName("createWrongAnswerCard: request->command 후 usecase에 위임한다")
	void create_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemCreateRequest request = mock(ProblemCreateRequest.class);
		CreateWrongAnswerCardCommand command = mock(CreateWrongAnswerCardCommand.class);

		when(request.toCommand()).thenReturn(command);
		when(problemCommandUseCase.createWrongAnswerCard(10L, command)).thenReturn(mock(ProblemCreateResponse.class));

		// when
		ApiResponse<ProblemCreateResponse> res = sut.createWrongAnswerCard(principal, request);

		// then
		assertThat(res).isNotNull();
		verify(problemCommandUseCase).createWrongAnswerCard(10L, command);
	}

	@Test
	@DisplayName("getMyProblemList: pageable/condition 만든 뒤 usecase에 위임한다")
	void list_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		MyProblemListRequest query = mock(MyProblemListRequest.class);
		when(query.page()).thenReturn(0);
		when(query.size()).thenReturn(20);

		ProblemListCondition condition = mock(ProblemListCondition.class);
		when(conditionFactory.from(query)).thenReturn(condition);

		when(problemQueryUseCase.getMyProblemCardList(eq(10L), eq(condition), any(Pageable.class)))
			.thenReturn(mock(PagedResponse.class));

		// when
		ApiResponse<PagedResponse<ProblemListItemResponse>> res = sut.getMyProblemList(principal, query);

		// then
		assertThat(res).isNotNull();
		verify(problemQueryUseCase).getMyProblemCardList(eq(10L), eq(condition),
			argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 20)
		);
	}

	@Test
	@DisplayName("completeWrongAnswerCard: usecase에 위임한다")
	void complete_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemCompleteRequest request = mock(ProblemCompleteRequest.class);
		when(request.solutionText()).thenReturn("sol");

		// when
		ApiResponse<Void> res = sut.completeWrongAnswerCard(principal, 5L, request);

		// then
		assertThat(res).isNotNull();
		verify(problemCommandUseCase).completeWrongAnswerCard(10L, 5L, "sol");
	}

	@Test
	@DisplayName("getMyProblemDetail: usecase에 위임한다")
	void detail_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		when(problemQueryUseCase.getMyProblemDetail(10L, 7L)).thenReturn(mock(ProblemDetailResponse.class));

		// when
		ApiResponse<ProblemDetailResponse> res = sut.getMyProblemDetail(principal, 7L);

		// then
		assertThat(res).isNotNull();
		verify(problemQueryUseCase).getMyProblemDetail(10L, 7L);
	}

	@Test
	@DisplayName("updateWrongAnswerCard: request->command 후 usecase에 위임한다")
	void update_delegates() {
		// given
		UserPrincipal principal = principal(10L);
		ProblemUpdateRequest request = mock(ProblemUpdateRequest.class);
		UpdateWrongAnswerCardCommand command = mock(UpdateWrongAnswerCardCommand.class);

		when(request.toCommand()).thenReturn(command);

		// when
		ApiResponse<Void> res = sut.updateWrongAnswerCard(principal, 9L, request);

		// then
		assertThat(res).isNotNull();
		verify(problemCommandUseCase).updateWrongAnswerCard(10L, 9L, command);
	}

	private UserPrincipal principal(long userId) {
		UserPrincipal p = mock(UserPrincipal.class);
		when(p.userId()).thenReturn(userId);
		return p;
	}
}
