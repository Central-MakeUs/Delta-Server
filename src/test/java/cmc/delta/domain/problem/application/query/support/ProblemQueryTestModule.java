package cmc.delta.domain.problem.application.query.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.service.query.ProblemQueryServiceImpl;
import cmc.delta.domain.problem.application.mapper.ProblemDetailMapper;
import cmc.delta.domain.problem.application.mapper.ProblemListMapper;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.ProblemQueryRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListRow;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.storage.StorageService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class ProblemQueryTestModule {

	public final ProblemQueryServiceImpl sut;
	public final Long userId;
	public final ProblemListCondition condition;
	public final Pageable pageable;

	private final ProblemQueryRepository queryRepo;
	private final ProblemListRequestValidator validator;
	private final ProblemListMapper listMapper;
	private final StorageService storage;
	private final ProblemDetailMapper detailMapper;

	private PagedResponse<ProblemListItemResponse> result;
	private RuntimeException expected;

	private ProblemQueryTestModule(
		ProblemQueryRepository queryRepo,
		ProblemListRequestValidator validator,
		ProblemListMapper listMapper,
		StorageService storage,
		ProblemDetailMapper detailMapper,
		Long userId,
		ProblemListCondition condition,
		Pageable pageable
	) {
		this.queryRepo = queryRepo;
		this.validator = validator;
		this.listMapper = listMapper;
		this.storage = storage;
		this.detailMapper = detailMapper;

		this.userId = userId;
		this.condition = condition;
		this.pageable = pageable;

		this.sut = new ProblemQueryServiceImpl(validator, listMapper, storage, queryRepo, detailMapper);
	}

	public static ProblemQueryTestModule successCase() {
		ProblemQueryRepository queryRepo = mock(ProblemQueryRepository.class);
		ProblemListRequestValidator validator = mock(ProblemListRequestValidator.class);
		ProblemListMapper listMapper = mock(ProblemListMapper.class);
		StorageService storage = mock(StorageService.class);
		ProblemDetailMapper detailMapper = mock(ProblemDetailMapper.class);

		Long userId = 10L;
		ProblemListCondition condition = new ProblemListCondition(
			"S1",
			"U1",
			"T1",
			ProblemListSort.RECENT,
			ProblemStatusFilter.ALL
		);
		Pageable pageable = PageRequest.of(0, 20);

		ProblemQueryTestModule m =
			new ProblemQueryTestModule(queryRepo, validator, listMapper, storage, detailMapper, userId, condition, pageable);

		List<ProblemListRow> rows = List.of(row(1L, "k1"), row(2L, "k2"));
		Page<ProblemListRow> page = new PageImpl<>(rows, (PageRequest) pageable, 2L);

		when(queryRepo.findMyProblemList(userId, condition, pageable)).thenReturn(page);

		when(storage.issueReadUrl(anyString(), isNull()))
			.thenAnswer(inv -> presigned("https://cdn/" + inv.getArgument(0)));

		when(listMapper.toResponse(any(), anyString()))
			.thenReturn(mock(ProblemListItemResponse.class));

		return m;
	}

	public static ProblemQueryTestModule invalidPaginationCase() {
		ProblemQueryRepository queryRepo = mock(ProblemQueryRepository.class);
		ProblemListRequestValidator validator = mock(ProblemListRequestValidator.class);
		ProblemListMapper listMapper = mock(ProblemListMapper.class);
		StorageService storage = mock(StorageService.class);
		ProblemDetailMapper detailMapper = mock(ProblemDetailMapper.class);

		Long userId = 10L;
		ProblemListCondition condition = new ProblemListCondition(
			"S1",
			"U1",
			"T1",
			ProblemListSort.RECENT,
			ProblemStatusFilter.ALL
		);
		Pageable pageable = PageRequest.of(0, 20);

		ProblemQueryTestModule m =
			new ProblemQueryTestModule(queryRepo, validator, listMapper, storage, detailMapper, userId, condition, pageable);

		RuntimeException ex = new RuntimeException("INVALID_PAGINATION");
		m.expected = ex;
		doThrow(ex).when(validator).validatePagination(pageable);

		return m;
	}

	public void call() {
		this.result = sut.getMyProblemCardList(userId, condition, pageable);
	}

	public void callExpectThrow() {
		try {
			sut.getMyProblemCardList(userId, condition, pageable);
			throw new AssertionError("expected exception");
		} catch (RuntimeException e) {
			if (expected == null) throw e;
		}
	}

	public void thenSuccess() {
		verify(validator).validatePagination(pageable);
		verify(queryRepo).findMyProblemList(userId, condition, pageable);
		verify(storage, times(2)).issueReadUrl(anyString(), isNull());
		verify(listMapper, times(2)).toResponse(any(), anyString());

		// list 조회에서는 상세 매퍼는 쓰지 않음
		verifyNoInteractions(detailMapper);
	}

	public void thenInvalidPagination() {
		verify(validator).validatePagination(pageable);
		verifyNoInteractions(queryRepo, storage, listMapper, detailMapper);
	}

	private static ProblemListRow row(Long problemId, String storageKey) {
		return new ProblemListRow(
			problemId,
			"S1",
			"SUBJECT",
			"U1",
			"UNIT",
			"T1",
			"TYPE",
			100L,
			storageKey,
			LocalDateTime.now()
		);
	}

	private static StoragePresignedGetData presigned(String url) {
		StoragePresignedGetData p = mock(StoragePresignedGetData.class);
		when(p.url()).thenReturn(url);
		return p;
	}
}
