package cmc.delta.domain.problem.api.problem.support;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.api.problem.ProblemController;
import cmc.delta.domain.problem.api.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.command.ProblemService;
import cmc.delta.domain.problem.application.query.ProblemQueryService;
import cmc.delta.domain.problem.application.query.support.ProblemListConditionFactory;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.config.security.principal.UserPrincipal;
import java.util.List;
import org.springframework.data.domain.Pageable;

public final class ProblemControllerTestModule {

	public final ProblemService problemService;
	public final ProblemQueryService problemQueryService;
	public final ProblemListConditionFactory conditionFactory;
	public final ProblemController sut;

	public final UserPrincipal principal;

	// create 케이스에서만 사용
	public final ProblemCreateRequest createRequest;
	public final ProblemCreateResponse createResponse;

	// list 케이스에서만 사용
	public final MyProblemListRequest listRequest;
	public final PagedResponse<ProblemListItemResponse> listResponse;

	private ApiResponse<?> lastApiResponse;

	private ProblemControllerTestModule(
		ProblemService problemService,
		ProblemQueryService problemQueryService,
		ProblemListConditionFactory conditionFactory,
		UserPrincipal principal,
		ProblemCreateRequest createRequest,
		ProblemCreateResponse createResponse,
		MyProblemListRequest listRequest,
		PagedResponse<ProblemListItemResponse> listResponse
	) {
		this.problemService = problemService;
		this.problemQueryService = problemQueryService;
		this.conditionFactory = conditionFactory;
		this.sut = new ProblemController(problemService, problemQueryService, conditionFactory);

		this.principal = principal;
		this.createRequest = createRequest;
		this.createResponse = createResponse;

		this.listRequest = listRequest;
		this.listResponse = listResponse;
	}

	public static ProblemControllerTestModule createCase() {
		// given
		ProblemService problemService = mock(ProblemService.class);
		ProblemQueryService problemQueryService = mock(ProblemQueryService.class);
		ProblemListConditionFactory conditionFactory = mock(ProblemListConditionFactory.class);

		UserPrincipal principal = mock(UserPrincipal.class);
		when(principal.userId()).thenReturn(10L);

		ProblemCreateRequest request = mock(ProblemCreateRequest.class);

		ProblemCreateResponse response = mock(ProblemCreateResponse.class);
		when(problemService.createWrongAnswerCard(eq(10L), same(request))).thenReturn(response);

		return new ProblemControllerTestModule(
			problemService,
			problemQueryService,
			conditionFactory,
			principal,
			request,
			response,
			null,
			null
		);
	}

	public static ProblemControllerTestModule listCase() {
		// given
		ProblemService problemService = mock(ProblemService.class);
		ProblemQueryService problemQueryService = mock(ProblemQueryService.class);
		ProblemListConditionFactory conditionFactory = mock(ProblemListConditionFactory.class);

		UserPrincipal principal = mock(UserPrincipal.class);
		when(principal.userId()).thenReturn(10L);

		MyProblemListRequest request = new MyProblemListRequest(
			"S1",
			"U1",
			"T1",
			ProblemListSort.RECENT,
			ProblemStatusFilter.ALL,
			0,
			20
		);

		ProblemListCondition condition = new ProblemListCondition(
			"S1",
			"U1",
			"T1",
			ProblemListSort.RECENT,
			ProblemStatusFilter.ALL
		);

		when(conditionFactory.from(same(request))).thenReturn(condition);

		PagedResponse<ProblemListItemResponse> listResponse =
			new PagedResponse<>(List.of(), 0, 20, 0L, 0);

		when(problemQueryService.getMyProblemCardList(eq(10L), same(condition), any(Pageable.class)))
			.thenReturn(listResponse);

		return new ProblemControllerTestModule(
			problemService,
			problemQueryService,
			conditionFactory,
			principal,
			null,
			null,
			request,
			listResponse
		);
	}

	public void callCreate() {
		this.lastApiResponse = sut.createWrongAnswerCard(principal, createRequest);
	}

	public void callList() {
		this.lastApiResponse = sut.getMyProblemList(principal, listRequest);
	}

	public void thenCreateCalled() {
		verify(problemService).createWrongAnswerCard(eq(10L), same(createRequest));
		verifyNoInteractions(problemQueryService);
		verifyNoMoreInteractions(problemService);
		if (lastApiResponse == null) throw new AssertionError("response is null");
	}

	public void thenListCalled() {
		verify(conditionFactory).from(same(listRequest));
		verify(problemQueryService).getMyProblemCardList(eq(10L), any(ProblemListCondition.class), any(Pageable.class));
		verifyNoInteractions(problemService);
		verifyNoMoreInteractions(problemQueryService);
		if (lastApiResponse == null) throw new AssertionError("response is null");
	}
}
