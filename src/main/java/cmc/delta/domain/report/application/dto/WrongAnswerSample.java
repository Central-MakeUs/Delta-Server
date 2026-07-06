package cmc.delta.domain.report.application.dto;

/**
 * AI 정성 분석에 넘기는 오답 문제 샘플. 단원/유형명과 문제 본문, 사용자 메모를 담는다.
 */
public record WrongAnswerSample(
	String unitName,
	String typeName,
	String problemMarkdown,
	String memoText) {
}
