package cmc.delta.domain.problem.adapter.out.ai;

import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import com.fasterxml.jackson.databind.JsonNode;

public final class AiCurriculumResponseParser {

	public static final String FIELD_IS_MATH_PROBLEM = "is_math_problem";
	public static final String FIELD_PREDICTED_SUBJECT_ID = "predicted_subject_id";
	public static final String FIELD_PREDICTED_UNIT_ID = "predicted_unit_id";
	public static final String FIELD_PREDICTED_TYPE_ID = "predicted_type_id";
	public static final String FIELD_CONFIDENCE = "confidence";
	public static final String FIELD_SUBJECT_CANDIDATES = "subject_candidates";
	public static final String FIELD_UNIT_CANDIDATES = "unit_candidates";
	public static final String FIELD_TYPE_CANDIDATES = "type_candidates";

	private AiCurriculumResponseParser() {}

	public static AiCurriculumResult parse(JsonNode root, String aiDraftJson) {
		return new AiCurriculumResult(
			root.path(FIELD_IS_MATH_PROBLEM).asBoolean(false),
			readPredictedId(root, FIELD_PREDICTED_SUBJECT_ID),
			readPredictedId(root, FIELD_PREDICTED_UNIT_ID),
			readPredictedId(root, FIELD_PREDICTED_TYPE_ID),
			root.path(FIELD_CONFIDENCE).asDouble(0.0),
			readCandidatesJson(root, FIELD_SUBJECT_CANDIDATES),
			readCandidatesJson(root, FIELD_UNIT_CANDIDATES),
			readCandidatesJson(root, FIELD_TYPE_CANDIDATES),
			aiDraftJson);
	}

	private static String readPredictedId(JsonNode root, String fieldName) {
		return AiResponseParseUtils.readTextOrNull(root, fieldName);
	}

	private static String readCandidatesJson(JsonNode root, String fieldName) {
		return root.path(fieldName).toString();
	}
}
