package cmc.delta.domain.problem.application.support.command;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

	private static final int LEADING_BLOCK_MIN_TEXT_LENGTH = 400;
	private static final int LEADING_BLOCK_MAX_ANCHOR_LENGTH = 160;
	private static final int LEADING_BLOCK_MIN_ANCHOR_LENGTH = 60;
	private static final int LEADING_BLOCK_ANCHOR_RATIO = 4;
	private static final int PREFIX_COMPARE_MAX_LENGTH = 500;
	private static final int PREFIX_COMPARE_MIN_LENGTH = 120;
	private static final double PREFIX_SIMILARITY_THRESHOLD = 0.9;

	private SolutionTextNormalizer() {}

	/**
	 * AI 풀이 텍스트를 저장 전 정규화하는 파이프라인.
	 * 중복 라인/문장 제거 → 추론 루프 꼬리 제거 → 반복 문장 제거 → 앞부분 중복 블록 제거 → 정답 라인 정규화.
	 */
	public static String normalize(String solutionText) {
		String text = normalizeWhitespace(solutionText);
		text = deduplicateConsecutiveLines(text);
		text = deduplicateGlobalLongLines(text);
		text = truncateReasoningLoopTail(text);
		text = sanitizeRepeatedSentences(text);
		text = collapseDuplicatedLeadingBlock(text);
		return ensureFinalAnswerLine(text);
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
		String deduplicated = removeConsecutiveDuplicateLines(solutionText.split("\n"));
		return deduplicated.isBlank() ? solutionText : deduplicated;
	}

	private static String removeConsecutiveDuplicateLines(String[] lines) {
		StringBuilder result = new StringBuilder();
		String previousKey = null;

		for (String line : lines) {
			String key = normalizeLineKey(line);
			if (isDuplicateOfPrevious(key, previousKey)) {
				continue;
			}
			appendLine(result, line);
			previousKey = key != null ? key : previousKey;
		}
		return result.toString().trim();
	}

	private static boolean isDuplicateOfPrevious(String key, String previousKey) {
		return previousKey != null
			&& key != null
			&& key.length() >= DUPLICATE_LINE_MIN_LENGTH
			&& key.equals(previousKey);
	}

	private static String deduplicateGlobalLongLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}
		String deduplicated = removeGlobalDuplicateLongLines(solutionText.split("\n"));
		return deduplicated.isBlank() ? solutionText : deduplicated;
	}

	private static String removeGlobalDuplicateLongLines(String[] lines) {
		Set<String> seenKeys = new HashSet<>();
		StringBuilder result = new StringBuilder();

		for (String line : lines) {
			if (isRepeatedLongLine(line, seenKeys)) {
				continue;
			}
			appendLine(result, line);
		}
		return result.toString().trim();
	}

	private static boolean isRepeatedLongLine(String line, Set<String> seenKeys) {
		String key = normalizeLineKey(line);
		if (key == null || key.length() < DUPLICATE_LINE_MIN_LENGTH) {
			return false;
		}
		// 처음 보는 키면 등록하고 통과, 이미 본 키면 중복으로 제거한다.
		return !seenKeys.add(key);
	}

	private static String truncateReasoningLoopTail(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		int truncateFrom = findLoopTruncationIndex(lines);

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

	private static int findLoopTruncationIndex(String[] lines) {
		int loopMarkerCount = 0;

		for (int index = 0; index < lines.length; index++) {
			if (!REASONING_LOOP_MARKER_PATTERN.matcher(lines[index]).matches()) {
				continue;
			}
			loopMarkerCount += 1;
			if (index >= LOOP_MARKER_TRUNCATION_MIN_LINES && loopMarkerCount >= LOOP_MARKER_TRUNCATION_THRESHOLD) {
				return index;
			}
		}

		return -1;
	}

	private static String sanitizeRepeatedSentences(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}
		String[] sentences = solutionText.split("(?<=[.!?])\\s+");
		if (sentences.length == 0) {
			return solutionText;
		}
		String sanitized = removeRepeatedLongSentences(sentences);
		return sanitized.isBlank() ? solutionText : sanitized;
	}

	private static String removeRepeatedLongSentences(String[] sentences) {
		Map<String, Integer> sentenceCounts = new HashMap<>();
		StringBuilder result = new StringBuilder();

		for (String sentence : sentences) {
			if (shouldKeepSentence(sentence, sentenceCounts)) {
				appendSentence(result, sentence);
			}
		}
		return result.toString().trim();
	}

	private static boolean shouldKeepSentence(String sentence, Map<String, Integer> sentenceCounts) {
		String key = normalizeSentenceKey(sentence);
		if (key == null) {
			// 짧은 문장은 반복 검사 대상이 아니다.
			return true;
		}
		int nextCount = sentenceCounts.merge(key, 1, Integer::sum);
		return nextCount <= MAX_SAME_LONG_SENTENCE_OCCURRENCES;
	}

	private static String collapseDuplicatedLeadingBlock(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}
		String normalized = solutionText.trim();
		String anchor = resolveLeadingAnchor(normalized);
		if (anchor == null) {
			return normalized;
		}
		return collapseIfLeadingBlockRepeats(normalized, anchor);
	}

	private static String resolveLeadingAnchor(String normalized) {
		if (normalized.length() < LEADING_BLOCK_MIN_TEXT_LENGTH) {
			return null;
		}
		int anchorLength = Math.min(LEADING_BLOCK_MAX_ANCHOR_LENGTH,
			normalized.length() / LEADING_BLOCK_ANCHOR_RATIO);
		return anchorLength < LEADING_BLOCK_MIN_ANCHOR_LENGTH ? null : normalized.substring(0, anchorLength);
	}

	private static String collapseIfLeadingBlockRepeats(String normalized, String anchor) {
		int secondIndex = findSecondBlockStart(normalized, anchor, anchor.length());
		if (secondIndex < 0) {
			return normalized;
		}
		String firstBlock = normalized.substring(0, secondIndex).trim();
		String secondBlock = normalized.substring(secondIndex).trim();
		return isHighlySimilarPrefix(firstBlock, secondBlock) ? firstBlock : normalized;
	}

	private static String ensureFinalAnswerLine(String solutionText) {
		String normalized = normalizeWhitespace(solutionText);
		TrailingAnswerStripResult stripResult = stripTrailingAnswerLines(normalized);

		String finalAnswer = stripResult.trailingAnswer();

		if (finalAnswer == null) {
			return stripResult.body();
		}
		if (stripResult.body() == null || stripResult.body().isBlank()) {
			return ANSWER_LINE_PREFIX + " " + finalAnswer;
		}
		return stripResult.body() + "\n\n" + ANSWER_LINE_PREFIX + " " + finalAnswer;
	}

	private static TrailingAnswerStripResult stripTrailingAnswerLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return new TrailingAnswerStripResult(null, null);
		}

		String[] lines = solutionText.split("\n");
		int endIndex = skipTrailingBlankLines(lines, lines.length - 1);
		String trailingAnswer = null;

		while (endIndex >= 0 && isFinalAnswerLine(lines[endIndex])) {
			trailingAnswer = trailingAnswer != null ? trailingAnswer : extractAnswerValue(lines[endIndex]);
			endIndex = skipTrailingBlankLines(lines, endIndex - 1);
		}
		return buildStripResult(lines, endIndex, trailingAnswer);
	}

	private static boolean isFinalAnswerLine(String line) {
		return FINAL_ANSWER_LINE_PATTERN.matcher(line).matches();
	}

	private static TrailingAnswerStripResult buildStripResult(String[] lines, int endIndex, String trailingAnswer) {
		if (endIndex < 0) {
			return new TrailingAnswerStripResult(null, trailingAnswer);
		}
		String bodyText = collectBody(lines, endIndex);
		return bodyText.isBlank()
			? new TrailingAnswerStripResult(null, trailingAnswer)
			: new TrailingAnswerStripResult(bodyText, trailingAnswer);
	}

	private static int skipTrailingBlankLines(String[] lines, int from) {
		int index = from;
		while (index >= 0 && lines[index].trim().isEmpty()) {
			index -= 1;
		}
		return index;
	}

	private static String collectBody(String[] lines, int endIndex) {
		StringBuilder body = new StringBuilder();
		for (int index = 0; index <= endIndex; index++) {
			appendLine(body, lines[index]);
		}
		return body.toString().trim();
	}

	private static String extractAnswerValue(String answerLine) {
		if (answerLine == null) {
			return null;
		}
		int separatorIndex = findAnswerSeparatorIndex(answerLine);
		if (separatorIndex < 0 || separatorIndex + 1 >= answerLine.length()) {
			return null;
		}
		String value = trimAnswerDecorations(answerLine.substring(separatorIndex + 1).trim());
		return value.isBlank() ? null : value;
	}

	private static int findAnswerSeparatorIndex(String answerLine) {
		int separatorIndex = answerLine.indexOf(':');
		return separatorIndex >= 0 ? separatorIndex : answerLine.indexOf('：');
	}

	private static String trimAnswerDecorations(String rawValue) {
		String value = rawValue;
		while (value.startsWith("*") || value.startsWith("\"") || value.startsWith("'") || value.startsWith("\\")) {
			value = value.substring(1).trim();
		}
		while (value.endsWith("*") || value.endsWith("\"") || value.endsWith("'")) {
			value = value.substring(0, value.length() - 1).trim();
		}
		return value;
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
		int compareLength = Math.min(Math.min(first.length(), second.length()), PREFIX_COMPARE_MAX_LENGTH);
		if (compareLength < PREFIX_COMPARE_MIN_LENGTH) {
			return false;
		}
		int matched = 0;
		for (int index = 0; index < compareLength; index++) {
			if (first.charAt(index) == second.charAt(index)) {
				matched += 1;
			}
		}
		return (double)matched / compareLength >= PREFIX_SIMILARITY_THRESHOLD;
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
