package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@EnableConfigurationProperties(GeminiProperties.class)
@RequiredArgsConstructor
public class GeminiAiClient implements AiClient {

	private static final String PATH_GENERATE_CONTENT = "/v1beta/models/{model}:generateContent";
	private static final String QUERY_KEY = "key";

	private static final String FIELD_PREDICTED_SUBJECT_ID = "predicted_subject_id";
	private static final String FIELD_PREDICTED_UNIT_ID = "predicted_unit_id";
	private static final String FIELD_PREDICTED_TYPE_ID = "predicted_type_id";
	private static final String FIELD_CONFIDENCE = "confidence";
	private static final String FIELD_SUBJECT_CANDIDATES = "subject_candidates";
	private static final String FIELD_UNIT_CANDIDATES = "unit_candidates";
	private static final String FIELD_TYPE_CANDIDATES = "type_candidates";

	private static final Map<String, Object> RESPONSE_SCHEMA = GeminiSchemaFactory.responseSchema();

	private static final int DEFAULT_TEMPERATURE = 0;

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
					.path(PATH_GENERATE_CONTENT)
					.queryParam(QUERY_KEY, props.apiKey())
					.build(props.model()))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(requestBody)
				.retrieve()
				.body(String.class);

			return parseResponse(rawResponseJson);

		} catch (RestClientResponseException e) {
			throw GeminiAiException.externalCallFailed(e);
		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private Map<String, Object> buildRequestBody(String promptText) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(
			Map.of("parts", List.of(Map.of("text", promptText)))));
		body.put("generationConfig", Map.of(
			"temperature", 0,
			"responseMimeType", "application/json",
			"responseSchema", RESPONSE_SCHEMA));
		return body;
	}

	private AiCurriculumResult parseResponse(String rawResponseJson) {
		try {
			String modelJsonText = extractModelJsonText(rawResponseJson);
			JsonNode out = objectMapper.readTree(modelJsonText);

			String subjectId = readTextOrNull(out, FIELD_PREDICTED_SUBJECT_ID);
			String unitId = readTextOrNull(out, FIELD_PREDICTED_UNIT_ID);
			String typeId = readTextOrNull(out, FIELD_PREDICTED_TYPE_ID);
			double confidence = out.path(FIELD_CONFIDENCE).asDouble(0.0);

			String subjectCandidatesJson = out.path(FIELD_SUBJECT_CANDIDATES).toString();
			String unitCandidatesJson = out.path(FIELD_UNIT_CANDIDATES).toString();
			String typeCandidatesJson = out.path(FIELD_TYPE_CANDIDATES).toString();

			return new AiCurriculumResult(
				subjectId,
				unitId,
				typeId,
				confidence,
				subjectCandidatesJson,
				unitCandidatesJson,
				typeCandidatesJson,
				modelJsonText);
		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private String extractModelJsonText(String rawResponseJson) {
		try {
			JsonNode root = objectMapper.readTree(rawResponseJson == null ? "{}" : rawResponseJson);
			JsonNode textNode = root.path("candidates")
				.path(0)
				.path("content")
				.path("parts")
				.path(0)
				.path("text");

			String modelText = textNode.isMissingNode() ? null : textNode.asText(null);
			if (modelText == null || modelText.isBlank()) {
				throw GeminiAiException.emptyText();
			}
			return modelText;

		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private String readTextOrNull(JsonNode node, String fieldName) {
		JsonNode v = node.get(fieldName);
		if (v == null || v.isNull())
			return null;
		String text = v.asText(null);
		return (text == null || text.isBlank()) ? null : text;
	}

	private String buildPromptText(AiCurriculumPrompt prompt) {
		try {
			String subjectsJson = objectMapper.writeValueAsString(prompt.subjects());
			String unitsJson = objectMapper.writeValueAsString(prompt.units());
			String typesJson = objectMapper.writeValueAsString(prompt.types());

			return GeminiPromptTemplate.render(subjectsJson, unitsJson, typesJson, prompt.ocrPlainText());
		} catch (Exception e) {
			throw GeminiAiException.promptBuildFailed(e);
		}
	}
}
