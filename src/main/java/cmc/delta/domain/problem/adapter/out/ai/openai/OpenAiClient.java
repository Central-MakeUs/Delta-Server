package cmc.delta.domain.problem.adapter.out.ai.openai;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cmc.delta.domain.problem.adapter.out.ai.AiCurriculumResponseParser;
import cmc.delta.domain.problem.adapter.out.ai.AiResponseParseUtils;
import cmc.delta.domain.problem.adapter.out.ai.CurriculumPromptTemplate;
import cmc.delta.domain.problem.adapter.out.ai.SolvePromptTemplate;
import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;

@Component
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiClient implements AiClient, ProblemSolveAiClient {

	private static final String PATH_CHAT_COMPLETIONS = "/v1/chat/completions";
	private static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";
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
		requireEnabled();
		try {
			String promptText = buildCurriculumPromptText(prompt);
			String rawResponseJson = requestOpenAi(buildCurriculumRequestBody(promptText));
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
		requireEnabled();
		try {
			String promptText = SolvePromptTemplate.render();
			String rawResponseJson = requestOpenAi(
				buildSolveRequestBody(promptText, prompt.problemImageBytes(), prompt.problemImageMimeType()));
			return parseSolveResponse(rawResponseJson);
		} catch (RestClientResponseException e) {
			throw OpenAiAiException.externalCallFailed(e);
		} catch (OpenAiAiException e) {
			throw e;
		} catch (Exception e) {
			throw OpenAiAiException.responseParseFailed(e);
		}
	}

	private void requireEnabled() {
		if (!isEnabled()) {
			throw OpenAiAiException.promptBuildFailed(new IllegalStateException("OPENAI_API_KEY is not configured"));
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
			return AiCurriculumResponseParser.parse(root, root.toString());
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
			String solutionLatex = AiResponseParseUtils.readTextOrNull(root, FIELD_SOLUTION_LATEX);
			String solutionText = AiResponseParseUtils.readTextOrNull(root, FIELD_SOLUTION_TEXT);
			if (AiResponseParseUtils.shouldPreferLatexText(solutionLatex, solutionText)) {
				solutionText = solutionLatex;
			}
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
			return AiResponseParseUtils.stripMarkdownCodeFence(modelText);
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
			if (!"text".equals(itemNode.path("type").asText(""))) {
				continue;
			}
			String text = itemNode.path("text").asText(null);
			if (text != null) {
				textBuilder.append(text);
			}
		}
		return textBuilder.length() == 0 ? null : textBuilder.toString();
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

	private String buildCurriculumPromptText(AiCurriculumPrompt prompt) {
		try {
			return CurriculumPromptTemplate.render(prompt, objectMapper);
		} catch (Exception e) {
			throw OpenAiAiException.promptBuildFailed(e);
		}
	}
}
