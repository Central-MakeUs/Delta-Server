package cmc.delta.domain.report.adapter.out.ai.gemini;

import java.util.List;
import java.util.Map;

final class GeminiReportSchemaFactory {

	private GeminiReportSchemaFactory() {
	}

	static Map<String, Object> responseSchema() {
		Map<String, Object> unitComment = Map.of(
			"type", "object",
			"properties", Map.of(
				"unit_name", Map.of("type", "string"),
				"comment", Map.of("type", "string")),
			"required", List.of("unit_name", "comment"));

		Map<String, Object> properties = Map.of(
			"diagnosis", Map.of("type", "string"),
			"unit_comments", Map.of("type", "array", "items", unitComment),
			"study_direction", Map.of("type", "string"),
			"recommendations", Map.of("type", "array", "items", Map.of("type", "string")));

		return Map.of(
			"type", "object",
			"properties", properties,
			"required", List.of("diagnosis", "unit_comments", "study_direction", "recommendations"));
	}
}
