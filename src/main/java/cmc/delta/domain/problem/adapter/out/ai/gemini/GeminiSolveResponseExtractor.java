package cmc.delta.domain.problem.adapter.out.ai.gemini;

import cmc.delta.domain.problem.adapter.out.ai.AiResponseParseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class GeminiSolveResponseExtractor {

	private static final int MAX_JSON_REPAIR_SCAN_LENGTH = 24000;

	private static final String KEY_SOLUTION_LATEX = "\"solution_latex\"";
	private static final String KEY_SOLUTION_TEXT = "\"solution_text\"";
	private static final String KEY_FINAL_ANSWER = "\"final_answer\"";

	private final ObjectMapper objectMapper;

	GeminiSolveResponseExtractor(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	String extractModelJsonText(String rawResponseJson) {
		try {
			JsonNode root = objectMapper.readTree(rawResponseJson == null ? "{}" : rawResponseJson);
			String modelText = concatPartTexts(findPartsNode(root));
			if (modelText == null || modelText.isBlank()) {
				logEmptyModelText(root, rawResponseJson);
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

	private JsonNode findPartsNode(JsonNode root) {
		return root.path("candidates")
			.path(0)
			.path("content")
			.path("parts");
	}

	private String concatPartTexts(JsonNode partsNode) {
		if (!partsNode.isArray()) {
			return null;
		}
		StringBuilder textBuilder = new StringBuilder();
		for (JsonNode partNode : partsNode) {
			String partText = partNode.path("text").asText(null);
			if (partText != null) {
				textBuilder.append(partText);
			}
		}
		return textBuilder.length() == 0 ? null : textBuilder.toString();
	}

	private void logEmptyModelText(JsonNode root, String rawResponseJson) {
		String finishReason = root.path("candidates").path(0).path("finishReason").asText("UNKNOWN");
		int thoughtsTokenCount = root.path("usageMetadata").path("thoughtsTokenCount").asInt(0);
		log.debug(
			"Gemini 풀이 text 비어있음 finishReason={} thoughtsTokenCount={} rawSnippet={}",
			finishReason,
			thoughtsTokenCount,
			abbreviate(rawResponseJson));
	}

	String unwrapJsonTextNodeIfNeeded(String modelText) {
		if (modelText == null || modelText.isBlank()) {
			return "";
		}
		try {
			return readWrappedJsonText(modelText).orElse(modelText);
		} catch (Exception e) {
			log.debug("Gemini 풀이 text 노드 unwrap 실패. 원문 유지 reason={}", e.getMessage());
			return modelText;
		}
	}

	/** 응답이 JSON 문자열 노드로 한 번 감싸져 온 경우에만 내부 JSON 텍스트를 꺼낸다. */
	private Optional<String> readWrappedJsonText(String modelText) throws JsonProcessingException {
		JsonNode root = objectMapper.readTree(modelText);
		if (!root.isTextual()) {
			return Optional.empty();
		}
		return Optional.ofNullable(root.asText())
			.map(String::trim)
			.filter(trimmed -> trimmed.startsWith("{") || trimmed.startsWith("["));
	}

	String extractJsonObject(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		int startIndex = text.indexOf('{');
		if (startIndex < 0) {
			return null;
		}

		JsonObjectScanner.ScanResult result = JsonObjectScanner.scan(text, startIndex, text.length());
		if (!result.isBalanced()) {
			return null;
		}
		return text.substring(startIndex, result.balancedEndIndex() + 1);
	}

	String repairTruncatedJsonObject(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		int startIndex = text.indexOf('{');
		if (startIndex < 0) {
			return null;
		}

		int endExclusive = Math.min(text.length(), startIndex + MAX_JSON_REPAIR_SCAN_LENGTH);
		String candidate = text.substring(startIndex, endExclusive);

		JsonObjectScanner.ScanResult state = JsonObjectScanner.scan(candidate, 0, candidate.length());
		String repairedJson = appendMissingClosers(candidate, state);

		if (!containsAnySolveField(repairedJson)) {
			return null;
		}
		return repairedJson;
	}

	private String appendMissingClosers(String candidate, JsonObjectScanner.ScanResult state) {
		StringBuilder repaired = new StringBuilder(candidate);
		if (state.escaped()) {
			repaired.append('\\');
		}
		if (state.inString()) {
			repaired.append('"');
		}
		repaired.append("}".repeat(Math.max(0, state.openDepth())));
		return repaired.toString();
	}

	private boolean containsAnySolveField(String json) {
		return json.contains(KEY_SOLUTION_LATEX)
			|| json.contains(KEY_SOLUTION_TEXT)
			|| json.contains(KEY_FINAL_ANSWER);
	}

	private String abbreviate(String text) {
		return AiResponseParseUtils.abbreviateForLog(text);
	}
}
