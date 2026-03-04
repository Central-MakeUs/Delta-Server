package cmc.delta.domain.problem.adapter.out.ai.openai;

import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiClient implements AiClient, ProblemSolveAiClient {

	private static final String PATH_CHAT_COMPLETIONS = "/v1/chat/completions";
	private static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";

	private static final String FIELD_IS_MATH_PROBLEM = "is_math_problem";
	private static final String FIELD_PREDICTED_SUBJECT_ID = "predicted_subject_id";
	private static final String FIELD_PREDICTED_UNIT_ID = "predicted_unit_id";
	private static final String FIELD_PREDICTED_TYPE_ID = "predicted_type_id";
	private static final String FIELD_CONFIDENCE = "confidence";
	private static final String FIELD_SUBJECT_CANDIDATES = "subject_candidates";
	private static final String FIELD_UNIT_CANDIDATES = "unit_candidates";
	private static final String FIELD_TYPE_CANDIDATES = "type_candidates";

	private static final String FIELD_SOLUTION_LATEX = "solution_latex";
	private static final String FIELD_SOLUTION_TEXT = "solution_text";

	private static final int TEMPERATURE = 0;

	private final OpenAiProperties properties;
	private final ObjectMapper objectMapper;
	private final RestClient openAiRestClient;

	public OpenAiClient(
		OpenAiProperties properties,
		ObjectMapper objectMapper,
		@Qualifier("openAiRestClient")
		RestClient openAiRestClient) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.openAiRestClient = openAiRestClient;
	}

	public boolean isEnabled() {
		return properties.isConfigured();
	}

	@Override
	public AiCurriculumResult classifyCurriculum(AiCurriculumPrompt prompt) {
		if (!isEnabled()) {
			throw OpenAiAiException.promptBuildFailed(new IllegalStateException("OPENAI_API_KEY is not configured"));
		}

		try {
			String promptText = buildCurriculumPromptText(prompt);
			Map<String, Object> requestBody = buildCurriculumRequestBody(promptText);
			String rawResponseJson = requestOpenAi(requestBody);
			return parseCurriculumResponse(rawResponseJson);
		} catch (RestClientResponseException e) {
			throw OpenAiAiException.externalCallFailed(e);
		} catch (OpenAiAiException e) {
			throw e;
		} catch (Exception e) {
			throw OpenAiAiException.responseParseFailed(e);
		}
	}

	@Override
	public ProblemAiSolveResult solveProblem(ProblemAiSolvePrompt prompt) {
		if (!isEnabled()) {
			throw OpenAiAiException.promptBuildFailed(new IllegalStateException("OPENAI_API_KEY is not configured"));
		}

		try {
			String promptText = buildSolvePromptText(prompt);
			Map<String, Object> requestBody = buildSolveRequestBody(promptText, prompt.problemImageBytes(),
				prompt.problemImageMimeType());
			String rawResponseJson = requestOpenAi(requestBody);
			return parseSolveResponse(rawResponseJson);
		} catch (RestClientResponseException e) {
			throw OpenAiAiException.externalCallFailed(e);
		} catch (OpenAiAiException e) {
			throw e;
		} catch (Exception e) {
			throw OpenAiAiException.responseParseFailed(e);
		}
	}

	private String requestOpenAi(Map<String, Object> requestBody) {
		return openAiRestClient
			.post()
			.uri(PATH_CHAT_COMPLETIONS)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_BEARER_PREFIX + properties.apiKey())
			.body(requestBody)
			.retrieve()
			.body(String.class);
	}

	private Map<String, Object> buildCurriculumRequestBody(String promptText) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.model());
		body.put("temperature", TEMPERATURE);
		body.put("response_format", Map.of("type", "json_object"));
		body.put("messages", List.of(
			Map.of("role", "system", "content", "Return only one JSON object."),
			Map.of("role", "user", "content", promptText)));
		return body;
	}

	private Map<String, Object> buildSolveRequestBody(String promptText, byte[] imageBytes, String imageMimeType) {
		if (imageBytes == null || imageBytes.length == 0) {
			throw OpenAiAiException.promptBuildFailed(new IllegalArgumentException("Problem image bytes are empty"));
		}

		String safeMimeType = (imageMimeType == null || imageMimeType.isBlank()) ? "image/jpeg" : imageMimeType;
		String imageDataUrl = "data:" + safeMimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

		Map<String, Object> userMessage = new LinkedHashMap<>();
		userMessage.put("role", "user");
		userMessage.put("content", List.of(
			Map.of("type", "text", "text", promptText),
			Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.solveModel());
		body.put("temperature", TEMPERATURE);
		body.put("response_format", Map.of("type", "json_object"));
		body.put("messages", List.of(
			Map.of("role", "system", "content", "Return only one JSON object."),
			userMessage));
		return body;
	}

	private AiCurriculumResult parseCurriculumResponse(String rawResponseJson) {
		try {
			String modelText = extractMessageText(rawResponseJson);
			JsonNode root = objectMapper.readTree(extractJsonObject(modelText));

			boolean isMathProblem = root.path(FIELD_IS_MATH_PROBLEM).asBoolean(false);
			String subjectId = readTextOrNull(root, FIELD_PREDICTED_SUBJECT_ID);
			String unitId = readTextOrNull(root, FIELD_PREDICTED_UNIT_ID);
			String typeId = readTextOrNull(root, FIELD_PREDICTED_TYPE_ID);
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
				root.toString());
		} catch (OpenAiAiException e) {
			throw e;
		} catch (Exception e) {
			throw OpenAiAiException.responseParseFailed(e);
		}
	}

	private ProblemAiSolveResult parseSolveResponse(String rawResponseJson) {
		try {
			String modelText = extractMessageText(rawResponseJson);
			JsonNode root = objectMapper.readTree(extractJsonObject(modelText));
			String solutionLatex = readTextOrNull(root, FIELD_SOLUTION_LATEX);
			String solutionText = readTextOrNull(root, FIELD_SOLUTION_TEXT);
			return new ProblemAiSolveResult(solutionLatex, solutionText);
		} catch (OpenAiAiException e) {
			throw e;
		} catch (Exception e) {
			throw OpenAiAiException.responseParseFailed(e);
		}
	}

	private String extractMessageText(String rawResponseJson) {
		try {
			JsonNode root = objectMapper.readTree(rawResponseJson == null ? "{}" : rawResponseJson);
			String modelText = extractFirstNonBlankChoiceContent(root.path("choices"));
			if (modelText == null || modelText.isBlank()) {
				throw OpenAiAiException.emptyText();
			}
			return stripMarkdownCodeFence(modelText);
		} catch (OpenAiAiException e) {
			throw e;
		} catch (Exception e) {
			throw OpenAiAiException.responseParseFailed(e);
		}
	}

	private String extractFirstNonBlankChoiceContent(JsonNode choicesNode) {
		if (!choicesNode.isArray()) {
			return null;
		}

		for (JsonNode choiceNode : choicesNode) {
			JsonNode contentNode = choiceNode.path("message").path("content");
			String contentText = contentText(contentNode);
			if (contentText != null && !contentText.isBlank()) {
				return contentText;
			}
		}

		return null;
	}

	private String contentText(JsonNode contentNode) {
		if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
			return null;
		}
		if (contentNode.isTextual()) {
			return contentNode.asText(null);
		}
		if (!contentNode.isArray()) {
			return null;
		}

		StringBuilder textBuilder = new StringBuilder();
		for (JsonNode itemNode : contentNode) {
			String type = itemNode.path("type").asText("");
			if (!"text".equals(type)) {
				continue;
			}
			String text = itemNode.path("text").asText(null);
			if (text != null) {
				textBuilder.append(text);
			}
		}

		if (textBuilder.length() == 0) {
			return null;
		}
		return textBuilder.toString();
	}

	private String stripMarkdownCodeFence(String text) {
		String trimmed = text == null ? "" : text.trim();
		if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) {
			return trimmed;
		}
		String withoutPrefix = trimmed.replaceFirst("^```[a-zA-Z]*\\n", "");
		return withoutPrefix.replaceFirst("\\n```$", "").trim();
	}

	private String extractJsonObject(String text) {
		if (text == null || text.isBlank()) {
			throw OpenAiAiException.emptyText();
		}
		String normalizedText = text.trim();
		int startIndex = normalizedText.indexOf('{');
		int endIndex = normalizedText.lastIndexOf('}');
		if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
			throw OpenAiAiException.responseParseFailed(
				new IllegalArgumentException("JSON object is missing in OpenAI response"));
		}
		return normalizedText.substring(startIndex, endIndex + 1);
	}

	private String readTextOrNull(JsonNode node, String fieldName) {
		JsonNode valueNode = node.get(fieldName);
		if (valueNode == null || valueNode.isNull()) {
			return null;
		}
		String text = valueNode.asText(null);
		if (text == null || text.isBlank()) {
			return null;
		}
		return text;
	}

	private String buildCurriculumPromptText(AiCurriculumPrompt prompt) {
		try {
			String subjectsJson = objectMapper.writeValueAsString(prompt.subjects());
			String unitsJson = objectMapper.writeValueAsString(prompt.units());
			String typesJson = objectMapper.writeValueAsString(prompt.types());
			int mathLineCount = prompt.ocrSignals() == null ? 0 : prompt.ocrSignals().mathLineCount();
			int textLineCount = prompt.ocrSignals() == null ? 0 : prompt.ocrSignals().textLineCount();
			int codeLineCount = prompt.ocrSignals() == null ? 0 : prompt.ocrSignals().codeLineCount();
			int pseudocodeLineCount = prompt.ocrSignals() == null ? 0 : prompt.ocrSignals().pseudocodeLineCount();

			return OpenAiPromptTemplate.render(
				subjectsJson,
				unitsJson,
				typesJson,
				mathLineCount,
				textLineCount,
				codeLineCount,
				pseudocodeLineCount,
				prompt.ocrPlainText());
		} catch (Exception e) {
			throw OpenAiAiException.promptBuildFailed(e);
		}
	}

	private String buildSolvePromptText(ProblemAiSolvePrompt prompt) {
		try {
			String answerFormat = prompt.answerFormat() == null ? "UNKNOWN" : prompt.answerFormat().name();
			String answerValue = prompt.answerValue() == null ? "" : prompt.answerValue();
			String answerChoiceNo = prompt.answerChoiceNo() == null ? "null" : String.valueOf(prompt.answerChoiceNo());
			return OpenAiSolvePromptTemplate.render(answerFormat, answerValue, answerChoiceNo);
		} catch (Exception e) {
			throw OpenAiAiException.promptBuildFailed(e);
		}
	}
}
