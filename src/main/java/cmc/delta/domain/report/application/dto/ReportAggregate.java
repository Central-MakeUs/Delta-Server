package cmc.delta.domain.report.application.dto;

import java.util.List;

/**
 * 취약점 정량 집계 결과. 숫자·순위는 전부 통계 쿼리에서 결정론적으로 계산되며 AI가 바꾸지 못한다.
 */
public record ReportAggregate(
	long totalCount,
	long solvedCount,
	long unsolvedCount,
	List<WeakArea> weakUnits,
	List<WeakArea> weakTypes) {

	/**
	 * 취약 영역 한 칸(단원 또는 유형). score 는 볼륨(미해결 수)과 미해결 비율을 결합한 순위용 지표.
	 */
	public record WeakArea(
		String id,
		String name,
		long totalCount,
		long unsolvedCount,
		double unsolvedRatio,
		double score) {
	}
}
