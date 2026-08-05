package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.domain.problem.adapter.out.ai.AiCurriculumResponseParser;
import cmc.delta.domain.problem.adapter.out.ai.CurriculumPromptTemplate;
import cmc.delta.domain.problem.application.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumPrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiAiClient implements AiClient {

	private static final String PATH_GENERATE_CONTENT = "/v1beta/models/{model}:generateContent";
	private static final String QUERY_KEY = "key";

	private static final long NANOS_PER_MILLISECOND = 1_000_000L;

	private static final Map<String, Object> RESPONSE_SCHEMA = GeminiSchemaFactory.responseSchema();

	private final GeminiProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient geminiRestClient;

	public GeminiAiClient(
		GeminiProperties props,
		ObjectMapper objectMapper,
		@Qualifier("geminiRestClient")
		RestClient geminiRestClient) {
		this.props = props;
		this.objectMapper = objectMapper;
		this.geminiRestClient = geminiRestClient;
	}

	@Override
	public AiCurriculumResult classifyCurriculum(AiCurriculumPrompt prompt) {
		long startedAtNanos = System.nanoTime();
		try {
			return requestAndParseClassify(prompt, startedAtNanos);
		} catch (RestClientResponseException e) {
			logClassifyHttpFailure(e, startedAtNanos);
			throw GeminiAiException.externalCallFailed(e);
		} catch (GeminiAiException e) {
			logClassifyFailure(e, startedAtNanos);
			throw e;
		} catch (Exception e) {
			logClassifyUnexpectedFailure(e, startedAtNanos);
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private AiCurriculumResult requestAndParseClassify(AiCurriculumPrompt prompt, long startedAtNanos) {
		String promptText = buildPromptText(prompt);
		Map<String, Object> requestBody = buildRequestBody(promptText);
		String rawResponseJson = callApi(requestBody);
		AiCurriculumResult result = parseResponse(rawResponseJson);
		log.debug("Gemini 분류 완료 model={} durationMs={}", props.model(), elapsedMillis(startedAtNanos));
		return result;
	}

	private void logClassifyHttpFailure(RestClientResponseException e, long startedAtNanos) {
		log.warn("Gemini 분류 HTTP 실패 model={} status={} durationMs={}",
			props.model(), e.getRawStatusCode(), elapsedMillis(startedAtNanos));
	}

	private void logClassifyFailure(GeminiAiException e, long startedAtNanos) {
		log.warn("Gemini 분류 실패 model={} reason={} durationMs={}",
			props.model(), e.getMessage(), elapsedMillis(startedAtNanos));
	}

	private void logClassifyUnexpectedFailure(Exception e, long startedAtNanos) {
		log.warn("Gemini 분류 예외 model={} reason={} durationMs={}",
			props.model(), e.getMessage(), elapsedMillis(startedAtNanos));
	}

	private String callApi(Map<String, Object> requestBody) {
		return geminiRestClient
			.post()
			.uri(uriBuilder -> uriBuilder
				.path(PATH_GENERATE_CONTENT)
				.queryParam(QUERY_KEY, props.apiKey())
				.build(props.model()))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(requestBody)
			.retrieve()
			.body(String.class);
	}

	private String buildPromptText(AiCurriculumPrompt prompt) {
		try {
			return CurriculumPromptTemplate.render(prompt, objectMapper);
		} catch (Exception e) {
			throw GeminiAiException.promptBuildFailed(e);
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
			JsonNode root = objectMapper.readTree(modelJsonText);
			return AiCurriculumResponseParser.parse(root, modelJsonText);
		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private String extractModelJsonText(String rawResponseJson) {
		try {
			JsonNode root = objectMapper.readTree(rawResponseJson == null ? "{}" : rawResponseJson);
			String modelText = readFirstPartText(root);
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

	private String readFirstPartText(JsonNode root) {
		JsonNode textNode = root.path("candidates")
			.path(0)
			.path("content")
			.path("parts")
			.path(0)
			.path("text");
		return textNode.isMissingNode() ? null : textNode.asText(null);
	}

	private long elapsedMillis(long startedAtNanos) {
		return (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND;
	}
}
