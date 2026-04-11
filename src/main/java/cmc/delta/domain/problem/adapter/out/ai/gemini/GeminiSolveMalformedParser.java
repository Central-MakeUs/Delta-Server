package cmc.delta.domain.problem.adapter.out.ai.gemini;

import org.springframework.stereotype.Component;

@Component
class GeminiSolveMalformedParser {

	private static final int MALFORMED_FIELD_MAX_LENGTH = 8000;

	private static final String KEY_SOLUTION_LATEX = "\"solution_latex\"";
	private static final String KEY_SOLUTION_TEXT = "\"solution_text\"";
	private static final String KEY_FINAL_ANSWER = "\"final_answer\"";

	private static final String FIELD_SOLUTION_LATEX = "solution_latex";
	private static final String FIELD_SOLUTION_TEXT = "solution_text";

	private final GeminiSolveTextNormalizer textNormalizer;

	GeminiSolveMalformedParser(GeminiSolveTextNormalizer textNormalizer) {
		this.textNormalizer = textNormalizer;
	}

	ParsedFields parse(String normalizedModelText) {
		String latex = extractMalformedFieldValue(normalizedModelText, KEY_SOLUTION_LATEX, KEY_SOLUTION_TEXT);
		String text = extractMalformedFieldValue(normalizedModelText, KEY_SOLUTION_TEXT, KEY_FINAL_ANSWER);

		String normalizedLatex = textNormalizer.normalizeExtractedValue(latex);
		String normalizedText = textNormalizer.normalizeExtractedValue(text);

		if ((normalizedLatex == null || normalizedLatex.isBlank())
			&& (normalizedText == null || normalizedText.isBlank())) {
			return null;
		}

		if ((normalizedText == null || normalizedText.isBlank())
			&& normalizedLatex != null
			&& !normalizedLatex.isBlank()) {
			normalizedText = normalizedLatex;
		}

		return new ParsedFields(normalizedLatex, normalizedText);
	}

	record ParsedFields(String solutionLatex, String solutionText) {
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
}
