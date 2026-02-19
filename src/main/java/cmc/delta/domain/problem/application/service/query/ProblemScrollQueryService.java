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

	@Cacheable(cacheNames = CacheNames.WRONG_ANSWER_PAGES, keyGenerator = "problemScrollKeyGenerator", sync = true)
	public CursorPagedResponse<ProblemListItemResponse> getMyProblemCardListCursorBase(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery) {
		validateCursorPagination(condition, cursorQuery);

		CursorPageResult<ProblemListRow> pageData = problemQueryPort.findMyProblemListCursor(userId, condition,
			cursorQuery);
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId = loadTypeItemsByProblemId(pageData.content());
		List<ProblemListItemResponse> items = mapListItemsNoPreview(pageData.content(), typeItemsByProblemId);
		return CursorPagedResponse.of(
			items,
			pageData.hasNext(),
			buildNextCursor(pageData),
			pageData.totalElements());
	}

	public CursorPagedResponse<ProblemListItemResponse> attachPreviewUrls(
		CursorPagedResponse<ProblemListItemResponse> response) {
		Map<String, String> previewUrls = loadPreviewUrls(response.content());
		if (previewUrls.isEmpty()) {
			return response;
		}
		List<ProblemListItemResponse> withPreview = applyPreviewUrls(response.content(), previewUrls);

		return CursorPagedResponse.of(withPreview, response.hasNext(), response.nextCursor(), response.totalElements());
	}

	private ProblemListItemResponse attachPreviewUrl(
		ProblemListItemResponse item,
		Map<String, String> previewUrls) {
		ProblemListItemResponse.PreviewImageResponse preview = item.previewImage();
		if (!needsPreviewUrl(preview)) {
			return item;
		}

		String viewUrl = previewUrls.get(preview.storageKey());
		if (viewUrl == null) {
			return item;
		}
		ProblemListItemResponse.PreviewImageResponse nextPreview = new ProblemListItemResponse.PreviewImageResponse(
			preview.storageKey(),
			viewUrl);
		return item.withTypesAndPreview(item.types(), nextPreview);
	}

	private List<String> extractMissingPreviewKeys(List<ProblemListItemResponse> items) {
		return items.stream()
			.map(ProblemListItemResponse::previewImage)
			.filter(this::needsPreviewUrl)
			.map(ProblemListItemResponse.PreviewImageResponse::storageKey)
			.toList();
	}

	private Map<String, String> loadPreviewUrls(List<ProblemListItemResponse> items) {
		List<String> storageKeys = extractMissingPreviewKeys(items);
		if (storageKeys.isEmpty()) {
			return Map.of();
		}
		return storagePort.issueReadUrls(storageKeys);
	}

	private List<ProblemListItemResponse> applyPreviewUrls(
		List<ProblemListItemResponse> items,
		Map<String, String> previewUrls) {
		return items.stream()
			.map(item -> attachPreviewUrl(item, previewUrls))
			.toList();
	}

	private CursorPagedResponse.Cursor buildNextCursor(CursorPageResult<ProblemListRow> pageData) {
		if (pageData.nextLastId() == null) {
			return null;
		}
		return new CursorPagedResponse.Cursor(pageData.nextLastId(), pageData.nextLastCreatedAt());
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
				case RECENT, OLDEST -> {}
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
		return base.withTypesAndPreview(types, base.previewImage());
	}

	private boolean needsPreviewUrl(ProblemListItemResponse.PreviewImageResponse preview) {
		return preview != null
			&& preview.viewUrl() == null
			&& preview.storageKey() != null
			&& !preview.storageKey().isBlank();
	}

}
