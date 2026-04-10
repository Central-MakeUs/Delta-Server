package cmc.delta.domain.problem.adapter.out.ai.gemini;

import org.springframework.stereotype.Component;

@Component
class GeminiSolveTextNormalizer {

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
}
