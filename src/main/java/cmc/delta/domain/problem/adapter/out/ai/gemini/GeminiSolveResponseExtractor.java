package cmc.delta.domain.problem.adapter.out.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	String unwrapJsonTextNodeIfNeeded(String modelText) {
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

	String extractJsonObject(String text) {
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
		if (compact.length() <= 1200) {
			return compact;
		}
		return compact.substring(0, 1200) + "...";
	}
}
