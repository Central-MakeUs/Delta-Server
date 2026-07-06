package cmc.delta.domain.report.application.dto;

import java.time.LocalDateTime;

/**
 * 사용자 오답셋의 경량 지문. 오답 추가/수정/완료 시 값이 바뀌므로 리포트 재생성 판단(inputHash)에 사용한다.
 */
public record ReportSourceSignature(
	long totalCount,
	long solvedCount,
	LocalDateTime lastUpdatedAt) {
}
