package cmc.delta.domain.report.application.support;

import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeStatsRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemUnitStatsRow;
import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportAggregate.WeakArea;
import java.util.Comparator;
import java.util.List;

/**
 * 통계 행을 취약점 순위로 환산한다. 순수 함수 — AI 없이 숫자와 순위를 결정론적으로 계산한다.
 */
public final class ReportAggregator {

	private static final int TOP_N = 5;

	private ReportAggregator() {
	}

	public static ReportAggregate aggregate(List<ProblemUnitStatsRow> unitStats, List<ProblemTypeStatsRow> typeStats) {
		long total = unitStats.stream().mapToLong(ProblemUnitStatsRow::totalCount).sum();
		long solved = unitStats.stream().mapToLong(ProblemUnitStatsRow::solvedCount).sum();
		long unsolved = unitStats.stream().mapToLong(ProblemUnitStatsRow::unsolvedCount).sum();

		List<WeakArea> weakUnits = unitStats.stream()
			.map(r -> weakArea(r.unitId(), r.unitName(), r.unsolvedCount(), r.totalCount()))
			.filter(area -> area.unsolvedCount() > 0)
			.sorted(Comparator.comparingDouble(WeakArea::score).reversed())
			.limit(TOP_N)
			.toList();

		List<WeakArea> weakTypes = typeStats.stream()
			.map(r -> weakArea(r.typeId(), r.typeName(), r.unsolvedCount(), r.totalCount()))
			.filter(area -> area.unsolvedCount() > 0)
			.sorted(Comparator.comparingDouble(WeakArea::score).reversed())
			.limit(TOP_N)
			.toList();

		return new ReportAggregate(total, solved, unsolved, weakUnits, weakTypes);
	}

	private static WeakArea weakArea(String id, String name, long unsolvedCount, long totalCount) {
		double ratio = totalCount == 0 ? 0.0 : (double) unsolvedCount / totalCount;
		// ponytail: 취약도 점수는 미해결 볼륨을 비율로 가중한 휴리스틱. 정교한 IRT 모델이 필요해지면 여기만 교체.
		double score = unsolvedCount * (1.0 + ratio);
		return new WeakArea(id, name, totalCount, unsolvedCount, ratio, score);
	}
}
