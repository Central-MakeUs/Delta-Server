package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.query.mapper.ProblemListMapper;
import cmc.delta.domain.problem.application.query.validation.ProblemListRequestValidator;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListRow;
import cmc.delta.global.api.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemQueryServiceImpl implements ProblemQueryService {

	private final ProblemJpaRepository problemRepository;
	private final ProblemListRequestValidator validator;
	private final ProblemListMapper mapper;

	@Override
	public PagedResponse<ProblemListItemResponse> getMyProblemList(
		Long userId,
		String subjectId,
		String unitId,
		String typeId,
		ProblemListSort sort,
		int page,
		int size
	) {
		validator.validatePagination(page, size);

		Pageable pageable = PageRequest.of(page, size);
		ProblemListCondition condition = new ProblemListCondition(subjectId, unitId, typeId, sort);

		Page<ProblemListRow> rows = problemRepository.findMyProblemList(userId, condition, pageable);
		return PagedResponse.of(rows, mapper::toResponse);
	}
}
