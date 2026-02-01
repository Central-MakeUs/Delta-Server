package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.problem.ProblemDetailMapper;
import cmc.delta.domain.problem.application.mapper.problem.ProblemListMapper;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemDetailResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemTypeTagQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.application.port.out.support.PageResult;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.global.api.response.CursorPagedResponse;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemQueryServiceImpl implements ProblemQueryUseCase {

	private final ProblemListRequestValidator requestValidator;
	private final ProblemQueryPort problemQueryPort;
	private final ProblemTypeTagQueryPort problemTypeTagQueryPort;
	private final StoragePort storagePort;
	private final ProblemScrollQueryService scrollQueryService;

	private final ProblemListMapper problemListMapper;
	private final ProblemDetailMapper problemDetailMapper;

	/** 반환 타입은 use case result로 두고, web adapter는 단순 위임만 담당한다. */
	@Override
	public PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		ProblemListCondition condition,
		PageQuery pageQuery) {
		validatePagination(pageQuery);

		PageResult<ProblemListRow> pageData = problemQueryPort.findMyProblemList(userId, condition, pageQuery);
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId = loadTypeItemsByProblemId(pageData);
		List<ProblemListItemResponse> items = mapListItems(pageData, typeItemsByProblemId);

		return PagedResponse.of(
			items,
			pageData.page(),
			pageData.size(),
			pageData.totalElements(),
			pageData.totalPages());
	}

	@Override
	public CursorPagedResponse<ProblemListItemResponse> getMyProblemCardListCursor(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery) {
		return getMyProblemCardListCursor(userId, condition, cursorQuery, true);
	}

	@Override
	public CursorPagedResponse<ProblemListItemResponse> getMyProblemCardListCursor(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery,
		boolean includePreviewUrl) {
		CursorPagedResponse<ProblemListItemResponse> base = scrollQueryService.getMyProblemCardListCursorBase(
			userId,
			condition,
			cursorQuery);

		if (!includePreviewUrl) {
			return base;
		}
		return scrollQueryService.attachPreviewUrls(base);
	}

	@Override
	public ProblemDetailResponse getMyProblemDetail(Long userId, Long problemId) {
		ProblemDetailRow row = problemQueryPort.findMyProblemDetail(userId, problemId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));

		String viewUrl = storagePort.issueReadUrl(row.storageKey());
		ProblemDetailResponse base = problemDetailMapper.toResponse(row, viewUrl);

		List<CurriculumItemResponse> types = loadTypeItems(problemId);
		return withTypes(base, types);
	}

	private void validatePagination(PageQuery pageQuery) {
		requestValidator.validatePagination(pageQuery);
	}

	private Map<Long, List<CurriculumItemResponse>> loadTypeItemsByProblemId(PageResult<ProblemListRow> pageData) {
		return loadTypeItemsByProblemId(pageData.content());
	}

	private Map<Long, List<CurriculumItemResponse>> loadTypeItemsByProblemId(List<ProblemListRow> rows) {
		List<Long> problemIds = rows.stream().map(ProblemListRow::problemId).toList();
		if (problemIds.isEmpty()) {
			return Map.of();
		}
		return groupTypeItemsByProblemId(problemIds);
	}

	private Map<Long, List<CurriculumItemResponse>> groupTypeItemsByProblemId(List<Long> problemIds) {
		List<ProblemTypeTagRow> rows = problemTypeTagQueryPort.findTypeTagsByProblemIds(problemIds);
		return rows.stream()
			.collect(
				Collectors.groupingBy(
					ProblemTypeTagRow::problemId,
					Collectors.mapping(this::toTypeItem, Collectors.toList())));
	}

	private List<ProblemListItemResponse> mapListItems(
		PageResult<ProblemListRow> pageData,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId) {
		return mapListItems(pageData.content(), typeItemsByProblemId, true);
	}

	private List<ProblemListItemResponse> mapListItems(
		List<ProblemListRow> rows,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId,
		boolean includePreviewUrl) {
		return rows.stream()
			.map(row -> toListItem(row, typeItemsByProblemId, includePreviewUrl))
			.toList();
	}

	private ProblemListItemResponse toListItem(
		ProblemListRow row,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId,
		boolean includePreviewUrl) {
		String previewUrl = includePreviewUrl ? storagePort.issueReadUrl(row.storageKey()) : null;
		ProblemListItemResponse base = problemListMapper.toResponse(row, previewUrl);

		List<CurriculumItemResponse> types = typeItemsByProblemId.getOrDefault(base.problemId(), List.of());
		return new ProblemListItemResponse(
			base.problemId(),
			base.subject(),
			base.unit(),
			types,
			base.previewImage(),
			base.isCompleted(),
			base.createdAt());
	}

	private List<CurriculumItemResponse> loadTypeItems(Long problemId) {
		return problemTypeTagQueryPort.findTypeTagsByProblemId(problemId).stream()
			.map(this::toTypeItem)
			.toList();
	}

	private CurriculumItemResponse toTypeItem(ProblemTypeTagRow r) {
		return new CurriculumItemResponse(r.typeId(), r.typeName());
	}

	private ProblemDetailResponse withTypes(ProblemDetailResponse base, List<CurriculumItemResponse> types) {
		return new ProblemDetailResponse(
			base.problemId(),
			base.subject(),
			base.unit(),
			types,
			base.originalImage(),
			base.answerFormat(),
			base.answerChoiceNo(),
			base.answerValue(),
			base.solutionText(),
			base.completed(),
			base.completedAt(),
			base.createdAt());
	}
}
