package cmc.delta.domain.problem.adapter.out.ai.gemini;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class GeminiSolveDegenerateDetector {

	private static final int MAX_ACCEPTABLE_SOLUTION_TEXT_LENGTH = 12000;
	private static final int REPEATED_LINE_MIN_LENGTH = 20;
	private static final int MAX_SAME_LINE_OCCURRENCES = 4;
	private static final int REPEATED_LINE_RATIO_MIN_LINES = 12;
	private static final int REPEATED_LINE_RATIO_PERCENT = 35;

	boolean isDegenerate(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		if (text.length() > MAX_ACCEPTABLE_SOLUTION_TEXT_LENGTH) {
			return true;
		}
		return hasExcessiveRepeatedLines(text);
	}

	private boolean hasExcessiveRepeatedLines(String text) {
		Map<String, Integer> lineCounts = countLongLineOccurrences(text);
		if (lineCounts.values().stream().anyMatch(count -> count >= MAX_SAME_LINE_OCCURRENCES)) {
			return true;
		}
		int longLineCount = lineCounts.values().stream().mapToInt(Integer::intValue).sum();
		int repeatedLongLineCount = longLineCount - lineCounts.size();
		return isRepeatedLineRatioTooHigh(longLineCount, repeatedLongLineCount);
	}

	private Map<String, Integer> countLongLineOccurrences(String text) {
		Map<String, Integer> lineCounts = new HashMap<>();
		for (String line : text.split("\\n")) {
			String key = normalizeLineKey(line);
			if (key == null || key.length() < REPEATED_LINE_MIN_LENGTH) {
				continue;
			}
			lineCounts.merge(key, 1, Integer::sum);
		}
		return lineCounts;
	}

	private boolean isRepeatedLineRatioTooHigh(int longLineCount, int repeatedLongLineCount) {
		if (longLineCount < REPEATED_LINE_RATIO_MIN_LINES) {
			return false;
		}
		int repeatedRatio = repeatedLongLineCount * 100 / longLineCount;
		return repeatedRatio >= REPEATED_LINE_RATIO_PERCENT;
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
}
