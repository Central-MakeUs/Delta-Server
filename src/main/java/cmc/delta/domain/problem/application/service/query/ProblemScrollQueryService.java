package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.mapper.problem.ProblemListMapper;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemTypeTagQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.application.port.out.support.CursorPageResult;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.global.api.response.CursorPagedResponse;
import cmc.delta.global.cache.CacheNames;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemScrollQueryService {

	private final ProblemListRequestValidator requestValidator;
	private final ProblemQueryPort problemQueryPort;
	private final ProblemTypeTagQueryPort problemTypeTagQueryPort;
	private final StoragePort storagePort;
	private final ProblemListMapper problemListMapper;

	@Cacheable(
		cacheNames = CacheNames.WRONG_ANSWER_PAGES,
		keyGenerator = "problemScrollKeyGenerator",
		sync = true)
	public CursorPagedResponse<ProblemListItemResponse> getMyProblemCardListCursorBase(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery) {
		validateCursorPagination(condition, cursorQuery);

		CursorPageResult<ProblemListRow> pageData = problemQueryPort.findMyProblemListCursor(userId, condition,
			cursorQuery);
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId = loadTypeItemsByProblemId(pageData.content());
		List<ProblemListItemResponse> items = mapListItemsNoPreview(pageData.content(), typeItemsByProblemId);

		CursorPagedResponse.Cursor nextCursor = (pageData.nextLastId() == null)
			? null
			: new CursorPagedResponse.Cursor(pageData.nextLastId(), pageData.nextLastCreatedAt());

		return CursorPagedResponse.of(items, pageData.hasNext(), nextCursor, pageData.totalElements());
	}

	public CursorPagedResponse<ProblemListItemResponse> attachPreviewUrls(
		CursorPagedResponse<ProblemListItemResponse> base) {
		List<ProblemListItemResponse> withPreview = base.content().stream()
			.map(this::attachPreviewUrl)
			.toList();

		return CursorPagedResponse.of(withPreview, base.hasNext(), base.nextCursor(), base.totalElements());
	}

	private ProblemListItemResponse attachPreviewUrl(ProblemListItemResponse item) {
		ProblemListItemResponse.PreviewImageResponse preview = item.previewImage();
		if (preview == null) {
			return item;
		}
		if (preview.storageKey() == null || preview.storageKey().isBlank()) {
			return item;
		}
		if (preview.viewUrl() != null) {
			return item;
		}

		String viewUrl = storagePort.issueReadUrl(preview.storageKey());
		ProblemListItemResponse.PreviewImageResponse nextPreview = new ProblemListItemResponse.PreviewImageResponse(
			preview.assetId(),
			preview.storageKey(),
			viewUrl);

		return new ProblemListItemResponse(
			item.problemId(),
			item.subject(),
			item.unit(),
			item.types(),
			nextPreview,
			item.isCompleted(),
			item.createdAt());
	}

	private void validateCursorPagination(ProblemListCondition condition, CursorQuery cursorQuery) {
		requestValidator.validatePagination(new PageQuery(0, cursorQuery.size()));

		boolean hasLastId = cursorQuery.lastId() != null;
		boolean hasLastCreatedAt = cursorQuery.lastCreatedAt() != null;
		if (hasLastId != hasLastCreatedAt) {
			throw new ProblemValidationException(ErrorCode.INVALID_REQUEST);
		}

		if (condition.sort() != null) {
			switch (condition.sort()) {
				case RECENT, OLDEST -> {
				}
				default -> throw new ProblemValidationException(ErrorCode.INVALID_REQUEST);
			}
		}
	}

	private Map<Long, List<CurriculumItemResponse>> loadTypeItemsByProblemId(List<ProblemListRow> rows) {
		List<Long> problemIds = rows.stream().map(ProblemListRow::problemId).toList();
		if (problemIds.isEmpty()) {
			return Map.of();
		}
		List<ProblemTypeTagRow> tags = problemTypeTagQueryPort.findTypeTagsByProblemIds(problemIds);
		return tags.stream()
			.collect(Collectors.groupingBy(
				ProblemTypeTagRow::problemId,
				Collectors.mapping(this::toTypeItem, Collectors.toList())));
	}

	private CurriculumItemResponse toTypeItem(ProblemTypeTagRow r) {
		return new CurriculumItemResponse(r.typeId(), r.typeName());
	}

	private List<ProblemListItemResponse> mapListItemsNoPreview(
		List<ProblemListRow> rows,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId) {
		return rows.stream()
			.map(row -> toListItemNoPreview(row, typeItemsByProblemId))
			.toList();
	}

	private ProblemListItemResponse toListItemNoPreview(
		ProblemListRow row,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId) {
		ProblemListItemResponse base = problemListMapper.toResponse(row, null);
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
}
