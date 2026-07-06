package cmc.delta.domain.report.application.dto;

import java.util.List;

/**
 * AI가 생성하는 정성 분석. 정량 집계 위에 "왜/어떻게"만 서술한다.
 */
public record ReportNarrative(
	String diagnosis,
	List<UnitComment> unitComments,
	String studyDirection,
	List<String> recommendations) {

	public record UnitComment(
		String unitName,
		String comment) {
	}
}
