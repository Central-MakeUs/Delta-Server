package cmc.delta.domain.problem.api.problem.support;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.api.problem.ProblemController;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemCreateRequest;
import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.command.ProblemService;
import cmc.delta.domain.problem.application.query.ProblemQueryService;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.config.security.principal.UserPrincipal;
import java.util.List;
import org.springframework.data.domain.Pageable;

public final class ProblemControllerTestModule {

	public final ProblemService problemService;
	public final ProblemQueryService problemQueryService;
	public final ProblemController sut;

	public final UserPrincipal principal;

	// create 케이스에서만 사용
	public final ProblemCreateRequest createRequest;
	public final ProblemCreateResponse createResponse;

	// list 케이스에서만 사용
	public final PagedResponse<ProblemListItemResponse> listResponse;

	private ApiResponse<?> lastApiResponse;

	private ProblemControllerTestModule(
		ProblemService problemService,
		ProblemQueryService problemQueryService,
		UserPrincipal principal,
		ProblemCreateRequest createRequest,
		ProblemCreateResponse createResponse,
		PagedResponse<ProblemListItemResponse> listResponse
	) {
		this.problemService = problemService;
		this.problemQueryService = problemQueryService;
		this.sut = new ProblemController(problemService, problemQueryService);

		this.principal = principal;
		this.createRequest = createRequest;
		this.createResponse = createResponse;
		this.listResponse = listResponse;
	}

	public static ProblemControllerTestModule createCase() {
		ProblemService problemService = mock(ProblemService.class);
		ProblemQueryService problemQueryService = mock(ProblemQueryService.class);

		UserPrincipal principal = mock(UserPrincipal.class);
		when(principal.userId()).thenReturn(10L);

		// ProblemCreateRequest가 record/final이면 mock이 막힐 수 있음.
		// 그때는 실제 생성자/정적 팩토리로 만들어 넣어주면 됨.
		ProblemCreateRequest request = mock(ProblemCreateRequest.class);

		ProblemCreateResponse response = mock(ProblemCreateResponse.class);
		when(problemService.createWrongAnswerCard(eq(10L), same(request))).thenReturn(response);

		return new ProblemControllerTestModule(problemService, problemQueryService, principal, request, response, null);
	}

	public static ProblemControllerTestModule listCase() {
		ProblemService problemService = mock(ProblemService.class);
		ProblemQueryService problemQueryService = mock(ProblemQueryService.class);

		UserPrincipal principal = mock(UserPrincipal.class);
		when(principal.userId()).thenReturn(10L);

		PagedResponse<ProblemListItemResponse> listResponse =
			new PagedResponse<>(List.of(), 0, 20, 0L, 0);

		when(problemQueryService.getMyProblemCardList(eq(10L), any(ProblemListCondition.class), any(Pageable.class)))
			.thenReturn(listResponse);

		return new ProblemControllerTestModule(problemService, problemQueryService, principal, null, null, listResponse);
	}

	public void callCreate() {
		this.lastApiResponse = sut.createWrongAnswerCard(principal, createRequest);
	}

	public void callList(
		String subjectId,
		String unitId,
		String typeId,
		ProblemListSort sort,
		int page,
		int size
	) {
		this.lastApiResponse = sut.getMyProblemList(principal, subjectId, unitId, typeId, sort, page, size);
	}

	public void thenCreateCalled() {
		verify(problemService).createWrongAnswerCard(eq(10L), same(createRequest));
		verifyNoInteractions(problemQueryService);
		verifyNoMoreInteractions(problemService);
		if (lastApiResponse == null) throw new AssertionError("response is null");
	}

	public void thenListCalled() {
		// condition/pageable 조립까지 “간단히” 검증(값까지 깊게 보려면 captor로 확장 가능)
		verify(problemQueryService).getMyProblemCardList(eq(10L), any(ProblemListCondition.class), any(Pageable.class));
		verifyNoInteractions(problemService);
		verifyNoMoreInteractions(problemQueryService);
		if (lastApiResponse == null) throw new AssertionError("response is null");
	}
}
