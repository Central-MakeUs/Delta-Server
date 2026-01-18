package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.request.ProblemListSort;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.application.query.mapper.ProblemListMapper;
import cmc.delta.domain.problem.application.query.validation.ProblemListRequestValidator;
import cmc.delta.domain.problem.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.persistence.problem.ProblemQueryRepository;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListRow;
import cmc.delta.global.api.response.PagedResponse;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.storage.StorageService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemQueryServiceImpl implements ProblemQueryService {

	private final ProblemJpaRepository problemRepository;
	private final ProblemListRequestValidator requestValidator;
	private final ProblemListMapper problemListMapper;
	private final StorageService storageService;

	@Override
	public PagedResponse<ProblemListItemResponse> getMyProblemCardList(
		Long userId,
		String subjectId,
		String unitId,
		String typeId,
		ProblemListSort sort,
		int page,
		int size
	) {
		requestValidator.validatePagination(page, size);

		Pageable pageable = PageRequest.of(page, size);
		ProblemListCondition condition = new ProblemListCondition(subjectId, unitId, typeId, sort);

		Page<ProblemListRow> rows = problemRepository.findMyProblemList(userId, condition, pageable);

		List<ProblemListItemResponse> content = new ArrayList<ProblemListItemResponse>(rows.getNumberOfElements());
		for (ProblemListRow row : rows.getContent()) {
			StoragePresignedGetData presigned = storageService.issueReadUrl(row.getStorageKey(), null);
			ProblemListItemResponse response = problemListMapper.toResponse(row, presigned.url());
			content.add(response);
		}

		return new PagedResponse<ProblemListItemResponse>(
			content,
			rows.getNumber(),
			rows.getSize(),
			rows.getTotalElements(),
			rows.getTotalPages()
		);
	}
}
