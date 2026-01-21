package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.mapper.problem.ProblemDetailMapper;
import cmc.delta.domain.problem.application.mapper.problem.ProblemListMapper;
import cmc.delta.domain.problem.application.port.in.problem.ProblemQueryUseCase;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.validation.query.ProblemListRequestValidator;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListRow;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
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

		return PagedResponse.of(pageData, row -> {
			String previewUrl = storagePort.issueReadUrl(row.storageKey());
			return problemListMapper.toResponse(row, previewUrl);
		});
	}

	@Override
	public ProblemDetailResponse getMyProblemDetail(Long userId, Long problemId) {
		ProblemDetailRow row = problemQueryPort.findMyProblemDetail(userId, problemId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));

		String viewUrl = storagePort.issueReadUrl(row.storageKey());
		return problemDetailMapper.toResponse(row, viewUrl);
	}
}
