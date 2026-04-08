package cmc.delta.domain.problem.adapter.out.ai;

import com.fasterxml.jackson.databind.JsonNode;

public final class AiResponseParseUtils {

	private AiResponseParseUtils() {
	}

	public static String readTextOrNull(JsonNode node, String fieldName) {
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

	public static String stripMarkdownCodeFence(String text) {
		String trimmed = text == null ? "" : text.trim();
		if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) {
			return trimmed;
		}
		String withoutPrefix = trimmed.replaceFirst("^```[a-zA-Z]*\\n", "");
		return withoutPrefix.replaceFirst("\\n```$", "").trim();
	}

	public static boolean hasMathDelimiter(String text) {
		return text.contains("$") || text.contains("\\(") || text.contains("\\[");
	}

	public static boolean containsLatexCommand(String text) {
		return text.matches("(?s).*\\\\[A-Za-z]+.*");
	}

	public static boolean shouldPreferLatexText(String latex, String plainText) {
		if (latex == null || latex.isBlank() || plainText == null || plainText.isBlank()) {
			return false;
		}
		if (hasMathDelimiter(plainText)) {
			return false;
		}
		return containsLatexCommand(plainText) && hasMathDelimiter(latex);
	}
}
