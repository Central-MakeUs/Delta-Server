package cmc.delta.domain.problem.adapter.out.ai;

import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrSignalSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CurriculumPromptTemplate {

	private CurriculumPromptTemplate() {
	}

	private static final String TEMPLATE = """
		You are a Korean high school math problem classifier.
		Given the OCR text below, select one each from the candidate lists:
		1) subject
		2) unit
		3) type

		First, decide whether the OCR text is a math problem.
		- If not a math problem: is_math_problem=false
		- If it is a math problem: is_math_problem=true

		Rules:
		- Use only ids from the candidate lists.
		- Ignore handwriting or scribbles; focus on the printed problem content.
		- If confidence is low, set a lower confidence value and recommend 3 candidates per category with scores.
		- Return exactly one JSON object, no explanation.
		- If is_math_problem=false, keep the JSON format but set classification ids to empty string and confidence to 0.

		[Output JSON format]
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

		[Subject candidates]
		%s

		[Unit candidates]
		%s

		[Type candidates]
		%s

		[OCR structured signals]
		- math_line_count: %d
		- text_line_count: %d
		- code_line_count: %d
		- pseudocode_line_count: %d

		[OCR text]
		%s
		""";

	public static String render(AiCurriculumPrompt prompt, ObjectMapper objectMapper)
		throws JsonProcessingException {
		String subjectsJson = objectMapper.writeValueAsString(prompt.subjects());
		String unitsJson = objectMapper.writeValueAsString(prompt.units());
		String typesJson = objectMapper.writeValueAsString(prompt.types());

		OcrSignalSummary signals = prompt.ocrSignals();
		int mathLineCount = signals == null ? 0 : signals.mathLineCount();
		int textLineCount = signals == null ? 0 : signals.textLineCount();
		int codeLineCount = signals == null ? 0 : signals.codeLineCount();
		int pseudocodeLineCount = signals == null ? 0 : signals.pseudocodeLineCount();

		return TEMPLATE.formatted(
			subjectsJson, unitsJson, typesJson,
			mathLineCount, textLineCount, codeLineCount, pseudocodeLineCount,
			prompt.ocrPlainText());
	}
}
