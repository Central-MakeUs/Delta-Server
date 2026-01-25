package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.application.port.in.problem.result.ProblemDetailResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.problem.ProblemDetailMapper;
import cmc.delta.domain.problem.application.mapper.problem.ProblemListMapper;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemTypeTagQueryPort;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	private final ProblemListMapper problemListMapper;
	private final ProblemDetailMapper problemDetailMapper;

	/** 반환 타입은 use case result로 두고, web adapter는 단순 위임만 담당한다. */
	@Override
	public PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		ProblemListCondition condition,
		Pageable pageable
	) {
		validatePagination(pageable);

		Page<ProblemListRow> pageData = problemQueryPort.findMyProblemList(userId, condition, pageable);
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId = loadTypeItemsByProblemId(pageData);
		List<ProblemListItemResponse> items = mapListItems(pageData, typeItemsByProblemId);

		return PagedResponse.of(pageData, items);
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

	private void validatePagination(Pageable pageable) {
		requestValidator.validatePagination(pageable);
	}

	private Map<Long, List<CurriculumItemResponse>> loadTypeItemsByProblemId(Page<ProblemListRow> pageData) {
		List<Long> problemIds = pageData.getContent().stream().map(ProblemListRow::problemId).toList();
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
					Collectors.mapping(this::toTypeItem, Collectors.toList())
				)
			);
	}

	private List<ProblemListItemResponse> mapListItems(
		Page<ProblemListRow> pageData,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId
	) {
		return pageData.getContent().stream()
			.map(row -> toListItem(row, typeItemsByProblemId))
			.toList();
	}

	private ProblemListItemResponse toListItem(
		ProblemListRow row,
		Map<Long, List<CurriculumItemResponse>> typeItemsByProblemId
	) {
		String previewUrl = storagePort.issueReadUrl(row.storageKey());
		ProblemListItemResponse base = problemListMapper.toResponse(row, previewUrl);

		List<CurriculumItemResponse> types = typeItemsByProblemId.getOrDefault(base.problemId(), List.of());
		return new ProblemListItemResponse(
			base.problemId(),
			base.subject(),
			base.unit(),
			types,
			base.previewImage(),
			base.createdAt()
		);
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
			base.createdAt()
		);
	}
}
