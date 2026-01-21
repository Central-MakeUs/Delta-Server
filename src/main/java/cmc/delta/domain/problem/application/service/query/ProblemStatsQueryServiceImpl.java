package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemStatsResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemUnitStatsItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemStatsCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.stats.dto.ProblemUnitStatsRow;
import cmc.delta.domain.problem.application.port.in.problem.ProblemStatsUseCase;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemStatsQueryPort;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemStatsQueryServiceImpl implements ProblemStatsUseCase {

	private final ProblemStatsQueryPort problemStatsQueryPort;

	@Override
	public ProblemStatsResponse<ProblemUnitStatsItemResponse> getUnitStats(Long userId, ProblemStatsCondition condition) {
		List<ProblemUnitStatsRow> rows = problemStatsQueryPort.findUnitStats(userId, condition);

		List<ProblemUnitStatsItemResponse> items = new ArrayList<>(rows.size());
		for (ProblemUnitStatsRow r : rows) {
			items.add(toUnitItem(r));
		}

		return new ProblemStatsResponse<>(items);
	}

	@Override
	public ProblemStatsResponse<ProblemTypeStatsItemResponse> getTypeStats(Long userId, ProblemStatsCondition condition) {
		List<ProblemTypeStatsRow> rows = problemStatsQueryPort.findTypeStats(userId, condition);

		List<ProblemTypeStatsItemResponse> items = new ArrayList<>(rows.size());
		for (ProblemTypeStatsRow r : rows) {
			items.add(toTypeItem(r));
		}

		return new ProblemStatsResponse<>(items);
	}

	private ProblemUnitStatsItemResponse toUnitItem(ProblemUnitStatsRow r) {
		return new ProblemUnitStatsItemResponse(
			item(r.subjectId(), r.subjectName()),
			item(r.unitId(), r.unitName()),
			r.solvedCount(),
			r.unsolvedCount(),
			r.totalCount()
		);
	}

	private ProblemTypeStatsItemResponse toTypeItem(ProblemTypeStatsRow r) {
		return new ProblemTypeStatsItemResponse(
			item(r.typeId(), r.typeName()),
			r.solvedCount(),
			r.unsolvedCount(),
			r.totalCount()
		);
	}

	private CurriculumItemResponse item(String id, String name) {
		return new CurriculumItemResponse(id, name);
	}
}
