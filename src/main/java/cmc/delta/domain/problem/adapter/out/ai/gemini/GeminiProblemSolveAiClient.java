package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.HashMap;
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
public class GeminiProblemSolveAiClient implements ProblemSolveAiClient {

	private static final int LOG_TEXT_LIMIT = 1200;
	private static final int SOLVE_MAX_OUTPUT_TOKENS = 8192;
	private static final int SOLVE_THINKING_BUDGET = 0;
	private static final int MALFORMED_FIELD_MAX_LENGTH = 8000;
	private static final int MAX_JSON_REPAIR_SCAN_LENGTH = 24000;
	private static final int MAX_ACCEPTABLE_SOLUTION_TEXT_LENGTH = 12000;
	private static final int REPEATED_LINE_MIN_LENGTH = 20;
	private static final int MAX_SAME_LINE_OCCURRENCES = 4;
	private static final int REPEATED_LINE_RATIO_MIN_LINES = 12;
	private static final int REPEATED_LINE_RATIO_PERCENT = 35;

	private static final String PATH_GENERATE_CONTENT = "/v1beta/models/{model}:generateContent";
	private static final String QUERY_KEY = "key";

	private static final String FIELD_SOLUTION_LATEX = "solution_latex";
	private static final String FIELD_SOLUTION_TEXT = "solution_text";
	private static final String FIELD_FINAL_ANSWER = "final_answer";
	private static final String KEY_SOLUTION_LATEX = "\"solution_latex\"";
	private static final String KEY_SOLUTION_TEXT = "\"solution_text\"";
	private static final String KEY_FINAL_ANSWER = "\"final_answer\"";
	private static final Map<String, Object> RESPONSE_SCHEMA = GeminiSolveSchemaFactory.responseSchema();

	private final GeminiProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient geminiRestClient;

	public GeminiProblemSolveAiClient(
		GeminiProperties props,
		ObjectMapper objectMapper,
		@Qualifier("geminiRestClient")
		RestClient geminiRestClient) {
		this.props = props;
		this.objectMapper = objectMapper;
		this.geminiRestClient = geminiRestClient;
	}

	@Override
	public ProblemAiSolveResult solveProblem(ProblemAiSolvePrompt prompt) {
		try {
			PreparedPrompt preparedPrompt = buildPromptText(prompt);
			Map<String, Object> requestBody = buildRequestBody(
				preparedPrompt.promptText(),
				prompt.problemImageBytes(),
				prompt.problemImageMimeType());
			log.debug(
				"Gemini 풀이 요청 model={} imageBytes={} imageMimeType={} promptLength={}",
				props.solveModel(),
				prompt.problemImageBytes() == null ? 0 : prompt.problemImageBytes().length,
				prompt.problemImageMimeType(),
				preparedPrompt.promptText().length());

			String rawResponseJson = geminiRestClient
				.post()
				.uri(uriBuilder -> uriBuilder
					.path(PATH_GENERATE_CONTENT)
					.queryParam(QUERY_KEY, props.apiKey())
					.build(props.solveModel()))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(requestBody)
				.retrieve()
				.body(String.class);
			log.debug(
				"Gemini 풀이 원본 응답 수신 model={} rawLength={} rawSnippet={}",
				props.solveModel(),
				rawResponseJson == null ? 0 : rawResponseJson.length(),
				abbreviate(rawResponseJson));

			return parseResponse(rawResponseJson);
		} catch (RestClientResponseException e) {
			log.debug(
				"Gemini 풀이 HTTP 실패 model={} status={} responseBodySnippet={}",
				props.solveModel(),
				e.getRawStatusCode(),
				abbreviate(e.getResponseBodyAsString()));
			throw GeminiAiException.externalCallFailed(e);
		} catch (GeminiAiException e) {
			log.debug("Gemini 풀이 GeminiAiException model={} message={}", props.solveModel(), e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			log.debug("Gemini 풀이 예외 model={} message={}", props.solveModel(), e.getMessage(), e);
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private PreparedPrompt buildPromptText(ProblemAiSolvePrompt prompt) {
		try {
			String promptText = GeminiSolvePromptTemplate.render();
			return new PreparedPrompt(promptText);
		} catch (Exception e) {
			throw GeminiAiException.promptBuildFailed(e);
		}
	}

	private Map<String, Object> buildRequestBody(String promptText, byte[] imageBytes, String imageMimeType) {
		if (imageBytes == null || imageBytes.length == 0) {
			throw GeminiAiException.promptBuildFailed(new IllegalArgumentException("Problem image bytes are empty"));
		}

		String safeMimeType = (imageMimeType == null || imageMimeType.isBlank()) ? "image/jpeg" : imageMimeType;
		String base64Image = Base64.getEncoder().encodeToString(imageBytes);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(Map.of(
			"role", "user",
			"parts", List.of(
				Map.of("inline_data", Map.of("mime_type", safeMimeType, "data", base64Image)),
				Map.of("text", promptText)))));

		Map<String, Object> generationConfig = new LinkedHashMap<>();
		generationConfig.put("temperature", 0);
		generationConfig.put("maxOutputTokens", SOLVE_MAX_OUTPUT_TOKENS);
		generationConfig.put("responseMimeType", "application/json");
		generationConfig.put("responseSchema", RESPONSE_SCHEMA);
		generationConfig.put("thinkingConfig", Map.of("thinkingBudget", SOLVE_THINKING_BUDGET));

		body.put("generationConfig", generationConfig);
		return body;
	}

	private ProblemAiSolveResult parseResponse(String rawResponseJson) {
		try {
			String modelText = extractModelJsonText(rawResponseJson);
			return parseOrFallback(modelText);
		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			log.debug("Gemini 풀이 parseResponse 실패 rawSnippet={} reason={}", abbreviate(rawResponseJson), e.getMessage(),
				e);
			throw GeminiAiException.responseParseFailed(e);
		}
	}

	private ProblemAiSolveResult parseOrFallback(String modelText) {
		String normalizedModelText = normalizeModelText(modelText);
		String unwrappedModelText = unwrapJsonTextNodeIfNeeded(normalizedModelText);
		String jsonPayload = extractJsonObject(unwrappedModelText);
		if (jsonPayload == null) {
			jsonPayload = repairTruncatedJsonObject(unwrappedModelText);
		}

		if (jsonPayload != null) {
			try {
				JsonNode root = objectMapper.readTree(jsonPayload);
				String solutionLatex = readTextOrNull(root, FIELD_SOLUTION_LATEX);
				String solutionText = readTextOrNull(root, FIELD_SOLUTION_TEXT);
				String finalAnswer = readTextOrNull(root, FIELD_FINAL_ANSWER);
				if (solutionLatex != null || solutionText != null || finalAnswer != null) {
					return finalizeResult(solutionLatex, solutionText);
				}
				log.debug("Gemini 풀이 JSON 파싱 성공했지만 필수 필드 누락. malformed fallback 시도 textLength={}",
					unwrappedModelText.length());
			} catch (Exception parseException) {
				log.debug("Gemini 풀이 JSON 파싱 실패. fallback 사용 textLength={} reason={}",
					unwrappedModelText.length(),
					parseException.getMessage());
			}
		} else {
			log.debug("Gemini 풀이 응답에서 JSON 객체를 찾지 못함. malformed fallback 시도 textLength={}",
				unwrappedModelText.length());
		}

		ProblemAiSolveResult extractedResult = extractFromMalformedStructuredText(unwrappedModelText);
		if (extractedResult != null) {
			log.debug(
				"Gemini 풀이 malformed structured text 추출 성공 latexLength={} textLength={}",
				extractedResult.solutionLatex() == null ? 0 : extractedResult.solutionLatex().length(),
				extractedResult.solutionText() == null ? 0 : extractedResult.solutionText().length());
			return extractedResult;
		}

		if (unwrappedModelText.isBlank()) {
			throw GeminiAiException.emptyText();
		}
		throw GeminiAiException
			.responseParseFailed(new IllegalArgumentException("Structured solve response parse failed"));
	}

	private String unwrapJsonTextNodeIfNeeded(String modelText) {
		if (modelText == null || modelText.isBlank()) {
			return "";
		}

		try {
			JsonNode root = objectMapper.readTree(modelText);
			if (!root.isTextual()) {
				return modelText;
			}
			String textValue = root.asText();
			if (textValue == null || textValue.isBlank()) {
				return modelText;
			}
			String trimmed = textValue.trim();
			if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
				return trimmed;
			}
			return modelText;
		} catch (Exception ignore) {
			return modelText;
		}
	}

	private String extractModelJsonText(String rawResponseJson) {
		try {
			JsonNode root = objectMapper.readTree(rawResponseJson == null ? "{}" : rawResponseJson);
			JsonNode partsNode = root.path("candidates")
				.path(0)
				.path("content")
				.path("parts");

			StringBuilder textBuilder = new StringBuilder();
			if (partsNode.isArray()) {
				for (JsonNode partNode : partsNode) {
					String partText = partNode.path("text").asText(null);
					if (partText != null) {
						textBuilder.append(partText);
					}
				}
			}

			String modelText = textBuilder.length() == 0 ? null : textBuilder.toString();
			if (modelText == null || modelText.isBlank()) {
				String finishReason = root.path("candidates").path(0).path("finishReason").asText("UNKNOWN");
				int thoughtsTokenCount = root.path("usageMetadata").path("thoughtsTokenCount").asInt(0);
				log.debug(
					"Gemini 풀이 text 비어있음 finishReason={} thoughtsTokenCount={} rawSnippet={}",
					finishReason,
					thoughtsTokenCount,
					abbreviate(rawResponseJson));
				throw GeminiAiException.emptyText();
			}
			log.debug("Gemini 풀이 raw model text 수신 length={}", modelText.length());
			return modelText;
		} catch (GeminiAiException e) {
			throw e;
		} catch (Exception e) {
			log.debug("Gemini 풀이 text 추출 실패 rawSnippet={} reason={}", abbreviate(rawResponseJson), e.getMessage(), e);
			throw GeminiAiException.responseParseFailed(e);
		}
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

	private String safeText(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}

	private String normalizeModelText(String modelText) {
		String trimmed = modelText == null ? "" : modelText.trim();
		if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
			String withoutPrefix = trimmed.replaceFirst("^```[a-zA-Z]*\\n", "");
			return withoutPrefix.replaceFirst("\n```$", "").trim();
		}
		return trimmed;
	}

	private String extractJsonObject(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		int startIndex = text.indexOf('{');
		if (startIndex < 0) {
			return null;
		}

		boolean inString = false;
		boolean escaped = false;
		int depth = 0;
		for (int index = startIndex; index < text.length(); index++) {
			char current = text.charAt(index);

			if (inString) {
				if (escaped) {
					escaped = false;
					continue;
				}
				if (current == '\\') {
					escaped = true;
					continue;
				}
				if (current == '"') {
					inString = false;
				}
				continue;
			}

			if (current == '"') {
				inString = true;
				continue;
			}
			if (current == '{') {
				depth += 1;
				continue;
			}
			if (current == '}') {
				depth -= 1;
				if (depth == 0) {
					return text.substring(startIndex, index + 1);
				}
			}
		}

		return null;
	}

	private String repairTruncatedJsonObject(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		int startIndex = text.indexOf('{');
		if (startIndex < 0) {
			return null;
		}

		int endExclusive = Math.min(text.length(), startIndex + MAX_JSON_REPAIR_SCAN_LENGTH);
		String candidate = text.substring(startIndex, endExclusive);
		StringBuilder repaired = new StringBuilder(candidate);

		boolean inString = false;
		boolean escaped = false;
		int depth = 0;

		for (int index = 0; index < candidate.length(); index++) {
			char current = candidate.charAt(index);

			if (inString) {
				if (escaped) {
					escaped = false;
					continue;
				}
				if (current == '\\') {
					escaped = true;
					continue;
				}
				if (current == '"') {
					inString = false;
				}
				continue;
			}

			if (current == '"') {
				inString = true;
				continue;
			}
			if (current == '{') {
				depth += 1;
				continue;
			}
			if (current == '}' && depth > 0) {
				depth -= 1;
			}
		}

		if (escaped) {
			repaired.append('\\');
		}
		if (inString) {
			repaired.append('"');
		}
		while (depth > 0) {
			repaired.append('}');
			depth -= 1;
		}

		String repairedJson = repaired.toString();
		if (!repairedJson.contains(KEY_SOLUTION_LATEX)
			&& !repairedJson.contains(KEY_SOLUTION_TEXT)
			&& !repairedJson.contains(KEY_FINAL_ANSWER)) {
			return null;
		}
		return repairedJson;
	}

	private String abbreviate(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		String compact = text.replace("\n", "\\n").replace("\r", "\\r");
		if (compact.length() <= LOG_TEXT_LIMIT) {
			return compact;
		}
		return compact.substring(0, LOG_TEXT_LIMIT) + "...";
	}

	private ProblemAiSolveResult extractFromMalformedStructuredText(String normalizedModelText) {
		String latex = extractMalformedFieldValue(normalizedModelText, KEY_SOLUTION_LATEX, KEY_SOLUTION_TEXT);
		String text = extractMalformedFieldValue(normalizedModelText, KEY_SOLUTION_TEXT, KEY_FINAL_ANSWER);
		String finalAnswer = extractMalformedFieldValue(normalizedModelText, KEY_FINAL_ANSWER, null);

		String normalizedLatex = normalizeExtractedValue(latex);
		String normalizedText = normalizeExtractedValue(text);

		if ((normalizedLatex == null || normalizedLatex.isBlank())
			&& (normalizedText == null || normalizedText.isBlank())) {
			return null;
		}

		String normalizedFinalAnswer = normalizeExtractedValue(finalAnswer);

		if ((normalizedText == null || normalizedText.isBlank())
			&& normalizedLatex != null
			&& !normalizedLatex.isBlank()) {
			normalizedText = normalizedLatex;
		}

		return finalizeResult(normalizedLatex, normalizedText);
	}

	private String extractMalformedFieldValue(String text, String fieldKey, String nextFieldKey) {
		int keyIndex = text.indexOf(fieldKey);
		if (keyIndex < 0) {
			return null;
		}

		int valueStartIndex = findFieldValueStartIndex(text, keyIndex + fieldKey.length());
		if (valueStartIndex < 0) {
			return null;
		}

		int closedQuoteIndex = findClosingQuoteIndex(text, valueStartIndex);
		String candidate;
		if (closedQuoteIndex > valueStartIndex) {
			candidate = text.substring(valueStartIndex, closedQuoteIndex);
			return isUsableMalformedFieldValue(candidate) ? candidate : null;
		}

		int nextFieldIndex = nextFieldKey == null ? -1 : text.indexOf(nextFieldKey, valueStartIndex);
		if (nextFieldIndex > valueStartIndex) {
			candidate = trimMalformedTail(text.substring(valueStartIndex, nextFieldIndex));
			return isUsableMalformedFieldValue(candidate) ? candidate : null;
		}

		return null;
	}

	private boolean isUsableMalformedFieldValue(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}

		String trimmed = value.trim();
		if (trimmed.length() > MALFORMED_FIELD_MAX_LENGTH) {
			return false;
		}
		if (trimmed.contains(KEY_SOLUTION_LATEX)
			|| trimmed.contains(KEY_SOLUTION_TEXT)
			|| trimmed.contains(KEY_FINAL_ANSWER)) {
			return false;
		}
		return true;
	}

	private int findFieldValueStartIndex(String text, int fromIndex) {
		int colonIndex = text.indexOf(':', fromIndex);
		if (colonIndex < 0) {
			return -1;
		}

		for (int index = colonIndex + 1; index < text.length(); index++) {
			char current = text.charAt(index);
			if (Character.isWhitespace(current)) {
				continue;
			}
			if (current == '"') {
				return index + 1;
			}
			return -1;
		}
		return -1;
	}

	private int findClosingQuoteIndex(String text, int startIndex) {
		boolean escaped = false;
		for (int index = startIndex; index < text.length(); index++) {
			char current = text.charAt(index);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (current == '\\') {
				escaped = true;
				continue;
			}
			if (current == '"') {
				return index;
			}
		}
		return -1;
	}

	private String trimMalformedTail(String value) {
		String trimmed = value == null ? null : value.trim();
		if (trimmed == null || trimmed.isBlank()) {
			return null;
		}
		if (trimmed.endsWith(",")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
		}
		if (trimmed.endsWith("}")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
		}
		if (trimmed.endsWith("\"")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private String normalizeExtractedValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String decoded = decodeJsonEscapes(value.trim());
		if (decoded == null || decoded.isBlank()) {
			return null;
		}
		return decoded.trim();
	}

	private String decodeJsonEscapes(String value) {
		StringBuilder result = new StringBuilder();
		for (int index = 0; index < value.length(); index++) {
			char current = value.charAt(index);
			if (current != '\\') {
				result.append(current);
				continue;
			}

			if (index + 1 >= value.length()) {
				return null;
			}

			char next = value.charAt(++index);
			switch (next) {
				case 'n' -> result.append('\n');
				case 'r' -> result.append('\r');
				case 't' -> result.append('\t');
				case 'b' -> result.append('\b');
				case 'f' -> result.append('\f');
				case '"' -> result.append('"');
				case '\\' -> result.append('\\');
				case '/' -> result.append('/');
				case 'u' -> {
					if (index + 4 >= value.length()) {
						return null;
					}
					String hex = value.substring(index + 1, index + 5);
					try {
						int codePoint = Integer.parseInt(hex, 16);
						result.append((char)codePoint);
					} catch (NumberFormatException exception) {
						return null;
					}
					index += 4;
				}
				default -> {
					return null;
				}
			}
		}
		return result.toString();
	}

	private ProblemAiSolveResult finalizeResult(String rawLatex, String rawPlainText) {
		String latex = normalizeDisplayText(rawLatex);
		String plainText = normalizeDisplayText(rawPlainText);
		if (plainText == null || plainText.isBlank()) {
			plainText = latex;
		}
		if (shouldPreferLatexText(latex, plainText)) {
			plainText = latex;
		}
		if (isDegenerateSolveText(plainText)) {
			throw GeminiAiException
				.responseParseFailed(new IllegalArgumentException("Degenerate solve text rejected"));
		}

		return new ProblemAiSolveResult(latex, plainText);
	}

	private boolean isDegenerateSolveText(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		if (text.length() > MAX_ACCEPTABLE_SOLUTION_TEXT_LENGTH) {
			return true;
		}

		String[] lines = text.split("\\n");
		Map<String, Integer> lineCounts = new HashMap<>();
		int longLineCount = 0;
		int repeatedLongLineCount = 0;

		for (String line : lines) {
			String key = normalizeLineKey(line);
			if (key == null || key.length() < REPEATED_LINE_MIN_LENGTH) {
				continue;
			}
			longLineCount += 1;
			int nextCount = lineCounts.getOrDefault(key, 0) + 1;
			lineCounts.put(key, nextCount);
			if (nextCount > 1) {
				repeatedLongLineCount += 1;
			}
			if (nextCount >= MAX_SAME_LINE_OCCURRENCES) {
				return true;
			}
		}

		if (longLineCount >= REPEATED_LINE_RATIO_MIN_LINES) {
			int repeatedRatio = repeatedLongLineCount * 100 / longLineCount;
			if (repeatedRatio >= REPEATED_LINE_RATIO_PERCENT) {
				return true;
			}
		}
		return false;
	}

	private String normalizeLineKey(String line) {
		if (line == null) {
			return null;
		}
		String normalized = line
			.replaceAll("\\s+", " ")
			.replaceAll("[\\\\\"']", "")
			.trim();
		if (normalized.isBlank()) {
			return null;
		}
		return normalized;
	}

	private boolean shouldPreferLatexText(String latex, String plainText) {
		if (latex == null || latex.isBlank() || plainText == null || plainText.isBlank()) {
			return false;
		}
		if (hasMathDelimiter(plainText)) {
			return false;
		}
		return containsLatexCommand(plainText) && hasMathDelimiter(latex);
	}

	private boolean hasMathDelimiter(String text) {
		return text.contains("$") || text.contains("\\(") || text.contains("\\[");
	}

	private boolean containsLatexCommand(String text) {
		return text.matches("(?s).*\\\\[A-Za-z]+.*");
	}

	private String normalizeDisplayText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value
			.replace("\r\n", "\n")
			.replaceAll("\\\\\\\\(?=[^A-Za-z])", "\n")
			.trim();
		if (normalized.isBlank()) {
			return null;
		}
		return normalized;
	}

	private record PreparedPrompt(String promptText) {
	}
}
