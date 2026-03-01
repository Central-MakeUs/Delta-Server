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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@EnableConfigurationProperties(GeminiProperties.class)
@RequiredArgsConstructor
public class GeminiProblemSolveAiClient implements ProblemSolveAiClient {

	private static final int LOG_TEXT_LIMIT = 1200;
	private static final int SOLVE_MAX_OUTPUT_TOKENS = 8192;
	private static final int SOLVE_THINKING_BUDGET = 0;
	private static final String ANSWER_LINE_PREFIX = "정답:";

	private static final String PATH_GENERATE_CONTENT = "/v1beta/models/{model}:generateContent";
	private static final String QUERY_KEY = "key";

	private static final String FIELD_SOLUTION_LATEX = "solution_latex";
	private static final String FIELD_SOLUTION_TEXT = "solution_text";
	private static final String FIELD_FINAL_ANSWER = "final_answer";
	private static final String KEY_SOLUTION_LATEX = "\"solution_latex\"";
	private static final String KEY_SOLUTION_TEXT = "\"solution_text\"";
	private static final String KEY_FINAL_ANSWER = "\"final_answer\"";
	private static final int MAX_SAME_LONG_SENTENCE_OCCURRENCES = 3;
	private static final int LONG_SENTENCE_MIN_LENGTH = 40;

	private static final Map<String, Object> RESPONSE_SCHEMA = GeminiSolveSchemaFactory.responseSchema();

	private final GeminiProperties props;
	private final ObjectMapper objectMapper;
	private final RestClient geminiRestClient;

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
			String answerFormat = prompt.answerFormat() == null ? "UNKNOWN" : prompt.answerFormat().name();
			String answerValue = safeText(prompt.answerValue());
			String answerChoiceNo = prompt.answerChoiceNo() == null ? "null" : String.valueOf(prompt.answerChoiceNo());
			String promptText = GeminiSolvePromptTemplate.render(answerFormat, answerValue, answerChoiceNo);
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

		if (jsonPayload != null) {
			try {
				JsonNode root = objectMapper.readTree(jsonPayload);
				String solutionLatex = readTextOrNull(root, FIELD_SOLUTION_LATEX);
				String solutionText = readTextOrNull(root, FIELD_SOLUTION_TEXT);
				String finalAnswer = readTextOrNull(root, FIELD_FINAL_ANSWER);
				if (solutionLatex != null || solutionText != null || finalAnswer != null) {
					return finalizeResult(solutionLatex, solutionText, finalAnswer);
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
		int startIndex = text.indexOf('{');
		int endIndex = text.lastIndexOf('}');
		if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
			return null;
		}
		return text.substring(startIndex, endIndex + 1);
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

		return finalizeResult(normalizedLatex, normalizedText, normalizedFinalAnswer);
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
		if (closedQuoteIndex > valueStartIndex) {
			return text.substring(valueStartIndex, closedQuoteIndex);
		}

		int nextFieldIndex = nextFieldKey == null ? -1 : text.indexOf(nextFieldKey, valueStartIndex);
		if (nextFieldIndex > valueStartIndex) {
			return trimMalformedTail(text.substring(valueStartIndex, nextFieldIndex));
		}

		return trimMalformedTail(text.substring(valueStartIndex));
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
		return value
			.replace("\\n", "\n")
			.replace("\\r", "\r")
			.replace("\\\"", "\"")
			.replace("\\\\", "\\")
			.trim();
	}

	private ProblemAiSolveResult finalizeResult(String rawLatex, String rawPlainText, String rawFinalAnswer) {
		String latex = normalizeDisplayText(rawLatex);
		String plainText = normalizeDisplayText(rawPlainText);
		if (plainText == null || plainText.isBlank()) {
			plainText = latex;
		}
		plainText = sanitizeRepeatedSentences(plainText);
		plainText = collapseDuplicatedLeadingBlock(plainText);

		String resolvedFinalAnswer = resolveFinalAnswer(rawFinalAnswer, plainText);
		plainText = mergeFinalAnswerLine(plainText, resolvedFinalAnswer);

		return new ProblemAiSolveResult(latex, plainText);
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

	private String resolveFinalAnswer(String rawFinalAnswer, String plainText) {
		String normalizedFinalAnswer = normalizeFinalAnswer(rawFinalAnswer);
		if (normalizedFinalAnswer != null) {
			return normalizedFinalAnswer;
		}

		String finalAnswerFromText = extractAnswerFromText(plainText);
		if (finalAnswerFromText != null) {
			return normalizeFinalAnswer(finalAnswerFromText);
		}
		return null;
	}

	private String extractAnswerFromText(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			return null;
		}

		String normalizedText = plainText.replace("\r\n", "\n");
		String[] lines = normalizedText.split("\n");
		for (int index = lines.length - 1; index >= 0; index--) {
			String line = normalizeAnswerLinePrefix(lines[index]);
			if (line.startsWith(ANSWER_LINE_PREFIX)) {
				return line.substring(ANSWER_LINE_PREFIX.length()).trim();
			}
		}

		return null;
	}

	private String normalizeFinalAnswer(String finalAnswer) {
		String normalized = normalizeDisplayText(finalAnswer);
		if (normalized == null) {
			return null;
		}
		return normalized.replace("\r\n", " ").replace("\n", " ").trim();
	}

	private String mergeFinalAnswerLine(String plainText, String finalAnswer) {
		if (plainText == null && finalAnswer == null) {
			return null;
		}

		String normalizedText = plainText == null ? "" : plainText.replace("\r\n", "\n").trim();
		if (finalAnswer == null || finalAnswer.isBlank()) {
			return normalizedText.isBlank() ? null : normalizedText;
		}

		String answerLine = ANSWER_LINE_PREFIX + " " + finalAnswer;
		if (normalizedText.isBlank()) {
			return answerLine;
		}

		String[] lines = normalizedText.split("\n");
		StringBuilder rebuilt = new StringBuilder();
		boolean replaced = false;
		for (String line : lines) {
			String trimmedLine = normalizeAnswerLinePrefix(line);
			if (trimmedLine.startsWith(ANSWER_LINE_PREFIX)) {
				if (!replaced) {
					appendLine(rebuilt, answerLine);
					replaced = true;
				}
				continue;
			}
			appendLine(rebuilt, line);
		}

		if (!replaced) {
			appendLine(rebuilt, "");
			appendLine(rebuilt, answerLine);
		}

		String merged = rebuilt.toString().trim();
		return merged.isBlank() ? null : merged;
	}

	private void appendLine(StringBuilder builder, String line) {
		if (builder.length() > 0) {
			builder.append('\n');
		}
		builder.append(line);
	}

	private String normalizeAnswerLinePrefix(String line) {
		if (line == null) {
			return "";
		}
		String normalized = line.trim();
		while (normalized.startsWith("\\")) {
			normalized = normalized.substring(1).trim();
		}
		return normalized;
	}

	private String sanitizeRepeatedSentences(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			return plainText;
		}

		String[] sentences = plainText.split("(?<=[.!?])\\s+");
		if (sentences.length == 0) {
			return plainText;
		}

		Map<String, Integer> sentenceCounts = new HashMap<>();
		StringBuilder sanitizedBuilder = new StringBuilder();
		for (String sentence : sentences) {
			String normalizedSentence = normalizeSentenceKey(sentence);
			if (normalizedSentence == null) {
				appendSentence(sanitizedBuilder, sentence);
				continue;
			}

			int nextCount = sentenceCounts.getOrDefault(normalizedSentence, 0) + 1;
			sentenceCounts.put(normalizedSentence, nextCount);
			if (nextCount > MAX_SAME_LONG_SENTENCE_OCCURRENCES) {
				continue;
			}
			appendSentence(sanitizedBuilder, sentence);
		}

		String sanitized = sanitizedBuilder.toString().trim();
		return sanitized.isBlank() ? plainText : sanitized;
	}

	private String normalizeSentenceKey(String sentence) {
		if (sentence == null) {
			return null;
		}
		String normalized = sentence.replace("\n", " ").trim().replaceAll("\\s+", " ");
		if (normalized.length() < LONG_SENTENCE_MIN_LENGTH) {
			return null;
		}
		return normalized;
	}

	private void appendSentence(StringBuilder builder, String sentence) {
		String trimmedSentence = sentence == null ? "" : sentence.trim();
		if (trimmedSentence.isBlank()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append('\n');
		}
		builder.append(trimmedSentence);
	}

	private String collapseDuplicatedLeadingBlock(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			return plainText;
		}

		String normalized = plainText.trim();
		if (normalized.length() < 400) {
			return normalized;
		}

		int anchorLength = Math.min(160, normalized.length() / 4);
		if (anchorLength < 60) {
			return normalized;
		}
		String anchor = normalized.substring(0, anchorLength);

		int secondIndex = findSecondBlockStart(normalized, anchor, anchorLength);
		if (secondIndex < 0) {
			return normalized;
		}

		String firstBlock = normalized.substring(0, secondIndex).trim();
		String secondBlock = normalized.substring(secondIndex).trim();
		if (!isHighlySimilarPrefix(firstBlock, secondBlock)) {
			return normalized;
		}

		return firstBlock;
	}

	private int findSecondBlockStart(String text, String anchor, int anchorLength) {
		int directIndex = text.indexOf(anchor, anchorLength);
		if (directIndex >= 0) {
			return directIndex;
		}

		String quotedAnchor = "\"" + anchor;
		int quotedIndex = text.indexOf(quotedAnchor, anchorLength);
		if (quotedIndex >= 0) {
			return quotedIndex + 1;
		}

		String lineAnchored = "\n" + anchor;
		int lineIndex = text.indexOf(lineAnchored, anchorLength);
		if (lineIndex >= 0) {
			return lineIndex + 1;
		}

		return -1;
	}

	private boolean isHighlySimilarPrefix(String first, String second) {
		int compareLength = Math.min(Math.min(first.length(), second.length()), 500);
		if (compareLength < 120) {
			return false;
		}

		int matched = 0;
		for (int index = 0; index < compareLength; index++) {
			if (first.charAt(index) == second.charAt(index)) {
				matched += 1;
			}
		}

		double ratio = (double)matched / (double)compareLength;
		return ratio >= 0.9;
	}

	private record PreparedPrompt(String promptText) {
	}
}
