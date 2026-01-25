package cmc.delta.domain.problem.adapter.out.ai.gemini;

import java.util.List;
import java.util.Map;

final class GeminiSchemaFactory {
	private GeminiSchemaFactory() {}

	static Map<String, Object> responseSchema() {
		Map<String, Object> candidateArraySchema = Map.of(
			"type", "ARRAY",
			"items", Map.of(
				"type", "OBJECT",
				"properties", Map.of(
					"id", Map.of("type", "STRING"),
					"score", Map.of("type", "NUMBER")),
				"required", List.of("id", "score")));

		return Map.of(
			"type", "OBJECT",
			"properties", Map.of(
				"predicted_subject_id", Map.of("type", "STRING"),
				"predicted_unit_id", Map.of("type", "STRING"),
				"predicted_type_id", Map.of("type", "STRING"),
				"confidence", Map.of("type", "NUMBER"),
				"subject_candidates", candidateArraySchema,
				"unit_candidates", candidateArraySchema,
				"type_candidates", candidateArraySchema),
			"required", List.of(
				"predicted_subject_id",
				"predicted_unit_id",
				"predicted_type_id",
				"confidence",
				"subject_candidates",
				"unit_candidates",
				"type_candidates"));
	}
}
