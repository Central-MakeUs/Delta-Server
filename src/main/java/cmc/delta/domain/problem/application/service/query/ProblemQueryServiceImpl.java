package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.dto.ProblemDetailRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.type.dto.ProblemTypeTagRow;
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
import java.util.Collections;
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
	private final ProblemTypeTagQueryPort problemTypeTagQueryPort; // ✅ 추가
	private final StoragePort storagePort;

	private final ProblemListMapper problemListMapper;
	private final ProblemDetailMapper problemDetailMapper;

	@Override
	public PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		ProblemListCondition condition,
		Pageable pageable
	) {
		requestValidator.validatePagination(pageable);

		Page<ProblemListRow> pageData = problemQueryPort.findMyProblemList(userId, condition, pageable);

		// ✅ 1) 현재 페이지의 problemIds 추출
		List<Long> problemIds = pageData.getContent().stream()
			.map(ProblemListRow::problemId) // record accessor
			.toList();

		// ✅ 2) 한방 조회 → (problemId -> types) 맵 만들기
		Map<Long, List<CurriculumItemResponse>> typesMap = loadTypesMap(problemIds);

		// ✅ 3) 기존 mapper 결과에 types만 채워서 record 재생성
		List<ProblemListItemResponse> items = pageData.getContent().stream()
			.map(row -> {
				String previewUrl = storagePort.issueReadUrl(row.storageKey());
				ProblemListItemResponse base = problemListMapper.toResponse(row, previewUrl);

				List<CurriculumItemResponse> types =
					typesMap.getOrDefault(base.problemId(), List.of());

				return new ProblemListItemResponse(
					base.problemId(),
					base.subject(),
					base.unit(),
					base.type(),     // 대표 타입(기존 유지)
					types,           // ✅ 추가
					base.previewImage(),
					base.createdAt()
				);
			})
			.toList();

		return PagedResponse.of(pageData, items);
	}

	@Override
	public ProblemDetailResponse getMyProblemDetail(Long userId, Long problemId) {
		ProblemDetailRow row = problemQueryPort.findMyProblemDetail(userId, problemId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));

		String viewUrl = storagePort.issueReadUrl(row.storageKey());
		ProblemDetailResponse base = problemDetailMapper.toResponse(row, viewUrl);

		List<CurriculumItemResponse> types = problemTypeTagQueryPort.findTypeTagsByProblemId(problemId).stream()
			.map(r -> new CurriculumItemResponse(r.typeId(), r.typeName()))
			.toList();

		return new ProblemDetailResponse(
			base.problemId(),
			base.subject(),
			base.unit(),
			base.type(),  // 대표 타입 유지
			types,        // ✅ 추가
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

	private Map<Long, List<CurriculumItemResponse>> loadTypesMap(List<Long> problemIds) {
		if (problemIds == null || problemIds.isEmpty()) return Collections.emptyMap();

		List<ProblemTypeTagRow> rows = problemTypeTagQueryPort.findTypeTagsByProblemIds(problemIds);

		return rows.stream()
			.collect(Collectors.groupingBy(
				ProblemTypeTagRow::problemId,
				Collectors.mapping(
					r -> new CurriculumItemResponse(r.typeId(), r.typeName()),
					Collectors.toList()
				)
			));
	}
}
