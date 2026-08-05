package cmc.delta.domain.report.adapter.out.ai;

import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportAggregate.WeakArea;
import cmc.delta.domain.report.application.dto.WrongAnswerSample;
import java.util.List;

/**
 * 취약점 정량 집계 + 오답 샘플을 한국어 분석 프롬프트로 렌더링한다.
 */
public final class ReportPromptTemplate {

	private static final int MARKDOWN_LIMIT = 400;
	private static final int MEMO_LIMIT = 200;

	private static final String INSTRUCTION = """
		너는 한국 중고등 수학 학습 코치다. 아래는 한 학생의 오답 노트를 단원/유형별로 집계한 결과와 실제 오답 샘플이다.
		이 데이터를 근거로 학생의 취약점을 진단하고 학습 방향을 제안하라.

		규칙:
		- 정확히 하나의 JSON 객체만 출력한다. 코드펜스(```)를 쓰지 마라.
		- 모든 문자열은 한국어로 작성한다.
		- 집계 숫자와 순위는 이미 확정된 사실이다. 숫자를 새로 지어내지 말고 해석만 하라.
		- diagnosis: 반복되는 오답 패턴과 원인을 2~4문장으로 진단한다.
		- unit_comments: 취약 단원별로 왜 약한지 한 줄 코멘트를 단다.
		- study_direction: 앞으로의 학습 방향을 2~3문장으로 제시한다.
		- recommendations: 구체적인 다음 학습 행동을 3개 내외의 짧은 항목으로 제안한다.

		출력 JSON 형식:
		{
		  "diagnosis": "...",
		  "unit_comments": [{"unit_name": "...", "comment": "..."}],
		  "study_direction": "...",
		  "recommendations": ["...", "..."]
		}
		""";

	private ReportPromptTemplate() {}

	public static String render(ReportAggregate aggregate, List<WrongAnswerSample> samples) {
		StringBuilder sb = new StringBuilder(INSTRUCTION);
		appendOverallStatus(sb, aggregate);

		sb.append("\n[취약 단원 순위]\n");
		appendWeakAreas(sb, aggregate.weakUnits());

		sb.append("\n[취약 유형 순위]\n");
		appendWeakAreas(sb, aggregate.weakTypes());

		appendSamples(sb, samples);
		return sb.toString();
	}

	private static void appendOverallStatus(StringBuilder sb, ReportAggregate aggregate) {
		sb.append("\n[전체 현황] 총 오답 ").append(aggregate.totalCount())
			.append("개, 해결 ").append(aggregate.solvedCount())
			.append("개, 미해결 ").append(aggregate.unsolvedCount()).append("개\n");
	}

	private static void appendSamples(StringBuilder sb, List<WrongAnswerSample> samples) {
		sb.append("\n[오답 샘플]\n");
		int index = 1;
		for (WrongAnswerSample sample : samples) {
			sb.append(index++).append(". 단원=").append(nullToDash(sample.unitName()))
				.append(", 유형=").append(nullToDash(sample.typeName())).append("\n")
				.append("   문제: ").append(truncate(sample.problemMarkdown(), MARKDOWN_LIMIT)).append("\n")
				.append("   메모: ").append(truncate(sample.memoText(), MEMO_LIMIT)).append("\n");
		}
	}

	private static void appendWeakAreas(StringBuilder sb, List<WeakArea> areas) {
		if (areas.isEmpty()) {
			sb.append("- (없음)\n");
			return;
		}
		for (WeakArea area : areas) {
			sb.append("- ").append(area.name())
				.append(": 미해결 ").append(area.unsolvedCount())
				.append("/").append(area.totalCount())
				.append(" (").append(Math.round(area.unsolvedRatio() * 100)).append("%)\n");
		}
	}

	private static String truncate(String text, int limit) {
		if (text == null || text.isBlank()) {
			return "(없음)";
		}
		String compact = text.replace("\n", " ").trim();
		return compact.length() <= limit ? compact : compact.substring(0, limit) + "...";
	}

	private static String nullToDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
