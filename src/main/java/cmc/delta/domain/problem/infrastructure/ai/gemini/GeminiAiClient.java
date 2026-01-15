package cmc.delta.domain.problem.infrastructure.ai.gemini;

import cmc.delta.domain.problem.application.port.ai.AiClient;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.ai.AiCurriculumResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@EnableConfigurationProperties(GeminiProperties.class)
@RequiredArgsConstructor
public class GeminiAiClient implements AiClient {

	private final GeminiProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient geminiRestClient;

	@Override
	public AiCurriculumResult classifyCurriculum(AiCurriculumPrompt prompt) {
		try {
			String promptText = buildPromptText(prompt);
			Map<String, Object> requestBody = buildRequestBody(promptText);

			String rawResponseJson = geminiRestClient
				.post()
				.uri(uriBuilder -> uriBuilder
					.path("/v1beta/models/{model}:generateContent")
					.queryParam("key", props.apiKey())
					.build(props.model()))
				.header("Content-Type", "application/json")
				.body(requestBody)
				.retrieve()
				.body(String.class);

			return parseResponse(rawResponseJson);

		} catch (RestClientResponseException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("GEMINI_AI_FAILED", e);
		}
	}

	private Map<String, Object> buildRequestBody(String promptText) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(
			Map.of("parts", List.of(Map.of("text", promptText)))
		));

		body.put("generationConfig", Map.of(
			"temperature", 0,
			"responseMimeType", "application/json",
			"responseSchema", responseSchema()
		));

		return body;
	}

	private AiCurriculumResult parseResponse(String rawResponseJson) throws Exception {
		String modelJsonText = extractModelJsonText(rawResponseJson);
		JsonNode out = objectMapper.readTree(modelJsonText);

		String subjectId = readTextOrNull(out, "predicted_subject_id");
		String unitId = readTextOrNull(out, "predicted_unit_id");
		String typeId = readTextOrNull(out, "predicted_type_id");
		double confidence = out.path("confidence").asDouble(0.0);

		String subjectCandidatesJson = out.path("subject_candidates").toString();
		String unitCandidatesJson = out.path("unit_candidates").toString();
		String typeCandidatesJson = out.path("type_candidates").toString();

		return new AiCurriculumResult(
			subjectId,
			unitId,
			typeId,
			confidence,
			subjectCandidatesJson,
			unitCandidatesJson,
			typeCandidatesJson,
			modelJsonText
		);
	}

	private String extractModelJsonText(String rawResponseJson) throws Exception {
		JsonNode root = objectMapper.readTree(rawResponseJson);
		JsonNode textNode = root.path("candidates")
			.path(0)
			.path("content")
			.path("parts")
			.path(0)
			.path("text");

		String modelText = textNode.isMissingNode() ? null : textNode.asText(null);
		if (modelText == null || modelText.isBlank()) {
			throw new IllegalStateException("GEMINI_EMPTY_TEXT");
		}
		return modelText;
	}

	private String readTextOrNull(JsonNode node, String fieldName) {
		JsonNode v = node.get(fieldName);
		if (v == null || v.isNull()) return null;
		String text = v.asText(null);
		return (text == null || text.isBlank()) ? null : text;
	}

	/**
	 * GEMINI AI용 프롬프트 생성
	 * Todo: 나중에 분류
	 */
	private String buildPromptText(AiCurriculumPrompt prompt) throws Exception {
		// 후보 목록은 id/name만 보내서 토큰 절약
		String subjectsJson = objectMapper.writeValueAsString(prompt.subjects());
		String unitsJson = objectMapper.writeValueAsString(prompt.units());
		String typesJson = objectMapper.writeValueAsString(prompt.types());

		return """
			너는 한국 고등학교 수학 문제 분류기다.
			입력 OCR 텍스트를 보고, 아래의 후보 목록 중에서
			1) 과목(subject)
			2) 단원(unit)
			3) 유형(type)
			을 각각 하나씩 고른다.

			규칙:
			- 반드시 후보 목록의 id만 사용한다.
			- 손글씨/낙서로 보이는 부분은 무시하고, 인쇄된 문제 내용 중심으로 판단한다.
			- 확신이 낮으면 confidence를 낮게 주고, 각 분류마다 후보 3개를 score와 함께 추천한다.
			- 출력은 반드시 JSON 한 덩어리만 반환한다(설명 문장 금지).

			[출력 JSON 형식]
			{
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

			[OCR 텍스트]
			%s
			""".formatted(subjectsJson, unitsJson, typesJson, prompt.ocrPlainText());
	}

	private Map<String, Object> responseSchema() {
		Map<String, Object> candidateArraySchema = Map.of(
			"type", "ARRAY",
			"items", Map.of(
				"type", "OBJECT",
				"properties", Map.of(
					"id", Map.of("type", "STRING"),
					"score", Map.of("type", "NUMBER")
				),
				"required", List.of("id", "score")
			)
		);

		return Map.of(
			"type", "OBJECT",
			"properties", Map.of(
				"predicted_subject_id", Map.of("type", "STRING"),
				"predicted_unit_id", Map.of("type", "STRING"),
				"predicted_type_id", Map.of("type", "STRING"),
				"confidence", Map.of("type", "NUMBER"),
				"subject_candidates", candidateArraySchema,
				"unit_candidates", candidateArraySchema,
				"type_candidates", candidateArraySchema
			),
			"required", List.of(
				"predicted_subject_id",
				"predicted_unit_id",
				"predicted_type_id",
				"confidence",
				"subject_candidates",
				"unit_candidates",
				"type_candidates"
			)
		);
	}
}
