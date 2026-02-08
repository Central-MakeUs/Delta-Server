package cmc.delta.domain.problem.adapter.out.ai.gemini;

final class GeminiPromptTemplate {
	private GeminiPromptTemplate() {}

	private static final String TEMPLATE = """
		너는 한국 고등학교 수학 문제 분류기다.
		입력 OCR 텍스트를 보고, 아래의 후보 목록 중에서
		1) 과목(subject)
		2) 단원(unit)
		3) 유형(type)
		을 각각 하나씩 고른다.

		먼저, 이 OCR 텍스트가 '수학 문제'인지 판단한다.
		- 수학 문제가 아니면 is_math_problem=false
		- 수학 문제면 is_math_problem=true

		규칙:
		- 반드시 후보 목록의 id만 사용한다.
		- 손글씨/낙서로 보이는 부분은 무시하고, 인쇄된 문제 내용 중심으로 판단한다.
		- 확신이 낮으면 confidence를 낮게 주고, 각 분류마다 후보 3개를 score와 함께 추천한다.
		- 출력은 반드시 JSON 한 덩어리만 반환한다(설명 문장 금지).
		- is_math_problem=false인 경우에도 출력 JSON 형식은 유지하되, 분류 id는 빈 문자열로 두고 confidence는 0으로 둔다.

		[출력 JSON 형식]
		{
		  "is_math_problem": true,
		  "predicted_subject_id": "subject_id",
		  "predicted_unit_id": "unit_id",
		  "predicted_type_id": "type_id",
		  "confidence": 0.0,
		  "subject_candidates": [{"id":"...", "score":0.0},{"id":"...", "score":0.0},{"id":"...", "score":0.0}],
		  "unit_candidates": [{"id":"...", "score":0.0},{"id":"...", "score":0.0},{"id":"...", "score":0.0}],
		  "type_candidates": [{"id":"...", "score":0.0},{"id":"...", "score":0.0},{"id":"...", "score":0.0}]
		}

		[후보 과목 목록]
		%s

		[후보 단원 목록]
		%s

		[후보 유형 목록]
		%s

		[OCR 구조화 신호]
		- math_line_count: %d
		- text_line_count: %d
		- code_line_count: %d
		- pseudocode_line_count: %d

		[OCR 텍스트]
		%s
		""";

	static String render(
		String subjectsJson,
		String unitsJson,
		String typesJson,
		int mathLineCount,
		int textLineCount,
		int codeLineCount,
		int pseudocodeLineCount,
		String ocrPlainText) {
		return TEMPLATE.formatted(
			subjectsJson,
			unitsJson,
			typesJson,
			mathLineCount,
			textLineCount,
			codeLineCount,
			pseudocodeLineCount,
			ocrPlainText);
	}
}
