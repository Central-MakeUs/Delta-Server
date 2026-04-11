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
}
