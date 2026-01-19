package cmc.delta.domain.problem.application.query.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.query.ProblemQueryServiceImpl;
import cmc.delta.domain.problem.application.query.mapper.ProblemListMapper;
import cmc.delta.domain.problem.application.query.validation.ProblemListRequestValidator;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListRow;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.storage.StorageService;
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

	private final ProblemJpaRepository repo;
	private final ProblemListRequestValidator validator;
	private final ProblemListMapper mapper;
	private final StorageService storage;

	private PagedResponse<ProblemListItemResponse> result;
	private RuntimeException expected;

	private ProblemQueryTestModule(
		ProblemJpaRepository repo,
		ProblemListRequestValidator validator,
		ProblemListMapper mapper,
		StorageService storage,
		Long userId,
		ProblemListCondition condition,
		Pageable pageable
	) {
		this.repo = repo;
		this.validator = validator;
		this.mapper = mapper;
		this.storage = storage;

		this.userId = userId;
		this.condition = condition;
		this.pageable = pageable;

		this.sut = new ProblemQueryServiceImpl(repo, validator, mapper, storage);
	}

	public static ProblemQueryTestModule successCase() {
		ProblemJpaRepository repo = mock(ProblemJpaRepository.class);
		ProblemListRequestValidator validator = mock(ProblemListRequestValidator.class);
		ProblemListMapper mapper = mock(ProblemListMapper.class);
		StorageService storage = mock(StorageService.class);

		Long userId = 10L;
		ProblemListCondition condition = new ProblemListCondition(
			"S1",
			"U1",
			"T1",
			ProblemListSort.RECENT,
			ProblemStatusFilter.ALL
		);
		Pageable pageable = PageRequest.of(0, 20);

		ProblemQueryTestModule m = new ProblemQueryTestModule(repo, validator, mapper, storage, userId, condition, pageable);

		List<ProblemListRow> rows = List.of(row("k1"), row("k2"));
		Page<ProblemListRow> page = new PageImpl<>(rows, (PageRequest) pageable, 2L);

		org.mockito.Mockito.when(repo.findMyProblemList(userId, condition, pageable)).thenReturn(page);

		org.mockito.Mockito.when(storage.issueReadUrl(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.isNull()))
			.thenAnswer(inv -> presigned("https://cdn/" + inv.getArgument(0)));

		org.mockito.Mockito.when(mapper.toResponse(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
			.thenReturn(mock(ProblemListItemResponse.class));

		return m;
	}

	public static ProblemQueryTestModule invalidPaginationCase() {
		ProblemJpaRepository repo = mock(ProblemJpaRepository.class);
		ProblemListRequestValidator validator = mock(ProblemListRequestValidator.class);
		ProblemListMapper mapper = mock(ProblemListMapper.class);
		StorageService storage = mock(StorageService.class);

		Long userId = 10L;
		ProblemListCondition condition = new ProblemListCondition(
			"S1",
			"U1",
			"T1",
			ProblemListSort.RECENT,
			ProblemStatusFilter.ALL
		);

		Pageable pageable = PageRequest.of(0, 20);

		ProblemQueryTestModule m = new ProblemQueryTestModule(repo, validator, mapper, storage, userId, condition, pageable);

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
		org.mockito.Mockito.verify(validator).validatePagination(pageable);
		org.mockito.Mockito.verify(repo).findMyProblemList(userId, condition, pageable);
		org.mockito.Mockito.verify(storage, org.mockito.Mockito.times(2)).issueReadUrl(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.isNull());
		org.mockito.Mockito.verify(mapper, org.mockito.Mockito.times(2)).toResponse(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
	}

	public void thenInvalidPagination() {
		org.mockito.Mockito.verify(validator).validatePagination(pageable);
		org.mockito.Mockito.verifyNoInteractions(repo, storage, mapper);
	}

	private static ProblemListRow row(String storageKey) {
		ProblemListRow r = mock(ProblemListRow.class);
		org.mockito.Mockito.when(r.getStorageKey()).thenReturn(storageKey);
		return r;
	}

	private static StoragePresignedGetData presigned(String url) {
		StoragePresignedGetData p = mock(StoragePresignedGetData.class);
		org.mockito.Mockito.when(p.url()).thenReturn(url);
		return p;
	}
}
