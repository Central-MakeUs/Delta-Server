package cmc.delta.domain.problem.application.service.command;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.port.in.problem.ProblemAiSolutionCommandUseCase;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemAiSolutionRequestResponse;
import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import cmc.delta.domain.problem.application.port.out.problem.ProblemAiSolutionTaskRepositoryPort;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.application.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.problem.ProblemAiSolutionTask;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 나중에 꼭 리팩토리 예정 데모데이가 얼마 남지않아서 급하게 짬
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemAiSolutionCommandServiceImpl implements ProblemAiSolutionCommandUseCase {

	private static final String PROMPT_VERSION = "v1";
	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String NULL_SENTINEL = "<null>";
	private static final String ANSWER_LINE_PREFIX = "정답:";
	private static final String JSON_FIELD_SOLUTION_LATEX = "\"solution_latex\"";
	private static final String REPEATED_GARBAGE_SENTENCE = "A$ 에서 $M N$ 에 내린 수선은 외접원의 중심 $O$ 를 지난다.";
	private static final int REPEATED_GARBAGE_THRESHOLD = 10;
	private static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
	private static final String MIME_TYPE_IMAGE_PNG = "image/png";
	private static final String MIME_TYPE_IMAGE_WEBP = "image/webp";
	private static final String MIME_TYPE_IMAGE_HEIC = "image/heic";
	private static final Pattern FINAL_ANSWER_LINE_PATTERN = Pattern.compile(
		"^\\s*[\\\\\"']*(?:\\*\\*)?정답(?:\\*\\*)?\\s*[:：].*");
	private static final int MAX_SAME_LONG_SENTENCE_OCCURRENCES = 1;
	private static final int LONG_SENTENCE_MIN_LENGTH = 40;
	private static final int DUPLICATE_LINE_MIN_LENGTH = 20;
	private static final int LOOP_MARKER_TRUNCATION_MIN_LINES = 12;
	private static final int LOOP_MARKER_TRUNCATION_THRESHOLD = 2;
 	private static final Pattern REASONING_LOOP_MARKER_PATTERN = Pattern.compile(
		"(?i).*(let\\s*'?s\\s+(recheck|reconsider|assume|use)|this is impossible|contradiction|다시\\s*검|재검토|모순|불가능).*"
	);

	private final ProblemRepositoryPort problemRepositoryPort;
	private final ProblemAiSolutionTaskRepositoryPort taskRepositoryPort;
	private final ProblemSolveAiClient problemSolveAiClient;
	private final ObjectStorageReader objectStorageReader;
	private final Clock clock;

	@Override
	@Transactional
	public ProblemAiSolutionRequestResponse requestMyProblemAiSolution(Long userId, Long problemId) {
		Problem problem = problemRepositoryPort.findByIdAndUserId(problemId, userId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now(clock);
		String inputHash = calculateInputHash(problem);

		Optional<ProblemAiSolutionTask> optionalTask = taskRepositoryPort.findByProblemIdForUpdate(problemId);
		if (optionalTask.isEmpty()) {
			ProblemAiSolutionTask createdTask = ProblemAiSolutionTask.createPending(
				problem,
				PROMPT_VERSION,
				inputHash,
				problem.getProblemMarkdown(),
				problem.getAnswerFormat(),
				problem.getAnswerValue(),
				problem.getAnswerChoiceNo(),
				now);
			ProblemAiSolutionTask savedTask = taskRepositoryPort.save(createdTask);
			return toRequestResponse(savedTask, false);
		}

		ProblemAiSolutionTask existingTask = optionalTask.get();
		if (shouldRegenerateReadyTask(existingTask)) {
			existingTask.requestAgain(
				PROMPT_VERSION,
				inputHash,
				problem.getProblemMarkdown(),
				problem.getAnswerFormat(),
				problem.getAnswerValue(),
				problem.getAnswerChoiceNo(),
				now);
			return toRequestResponse(existingTask, false);
		}
		if (existingTask.canReuseFor(inputHash)) {
			return new ProblemAiSolutionRequestResponse(
				existingTask.getId(),
				existingTask.getStatus().name(),
				true,
				existingTask.getRequestedAt());
		}

		existingTask.requestAgain(
			PROMPT_VERSION,
			inputHash,
			problem.getProblemMarkdown(),
			problem.getAnswerFormat(),
			problem.getAnswerValue(),
			problem.getAnswerChoiceNo(),
			now);
		return toRequestResponse(existingTask, false);
	}

	@Override
	@Transactional
	public void deleteMyProblemAiSolution(Long userId, Long problemId) {
		problemRepositoryPort.findByIdAndUserId(problemId, userId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));

		taskRepositoryPort.deleteByProblemId(problemId);
	}

	@Transactional
	public void processNextPendingTask() {
		Optional<ProblemAiSolutionTask> optionalTask = taskRepositoryPort.findNextPendingForUpdate(LocalDateTime.now(clock));
		if (optionalTask.isEmpty()) {
			return;
		}
		executePendingSolve(optionalTask.get());
	}

	private void executePendingSolve(ProblemAiSolutionTask task) {
		LocalDateTime startedAt = LocalDateTime.now(clock);
		log.debug(
			"AI 풀이 비동기 실행 시작 taskId={} problemId={} promptVersion={} requestedAt={}",
			task.getId(),
			task.getProblem().getId(),
			task.getPromptVersion(),
			task.getRequestedAt());
		task.markProcessing(startedAt);

		try {
			byte[] problemImageBytes = loadProblemImageBytes(task);
			String problemImageMimeType = resolveImageMimeType(task);
			ProblemAiSolvePrompt prompt = new ProblemAiSolvePrompt(
				problemImageBytes,
				problemImageMimeType,
				null,
				null,
				null);
			ProblemAiSolveResult solveResult = problemSolveAiClient.solveProblem(prompt);
			String normalizedSolutionText = normalizeSolutionTextForStorage(solveResult.solutionText(), task);
			LocalDateTime completedAt = LocalDateTime.now(clock);
			task.markReady(solveResult.solutionLatex(), normalizedSolutionText, completedAt);
			log.debug(
				"AI 풀이 비동기 실행 성공 taskId={} problemId={} elapsedSeconds={} completedAt={}",
				task.getId(),
				task.getProblem().getId(),
				elapsedSeconds(startedAt, completedAt),
				completedAt);
		} catch (Exception exception) {
			LocalDateTime failedAt = LocalDateTime.now(clock);
			String failureReason = extractFailureReason(exception);
			task.markTerminalFailure(failureReason, failedAt);
			log.debug(
				"AI 풀이 비동기 실행 실패 taskId={} problemId={} elapsedSeconds={} failureReason={} exceptionClass={} message={}",
				task.getId(),
				task.getProblem().getId(),
				elapsedSeconds(startedAt, failedAt),
				failureReason,
				exception.getClass().getSimpleName(),
				exception.getMessage());
		}
}

	private ProblemAiSolutionRequestResponse toRequestResponse(ProblemAiSolutionTask task, boolean reusedExistingTask) {
		return new ProblemAiSolutionRequestResponse(
			task.getId(),
			task.getStatus().name(),
			reusedExistingTask,
			task.getRequestedAt());
	}

	private String normalizeSolutionTextForStorage(String solutionText, ProblemAiSolutionTask task) {
		String normalizedSolutionText = normalizeSolutionText(solutionText);
		normalizedSolutionText = deduplicateConsecutiveLines(normalizedSolutionText);
		normalizedSolutionText = deduplicateGlobalLongLines(normalizedSolutionText);
		normalizedSolutionText = truncateReasoningLoopTail(normalizedSolutionText);
		normalizedSolutionText = sanitizeRepeatedSentences(normalizedSolutionText);
		normalizedSolutionText = collapseDuplicatedLeadingBlock(normalizedSolutionText);
		return ensureFinalAnswerLine(normalizedSolutionText, task);
	}

	private String extractFailureReason(Exception exception) {
		if (exception instanceof BusinessException businessException && businessException.getErrorCode() != null) {
			return businessException.getErrorCode().code();
		}
		if (exception.getMessage() == null || exception.getMessage().isBlank()) {
			return "AI_SOLVE_FAILED";
		}
		return exception.getMessage();
	}

	private long elapsedSeconds(LocalDateTime startedAt, LocalDateTime endedAt) {
		return Duration.between(startedAt, endedAt).toSeconds();
	}

	private String calculateInputHash(Problem problem) {
		String payload = String.join("|",
			normalize(problem.getProblemMarkdown()),
			normalize(problem.getAnswerFormat() == null ? null : problem.getAnswerFormat().name()),
			normalize(problem.getAnswerValue()),
			normalize(problem.getAnswerChoiceNo() == null ? null : String.valueOf(problem.getAnswerChoiceNo())));

		try {
			MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] digest = messageDigest.digest(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
		}
	}

	private String normalize(String value) {
		if (value == null) {
			return NULL_SENTINEL;
		}
		return value;
	}

	private String ensureFinalAnswerLine(String solutionText, ProblemAiSolutionTask task) {
		String normalizedSolutionText = normalizeSolutionText(solutionText);
		TrailingAnswerStripResult stripResult = stripTrailingAnswerLines(normalizedSolutionText);
		if (stripResult.trailingAnswer() == null) {
			return stripResult.body();
		}
		if (stripResult.body() == null || stripResult.body().isBlank()) {
			return ANSWER_LINE_PREFIX + " " + stripResult.trailingAnswer();
		}
		return stripResult.body() + "\n\n" + ANSWER_LINE_PREFIX + " " + stripResult.trailingAnswer();
	}

	private String normalizeSolutionText(String solutionText) {
		if (solutionText == null) {
			return null;
		}
		String normalized = solutionText.replace("\r\n", "\n").trim();
		if (normalized.isBlank()) {
			return null;
		}
		return normalized;
	}

	private TrailingAnswerStripResult stripTrailingAnswerLines(String solutionText) {
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

		StringBuilder bodyBuilder = new StringBuilder();
		for (int index = 0; index <= endIndex; index++) {
			if (bodyBuilder.length() > 0) {
				bodyBuilder.append('\n');
			}
			bodyBuilder.append(lines[index]);
		}

		String body = bodyBuilder.toString().trim();
		if (body.isBlank()) {
			return new TrailingAnswerStripResult(null, trailingAnswer);
		}
		return new TrailingAnswerStripResult(body, trailingAnswer);
	}

	private String extractAnswerValue(String answerLine) {
		if (answerLine == null) {
			return null;
		}
		int separatorIndex = answerLine.indexOf(':');
		if (separatorIndex < 0) {
			separatorIndex = answerLine.indexOf('：');
		}
		if (separatorIndex < 0 || separatorIndex + 1 >= answerLine.length()) {
			return null;
		}
		String answerValue = answerLine.substring(separatorIndex + 1).trim();
		while (answerValue.startsWith("*") || answerValue.startsWith("\"") || answerValue.startsWith("'")
			|| answerValue.startsWith("\\")) {
			answerValue = answerValue.substring(1).trim();
		}
		while (answerValue.endsWith("*") || answerValue.endsWith("\"") || answerValue.endsWith("'")) {
			answerValue = answerValue.substring(0, answerValue.length() - 1).trim();
		}
		if (answerValue.isBlank()) {
			return null;
		}
		return answerValue;
	}

	private String sanitizeRepeatedSentences(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] sentences = solutionText.split("(?<=[.!?])\\s+");
		if (sentences.length == 0) {
			return solutionText;
		}

		Map<String, Integer> sentenceCounts = new HashMap<>();
		StringBuilder sanitizedBuilder = new StringBuilder();
		for (String sentence : sentences) {
			String normalizedSentence = normalizeSentenceKey(sentence);
			if (normalizedSentence == null) {
				appendSentence(sanitizedBuilder, sentence);
				continue;
			}

			int nextCount = sentenceCounts.getOrDefault(normalizedSentence, 0) + 1;
			sentenceCounts.put(normalizedSentence, nextCount);
			if (nextCount > MAX_SAME_LONG_SENTENCE_OCCURRENCES) {
				continue;
			}
			appendSentence(sanitizedBuilder, sentence);
		}

		String sanitized = sanitizedBuilder.toString().trim();
		if (sanitized.isBlank()) {
			return solutionText;
		}
		return sanitized;
	}

	private String deduplicateConsecutiveLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		StringBuilder deduplicatedBuilder = new StringBuilder();
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

			if (deduplicatedBuilder.length() > 0) {
				deduplicatedBuilder.append('\n');
			}
			deduplicatedBuilder.append(line);
			if (key != null) {
				previousKey = key;
			}
		}

		String deduplicated = deduplicatedBuilder.toString().trim();
		if (deduplicated.isBlank()) {
			return solutionText;
		}
		return deduplicated;
	}

	private String deduplicateGlobalLongLines(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		Map<String, Integer> seen = new HashMap<>();
		StringBuilder deduplicatedBuilder = new StringBuilder();

		for (String line : lines) {
			String key = normalizeLineKey(line);
			if (key != null && key.length() >= DUPLICATE_LINE_MIN_LENGTH) {
				int occurrence = seen.getOrDefault(key, 0);
				if (occurrence >= 1) {
					continue;
				}
				seen.put(key, occurrence + 1);
			}

			if (deduplicatedBuilder.length() > 0) {
				deduplicatedBuilder.append('\n');
			}
			deduplicatedBuilder.append(line);
		}

		String deduplicated = deduplicatedBuilder.toString().trim();
		if (deduplicated.isBlank()) {
			return solutionText;
		}
		return deduplicated;
	}

	private String truncateReasoningLoopTail(String solutionText) {
		if (solutionText == null || solutionText.isBlank()) {
			return solutionText;
		}

		String[] lines = solutionText.split("\n");
		int loopMarkerCount = 0;
		int truncateFrom = -1;

		for (int index = 0; index < lines.length; index++) {
			String line = lines[index];
			if (!REASONING_LOOP_MARKER_PATTERN.matcher(line).matches()) {
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

		StringBuilder keptBuilder = new StringBuilder();
		for (int index = 0; index < truncateFrom; index++) {
			if (keptBuilder.length() > 0) {
				keptBuilder.append('\n');
			}
			keptBuilder.append(lines[index]);
		}

		String kept = keptBuilder.toString().trim();
		if (kept.isBlank()) {
			return solutionText;
		}
		return kept;
	}

	private String normalizeLineKey(String line) {
		if (line == null) {
			return null;
		}
		String normalized = line.trim().replaceAll("\\s+", " ");
		if (normalized.isBlank()) {
			return null;
		}
		return normalized;
	}

	private String normalizeSentenceKey(String sentence) {
		if (sentence == null) {
			return null;
		}
		String normalized = sentence.replace("\n", " ").trim().replaceAll("\\s+", " ");
		if (normalized.length() < LONG_SENTENCE_MIN_LENGTH) {
			return null;
		}
		return normalized;
	}

	private void appendSentence(StringBuilder builder, String sentence) {
		String trimmedSentence = sentence == null ? "" : sentence.trim();
		if (trimmedSentence.isBlank()) {
			return;
		}
		if (builder.length() > 0) {
			builder.append('\n');
		}
		builder.append(trimmedSentence);
	}

	private String collapseDuplicatedLeadingBlock(String solutionText) {
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

	private int findSecondBlockStart(String text, String anchor, int anchorLength) {
		int directIndex = text.indexOf(anchor, anchorLength);
		if (directIndex >= 0) {
			return directIndex;
		}

		String quotedAnchor = "\"" + anchor;
		int quotedIndex = text.indexOf(quotedAnchor, anchorLength);
		if (quotedIndex >= 0) {
			return quotedIndex + 1;
		}

		String lineAnchored = "\n" + anchor;
		int lineIndex = text.indexOf(lineAnchored, anchorLength);
		if (lineIndex >= 0) {
			return lineIndex + 1;
		}

		return -1;
	}

	private boolean isHighlySimilarPrefix(String first, String second) {
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

		double ratio = (double)matched / (double)compareLength;
		return ratio >= 0.9;
	}

	private byte[] loadProblemImageBytes(ProblemAiSolutionTask task) {
		String originalStorageKey = task.getProblem().getOriginalStorageKey();
		if (originalStorageKey == null || originalStorageKey.isBlank()) {
			throw ProblemException.originalAssetNotFound();
		}
		return objectStorageReader.readBytes(originalStorageKey);
	}

	private String resolveImageMimeType(ProblemAiSolutionTask task) {
		String originalStorageKey = task.getProblem().getOriginalStorageKey();
		if (originalStorageKey == null || originalStorageKey.isBlank()) {
			return MIME_TYPE_IMAGE_JPEG;
		}
		String lowerStorageKey = originalStorageKey.toLowerCase();
		if (lowerStorageKey.endsWith(".png")) {
			return MIME_TYPE_IMAGE_PNG;
		}
		if (lowerStorageKey.endsWith(".webp")) {
			return MIME_TYPE_IMAGE_WEBP;
		}
		if (lowerStorageKey.endsWith(".heic") || lowerStorageKey.endsWith(".heif")) {
			return MIME_TYPE_IMAGE_HEIC;
		}
		return MIME_TYPE_IMAGE_JPEG;
	}

	private boolean shouldRegenerateReadyTask(ProblemAiSolutionTask task) {
		if (!"READY".equals(task.getStatus().name())) {
			return false;
		}
		String solutionText = task.getSolutionText();
		if (solutionText == null || solutionText.isBlank()) {
			return true;
		}
		if (looksLikeRawJsonDump(solutionText)) {
			return true;
		}
		return containsSevereKnownRepetition(solutionText);
	}

	private boolean looksLikeRawJsonDump(String solutionText) {
		String trimmed = solutionText.trim();
		return trimmed.startsWith("{") && trimmed.contains(JSON_FIELD_SOLUTION_LATEX);
	}

	private boolean containsSevereKnownRepetition(String solutionText) {
		int occurrences = countOccurrences(solutionText, REPEATED_GARBAGE_SENTENCE);
		return occurrences >= REPEATED_GARBAGE_THRESHOLD;
	}

	private int countOccurrences(String source, String target) {
		if (source == null || source.isBlank() || target == null || target.isBlank()) {
			return 0;
		}
		int count = 0;
		int fromIndex = 0;
		while (true) {
			int found = source.indexOf(target, fromIndex);
			if (found < 0) {
				break;
			}
			count += 1;
			fromIndex = found + target.length();
		}
		return count;
	}

	private record TrailingAnswerStripResult(String body, String trailingAnswer) {
	}
}
