package cmc.delta.domain.problem.application.support.command;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class SolutionTextNormalizer {

	private static final String ANSWER_LINE_PREFIX = "정답:";
	private static final Pattern FINAL_ANSWER_LINE_PATTERN = Pattern.compile(
		"^\\s*[\\\\\"']*(?:\\*\\*)?정답(?:\\*\\*)?\\s*[:：].*");
	private static final Pattern REASONING_LOOP_MARKER_PATTERN = Pattern.compile(
		"(?i).*(let\\s*'?s\\s+(recheck|reconsider|assume|use)|this is impossible|contradiction|다시\\s*검|재검토|모순|불가능).*");

	private static final int LONG_SENTENCE_MIN_LENGTH = 40;
	private static final int DUPLICATE_LINE_MIN_LENGTH = 20;
	private static final int MAX_SAME_LONG_SENTENCE_OCCURRENCES = 1;
	private static final int LOOP_MARKER_TRUNCATION_MIN_LINES = 12;
	private static final int LOOP_MARKER_TRUNCATION_THRESHOLD = 2;

	private SolutionTextNormalizer() {
	}

	/**
	 * AI 풀이 텍스트를 저장 전 정규화하는 파이프라인.
	 * 중복 라인/문장 제거 → 추론 루프 꼬리 제거 → 반복 문장 제거 → 앞부분 중복 블록 제거 → 정답 라인 정규화.
	 */
	public static String normalize(String solutionText, String answerValue, String answerFormat,
		Integer answerChoiceNo) {
		String text = normalizeWhitespace(solutionText);
		text = deduplicateConsecutiveLines(text);
		text = deduplicateGlobalLongLines(text);
		text = truncateReasoningLoopTail(text);
		text = sanitizeRepeatedSentences(text);
		text = collapseDuplicatedLeadingBlock(text);
		return ensureFinalAnswerLine(text, answerValue, answerFormat, answerChoiceNo);
	}

	public static String normalizeWhitespace(String solutionText) {
		if (solutionText == null) {
			return null;
		}
		String normalized = solutionText.replace("\r\n", "\n").trim();
		return normalized.isBlank() ? null : normalized;
	}

	public static boolean looksLikeRawJsonDump(String solutionText) {
		String trimmed = solutionText.trim();
		return trimmed.startsWith("{") && trimmed.contains("\"solution_latex\"");
	}

	private static String deduplicateConsecutiveLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		StringBuilder result = new StringBuilder();
		String previousKey = null;

		for (String line : lines) {
			String key = normalizeLineKey(line);
			boolean isDuplicate = previousKey != null
				&& key != null
				&& key.length() >= DUPLICATE_LINE_MIN_LENGTH
				&& key.equals(previousKey);
			if (isDuplicate) {
				continue;
			}
			appendLine(result, line);
			if (key != null) {
				previousKey = key;
			}
		}

		String deduplicated = result.toString().trim();
		return deduplicated.isBlank() ? solutionText : deduplicated;
	}

	private static String deduplicateGlobalLongLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		Map<String, Integer> seen = new HashMap<>();
		StringBuilder result = new StringBuilder();

		for (String line : lines) {
			String key = normalizeLineKey(line);
			if (key != null && key.length() >= DUPLICATE_LINE_MIN_LENGTH) {
				int occurrence = seen.getOrDefault(key, 0);
				if (occurrence >= 1) {
					continue;
				}
				seen.put(key, occurrence + 1);
			}
			appendLine(result, line);
		}

		String deduplicated = result.toString().trim();
		return deduplicated.isBlank() ? solutionText : deduplicated;
	}

	private static String truncateReasoningLoopTail(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		int loopMarkerCount = 0;
		int truncateFrom = -1;

		for (int index = 0; index < lines.length; index++) {
			if (!REASONING_LOOP_MARKER_PATTERN.matcher(lines[index]).matches()) {
				continue;
			}
			loopMarkerCount += 1;
			if (index >= LOOP_MARKER_TRUNCATION_MIN_LINES && loopMarkerCount >= LOOP_MARKER_TRUNCATION_THRESHOLD) {
				truncateFrom = index;
				break;
			}
		}

		if (truncateFrom < 0) {
			return solutionText;
		}

		StringBuilder kept = new StringBuilder();
		for (int index = 0; index < truncateFrom; index++) {
			appendLine(kept, lines[index]);
		}

		String result = kept.toString().trim();
		return result.isBlank() ? solutionText : result;
	}

	private static String sanitizeRepeatedSentences(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] sentences = solutionText.split("(?<=[.!?])\\s+");
		if (sentences.length == 0) {
			return solutionText;
		}

		Map<String, Integer> sentenceCounts = new HashMap<>();
		StringBuilder result = new StringBuilder();

		for (String sentence : sentences) {
			String key = normalizeSentenceKey(sentence);
			if (key == null) {
				appendSentence(result, sentence);
				continue;
			}
			int nextCount = sentenceCounts.getOrDefault(key, 0) + 1;
			sentenceCounts.put(key, nextCount);
			if (nextCount <= MAX_SAME_LONG_SENTENCE_OCCURRENCES) {
				appendSentence(result, sentence);
			}
		}

		String sanitized = result.toString().trim();
		return sanitized.isBlank() ? solutionText : sanitized;
	}

	private static String collapseDuplicatedLeadingBlock(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String normalized = solutionText.trim();
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

	private static String ensureFinalAnswerLine(String solutionText, String answerValue, String answerFormat,
		Integer answerChoiceNo) {
		String normalized = normalizeWhitespace(solutionText);
		TrailingAnswerStripResult stripResult = stripTrailingAnswerLines(normalized);

		String finalAnswer = resolveDisplayAnswer(stripResult.trailingAnswer(), answerValue, answerFormat,
			answerChoiceNo);

		if (finalAnswer == null) {
			return stripResult.body();
		}
		if (stripResult.body() == null || stripResult.body().isBlank()) {
			return ANSWER_LINE_PREFIX + " " + finalAnswer;
		}
		return stripResult.body() + "\n\n" + ANSWER_LINE_PREFIX + " " + finalAnswer;
	}

	private static String resolveDisplayAnswer(String trailingAnswer, String answerValue, String answerFormat,
		Integer answerChoiceNo) {
		if (trailingAnswer != null) {
			return trailingAnswer;
		}
		if (answerChoiceNo != null) {
			return String.valueOf(answerChoiceNo);
		}
		if (answerValue != null && !answerValue.isBlank()) {
			return answerValue;
		}
		return null;
	}

	private static TrailingAnswerStripResult stripTrailingAnswerLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return new TrailingAnswerStripResult(null, null);
		}

		String[] lines = solutionText.split("\n");
		int endIndex = lines.length - 1;
		String trailingAnswer = null;

		while (endIndex >= 0 && lines[endIndex].trim().isEmpty()) {
			endIndex -= 1;
		}
		while (endIndex >= 0 && FINAL_ANSWER_LINE_PATTERN.matcher(lines[endIndex]).matches()) {
			if (trailingAnswer == null) {
				trailingAnswer = extractAnswerValue(lines[endIndex]);
			}
			endIndex -= 1;
			while (endIndex >= 0 && lines[endIndex].trim().isEmpty()) {
				endIndex -= 1;
			}
		}

		if (endIndex < 0) {
			return new TrailingAnswerStripResult(null, trailingAnswer);
		}

		StringBuilder body = new StringBuilder();
		for (int index = 0; index <= endIndex; index++) {
			appendLine(body, lines[index]);
		}

		String bodyText = body.toString().trim();
		return bodyText.isBlank()
			? new TrailingAnswerStripResult(null, trailingAnswer)
			: new TrailingAnswerStripResult(bodyText, trailingAnswer);
	}

	private static String extractAnswerValue(String answerLine) {
		if (answerLine == null) {
			return null;
		}
		int sep = answerLine.indexOf(':');
		if (sep < 0) {
			sep = answerLine.indexOf('：');
		}
		if (sep < 0 || sep + 1 >= answerLine.length()) {
			return null;
		}
		String value = answerLine.substring(sep + 1).trim();
		while (value.startsWith("*") || value.startsWith("\"") || value.startsWith("'") || value.startsWith("\\")) {
			value = value.substring(1).trim();
		}
		while (value.endsWith("*") || value.endsWith("\"") || value.endsWith("'")) {
			value = value.substring(0, value.length() - 1).trim();
		}
		return value.isBlank() ? null : value;
	}

	private static int findSecondBlockStart(String text, String anchor, int anchorLength) {
		int directIndex = text.indexOf(anchor, anchorLength);
		if (directIndex >= 0) {
			return directIndex;
		}
		int quotedIndex = text.indexOf("\"" + anchor, anchorLength);
		if (quotedIndex >= 0) {
			return quotedIndex + 1;
		}
		int lineIndex = text.indexOf("\n" + anchor, anchorLength);
		if (lineIndex >= 0) {
			return lineIndex + 1;
		}
		return -1;
	}

	private static boolean isHighlySimilarPrefix(String first, String second) {
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
		return (double)matched / compareLength >= 0.9;
	}

	private static String normalizeLineKey(String line) {
		if (line == null) {
			return null;
		}
		String normalized = line.trim().replaceAll("\\s+", " ");
		return normalized.isBlank() ? null : normalized;
	}

	private static String normalizeSentenceKey(String sentence) {
		if (sentence == null) {
			return null;
		}
		String normalized = sentence.replace("\n", " ").trim().replaceAll("\\s+", " ");
		return normalized.length() < LONG_SENTENCE_MIN_LENGTH ? null : normalized;
	}

	private static void appendLine(StringBuilder builder, String line) {
		if (builder.length() > 0) {
			builder.append('\n');
		}
		builder.append(line);
	}

	private static void appendSentence(StringBuilder builder, String sentence) {
		String trimmed = sentence == null ? "" : sentence.trim();
		if (trimmed.isBlank()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append('\n');
		}
		builder.append(trimmed);
	}

	private record TrailingAnswerStripResult(String body, String trailingAnswer) {
	}
}
