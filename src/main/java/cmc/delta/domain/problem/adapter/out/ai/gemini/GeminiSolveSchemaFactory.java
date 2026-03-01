package cmc.delta.domain.problem.adapter.out.ai.gemini;

import java.util.List;
import java.util.Map;

final class GeminiSolveSchemaFactory {
	private GeminiSolveSchemaFactory() {}

	static Map<String, Object> responseSchema() {
		Map<String, Object> properties = Map.of(
			"solution_latex", Map.of("type", "string"),
			"solution_text", Map.of("type", "string"),
			"final_answer", Map.of("type", "string"));

		return Map.of(
			"type", "object",
			"properties", properties,
			"required", List.of("solution_latex", "solution_text", "final_answer"));
	}
}
