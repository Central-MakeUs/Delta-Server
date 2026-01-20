package cmc.delta.domain.problem.application.query;

import cmc.delta.domain.problem.api.problem.dto.response.*;
import cmc.delta.domain.problem.persistence.problem.query.ProblemStatsQueryRepository;
import cmc.delta.domain.problem.persistence.problem.query.dto.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemStatsQueryServiceImpl implements ProblemStatsQueryService {

	private final ProblemStatsQueryRepository statsRepository;

	@Override
	public ProblemStatsResponse<ProblemUnitStatsItemResponse> getUnitStats(Long userId, ProblemStatsCondition condition) {
		List<ProblemUnitStatsRow> rows = statsRepository.findUnitStats(userId, condition);
		List<ProblemUnitStatsItemResponse> items = new ArrayList<>(rows.size());

		for (ProblemUnitStatsRow r : rows) {
			items.add(new ProblemUnitStatsItemResponse(
				new CurriculumItemResponse(r.subjectId(), r.subjectName()),
				new CurriculumItemResponse(r.unitId(), r.unitName()),
				r.solvedCount(),
				r.unsolvedCount(),
				r.totalCount()
			));
		}

		return new ProblemStatsResponse<>(items);
	}

	@Override
	public ProblemStatsResponse<ProblemTypeStatsItemResponse> getTypeStats(Long userId, ProblemStatsCondition condition) {
		List<ProblemTypeStatsRow> rows = statsRepository.findTypeStats(userId, condition);
		List<ProblemTypeStatsItemResponse> items = new ArrayList<>(rows.size());

		for (ProblemTypeStatsRow r : rows) {
			items.add(new ProblemTypeStatsItemResponse(
				new CurriculumItemResponse(r.typeId(), r.typeName()),
				r.solvedCount(),
				r.unsolvedCount(),
				r.totalCount()
			));
		}

		return new ProblemStatsResponse<>(items);
	}
}
