package cmc.delta.domain.problem.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;

import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;

public final class AiCurriculumResponseParser {

	public static final String FIELD_IS_MATH_PROBLEM = "is_math_problem";
	public static final String FIELD_PREDICTED_SUBJECT_ID = "predicted_subject_id";
	public static final String FIELD_PREDICTED_UNIT_ID = "predicted_unit_id";
	public static final String FIELD_PREDICTED_TYPE_ID = "predicted_type_id";
	public static final String FIELD_CONFIDENCE = "confidence";
	public static final String FIELD_SUBJECT_CANDIDATES = "subject_candidates";
	public static final String FIELD_UNIT_CANDIDATES = "unit_candidates";
	public static final String FIELD_TYPE_CANDIDATES = "type_candidates";

	private AiCurriculumResponseParser() {
	}

	public static AiCurriculumResult parse(JsonNode root, String aiDraftJson) {
		boolean isMathProblem = root.path(FIELD_IS_MATH_PROBLEM).asBoolean(false);
		String subjectId = AiResponseParseUtils.readTextOrNull(root, FIELD_PREDICTED_SUBJECT_ID);
		String unitId = AiResponseParseUtils.readTextOrNull(root, FIELD_PREDICTED_UNIT_ID);
		String typeId = AiResponseParseUtils.readTextOrNull(root, FIELD_PREDICTED_TYPE_ID);
		double confidence = root.path(FIELD_CONFIDENCE).asDouble(0.0);
		String subjectCandidatesJson = root.path(FIELD_SUBJECT_CANDIDATES).toString();
		String unitCandidatesJson = root.path(FIELD_UNIT_CANDIDATES).toString();
		String typeCandidatesJson = root.path(FIELD_TYPE_CANDIDATES).toString();

		return new AiCurriculumResult(
			isMathProblem,
			subjectId,
			unitId,
			typeId,
			confidence,
			subjectCandidatesJson,
			unitCandidatesJson,
			typeCandidatesJson,
			aiDraftJson);
	}
}
