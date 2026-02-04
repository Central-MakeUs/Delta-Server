package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.curriculum.application.port.out.ProblemTypeLoadPort;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.port.in.problem.ProblemStatsUseCase;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemMonthlyProgressResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemStatsResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemTypeStatsItemResponse;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemUnitStatsItemResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemStatsQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemMonthlyProgressRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemUnitStatsRow;
import cmc.delta.domain.problem.application.validation.query.ProblemMonthlyProgressValidator;
import cmc.delta.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemStatsQueryServiceImpl implements ProblemStatsUseCase {

	private static final String EMPTY = "";
	private static final int DAY_ONE = 1;
	private static final int ONE_MONTH = 1;

	private final ProblemStatsQueryPort problemStatsQueryPort;
	private final ProblemTypeLoadPort problemTypeLoadPort;
	private final ProblemMonthlyProgressValidator monthlyProgressValidator;

	@Override
	public ProblemStatsResponse<ProblemUnitStatsItemResponse> getUnitStats(Long userId,
		ProblemStatsCondition condition) {
		List<ProblemUnitStatsRow> rows = problemStatsQueryPort.findUnitStats(userId, condition);
		return new ProblemStatsResponse<>(mapUnitItems(rows));
	}

	@Override
	public ProblemStatsResponse<ProblemTypeStatsItemResponse> getTypeStats(Long userId,
		ProblemStatsCondition condition) {
		validateTypeFilter(userId, condition);
		List<ProblemTypeStatsRow> rows = problemStatsQueryPort.findTypeStats(userId, condition);
		return new ProblemStatsResponse<>(mapTypeItems(rows));
	}

	@Override
	public ProblemMonthlyProgressResponse getMonthlyProgress(Long userId, Integer year, Integer month) {
		YearMonth yearMonth = monthlyProgressValidator.validateAndParse(year, month);
		MonthlyRange range = buildMonthlyRange(yearMonth);
		ProblemMonthlyProgressRow row = problemStatsQueryPort.findMonthlyProgress(userId, range.from(), range.to());
		return new ProblemMonthlyProgressResponse(
			yearMonth.toString(),
			row.totalCount(),
			row.solvedCount(),
			row.unsolvedCount());
	}

	private void validateTypeFilter(Long userId, ProblemStatsCondition condition) {
		String typeId = condition.typeId();
		if (typeId == null || typeId.trim().equals(EMPTY)) {
			return;
		}
		problemTypeLoadPort.findActiveVisibleById(userId, typeId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_FINAL_TYPE_NOT_FOUND));
	}

	private List<ProblemUnitStatsItemResponse> mapUnitItems(List<ProblemUnitStatsRow> rows) {
		List<ProblemUnitStatsItemResponse> items = new ArrayList<>(rows.size());
		for (ProblemUnitStatsRow row : rows) {
			items.add(toUnitItem(row));
		}
		return items;
	}

	private List<ProblemTypeStatsItemResponse> mapTypeItems(List<ProblemTypeStatsRow> rows) {
		List<ProblemTypeStatsItemResponse> items = new ArrayList<>(rows.size());
		for (ProblemTypeStatsRow row : rows) {
			items.add(toTypeItem(row));
		}
		return items;
	}

	private ProblemUnitStatsItemResponse toUnitItem(ProblemUnitStatsRow r) {
		return new ProblemUnitStatsItemResponse(
			item(r.subjectId(), r.subjectName()),
			item(r.unitId(), r.unitName()),
			r.solvedCount(),
			r.unsolvedCount(),
			r.totalCount());
	}

	private ProblemTypeStatsItemResponse toTypeItem(ProblemTypeStatsRow r) {
		return new ProblemTypeStatsItemResponse(
			item(r.typeId(), r.typeName()),
			r.solvedCount(),
			r.unsolvedCount(),
			r.totalCount());
	}

	private CurriculumItemResponse item(String id, String name) {
		return new CurriculumItemResponse(id, name);
	}

	private MonthlyRange buildMonthlyRange(YearMonth yearMonth) {
		LocalDateTime from = yearMonth.atDay(DAY_ONE).atStartOfDay();
		LocalDateTime to = yearMonth.plusMonths(ONE_MONTH).atDay(DAY_ONE).atStartOfDay();
		return new MonthlyRange(from, to);
	}

	private record MonthlyRange(LocalDateTime from, LocalDateTime to) {
	}
}
