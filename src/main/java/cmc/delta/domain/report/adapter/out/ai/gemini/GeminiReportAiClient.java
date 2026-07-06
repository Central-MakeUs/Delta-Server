package cmc.delta.domain.report.adapter.out.ai.gemini;

import cmc.delta.domain.problem.adapter.out.ai.AiResponseParseUtils;
import cmc.delta.domain.report.adapter.out.ai.ReportPromptTemplate;
import cmc.delta.domain.report.application.dto.ReportAggregate;
import cmc.delta.domain.report.application.dto.ReportNarrative;
import cmc.delta.domain.report.application.dto.ReportNarrative.UnitComment;
import cmc.delta.domain.report.application.dto.WrongAnswerSample;
import cmc.delta.domain.report.application.port.out.ai.ReportAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 텍스트(집계 + 오답 샘플) → 구조화 JSON 서술 분석. 기존 geminiRestClient 빈을 재사용한다.
 */
@Slf4j
@Component
public class GeminiReportAiClient implements ReportAiClient {

	private static final int MAX_OUTPUT_TOKENS = 4096;
	private static final int THINKING_BUDGET = -1;
	private static final String PATH_GENERATE_CONTENT = "/v1beta/models/{model}:generateContent";
	private static final String QUERY_KEY = "key";

	private static final String FIELD_DIAGNOSIS = "diagnosis";
	private static final String FIELD_UNIT_COMMENTS = "unit_comments";
	private static final String FIELD_UNIT_NAME = "unit_name";
	private static final String FIELD_COMMENT = "comment";
	private static final String FIELD_STUDY_DIRECTION = "study_direction";
	private static final String FIELD_RECOMMENDATIONS = "recommendations";

	private final RestClient geminiRestClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	public GeminiReportAiClient(
		@Qualifier("geminiRestClient") RestClient geminiRestClient,
		ObjectMapper objectMapper,
		@Value("${gemini.api-key}") String apiKey,
		@Value("${gemini.report-model:${gemini.model:gemini-3.1-flash-lite}}") String model) {
		this.geminiRestClient = geminiRestClient;
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public ReportNarrative analyze(ReportAggregate aggregate, List<WrongAnswerSample> samples) {
		String promptText = ReportPromptTemplate.render(aggregate, samples);
		String rawResponseJson = callApi(buildRequestBody(promptText));
		return parseResponse(rawResponseJson);
	}

	private String callApi(Map<String, Object> requestBody) {
		return geminiRestClient
			.post()
			.uri(uriBuilder -> uriBuilder
				.path(PATH_GENERATE_CONTENT)
				.queryParam(QUERY_KEY, apiKey)
				.build(model))
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.body(requestBody)
			.retrieve()
			.body(String.class);
	}

	private Map<String, Object> buildRequestBody(String promptText) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(Map.of(
			"role", "user",
			"parts", List.of(Map.of("text", promptText)))));

		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("temperature", 0);
		generationConfig.put("maxOutputTokens", MAX_OUTPUT_TOKENS);
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", GeminiReportSchemaFactory.responseSchema());
		generationConfig.put("thinkingConfig", Map.of("thinkingBudget", THINKING_BUDGET));
		body.put("generationConfig", generationConfig);
		return body;
	}

	private ReportNarrative parseResponse(String rawResponseJson) {
		try {
			String modelText = extractModelText(rawResponseJson);
			JsonNode root = objectMapper.readTree(AiResponseParseUtils.stripMarkdownCodeFence(modelText));
			return new ReportNarrative(
				AiResponseParseUtils.readTextOrNull(root, FIELD_DIAGNOSIS),
				readUnitComments(root.get(FIELD_UNIT_COMMENTS)),
				AiResponseParseUtils.readTextOrNull(root, FIELD_STUDY_DIRECTION),
				readStringList(root.get(FIELD_RECOMMENDATIONS)));
		} catch (Exception e) {
			throw new IllegalStateException("REPORT_AI_RESPONSE_PARSE_FAILED", e);
		}
	}

	private String extractModelText(String rawResponseJson) throws Exception {
		JsonNode parts = objectMapper.readTree(rawResponseJson)
			.path("candidates").path(0).path("content").path("parts");
		StringBuilder sb = new StringBuilder();
		for (JsonNode part : parts) {
			JsonNode text = part.get("text");
			if (text != null && !text.isNull()) {
				sb.append(text.asText());
			}
		}
		if (sb.length() == 0) {
			throw new IllegalStateException("REPORT_AI_EMPTY_TEXT");
		}
		return sb.toString();
	}

	private List<UnitComment> readUnitComments(JsonNode node) {
		List<UnitComment> comments = new ArrayList<>();
		if (node == null || !node.isArray()) {
			return comments;
		}
		for (JsonNode item : node) {
			comments.add(new UnitComment(
				AiResponseParseUtils.readTextOrNull(item, FIELD_UNIT_NAME),
				AiResponseParseUtils.readTextOrNull(item, FIELD_COMMENT)));
		}
		return comments;
	}

	private List<String> readStringList(JsonNode node) {
		List<String> values = new ArrayList<>();
		if (node == null || !node.isArray()) {
			return values;
		}
		for (JsonNode item : node) {
			if (!item.isNull()) {
				values.add(item.asText());
			}
		}
		return values;
	}
}
