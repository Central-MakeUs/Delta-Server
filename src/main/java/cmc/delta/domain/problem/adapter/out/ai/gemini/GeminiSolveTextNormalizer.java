package cmc.delta.domain.problem.adapter.out.ai.gemini;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class GeminiSolveTextNormalizer {

	private static final Map<Character, Character> ESCAPE_REPLACEMENTS = Map.of(
		'n', '\n',
		'r', '\r',
		't', '\t',
		'b', '\b',
		'f', '\f',
		'"', '"',
		'\\', '\\',
		'/', '/');

	String normalizeDisplayText(String value) {
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

	String normalizeExtractedValue(String value) {
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
			int consumed = consumeEscapeSequence(result, value, index);
			if (consumed < 0) {
				return null;
			}
			index += consumed;
		}
		return result.toString();
	}

	// 역슬래시 뒤 이스케이프가 소비한 문자 수를 돌려주고, 복원 불가능하면 음수를 돌려준다.
	private int consumeEscapeSequence(StringBuilder result, String value, int backslashIndex) {
		if (backslashIndex + 1 >= value.length()) {
			return -1;
		}
		return appendDecodedEscape(result, value, backslashIndex + 1);
	}

	// 복원한 이스케이프가 소비한 문자 수를 돌려주고, 복원 불가능하면 음수를 돌려준다.
	private int appendDecodedEscape(StringBuilder result, String value, int escapeIndex) {
		char next = value.charAt(escapeIndex);
		if (next == 'u') {
			return decodeUnicodeEscape(result, value, escapeIndex);
		}
		Character replacement = ESCAPE_REPLACEMENTS.get(next);
		if (replacement == null) {
			return -1;
		}
		result.append(replacement.charValue());
		return 1;
	}

	private int decodeUnicodeEscape(StringBuilder result, String value, int escapeIndex) {
		if (escapeIndex + 4 >= value.length()) {
			return -1;
		}
		String hex = value.substring(escapeIndex + 1, escapeIndex + 5);
		try {
			int codePoint = Integer.parseInt(hex, 16);
			result.append((char)codePoint);
		} catch (NumberFormatException exception) {
			log.debug("Gemini 풀이 유니코드 이스케이프 복원 실패 hex={}", hex);
			return -1;
		}
		return 5;
	}
}
