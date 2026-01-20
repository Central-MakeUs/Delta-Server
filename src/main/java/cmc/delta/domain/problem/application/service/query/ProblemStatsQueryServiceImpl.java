package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemStatsResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemUnitStatsItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.ProblemStatsQueryRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemUnitStatsRow;

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
