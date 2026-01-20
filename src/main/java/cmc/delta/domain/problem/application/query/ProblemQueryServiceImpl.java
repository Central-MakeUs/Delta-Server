package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.common.exception.ProblemScanNotFoundException;
import cmc.delta.domain.problem.application.query.mapper.ProblemDetailMapper;
import cmc.delta.domain.problem.application.query.mapper.ProblemListMapper;
import cmc.delta.domain.problem.application.query.validation.ProblemListRequestValidator;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.problem.query.ProblemQueryRepository;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListRow;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.storage.StorageService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemQueryServiceImpl implements ProblemQueryService {

	private final ProblemListRequestValidator requestValidator;
	private final ProblemListMapper problemListMapper;
	private final StorageService storageService;
	private final ProblemQueryRepository problemQueryRepository;
	private final ProblemDetailMapper problemDetailMapper;

	@Override
	public PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		ProblemListCondition condition,
		Pageable pageable
	) {
		requestValidator.validatePagination(pageable);

		Page<ProblemListRow> rows = problemQueryRepository.findMyProblemList(userId, condition, pageable);
		List<ProblemListItemResponse> content = toProblemListItemResponses(rows.getContent());

		return new PagedResponse<ProblemListItemResponse>(
			content,
			rows.getNumber(),
			rows.getSize(),
			rows.getTotalElements(),
			rows.getTotalPages()
		);
	}

	@Override
	public ProblemDetailResponse getMyProblemDetail(Long userId, Long problemId) {
		ProblemDetailRow row = problemQueryRepository.findMyProblemDetail(userId, problemId)
			.orElseThrow(() -> new ProblemScanNotFoundException());

		StoragePresignedGetData presigned = storageService.issueReadUrl(row.storageKey(), null);
		return problemDetailMapper.toResponse(row, presigned.url());
	}

	private List<ProblemListItemResponse> toProblemListItemResponses(List<ProblemListRow> rows) {
		List<ProblemListItemResponse> result = new ArrayList<ProblemListItemResponse>(rows.size());

		for (ProblemListRow row : rows) {
			String previewImageUrl = issuePreviewImageUrl(row.storageKey());
			ProblemListItemResponse item = problemListMapper.toResponse(row, previewImageUrl);
			result.add(item);
		}
		return result;
	}

	private String issuePreviewImageUrl(String storageKey) {
		StoragePresignedGetData presigned = storageService.issueReadUrl(storageKey, null);
		return presigned.url();
	}
}
