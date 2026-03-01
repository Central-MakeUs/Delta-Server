package cmc.delta.domain.problem.application.service.command;

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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
			return executeSyncSolve(savedTask, false);
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
			return executeSyncSolve(existingTask, false);
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
		return executeSyncSolve(existingTask, false);
	}

	private ProblemAiSolutionRequestResponse executeSyncSolve(ProblemAiSolutionTask task, boolean reusedExistingTask) {
		LocalDateTime startedAt = LocalDateTime.now(clock);
		log.debug(
			"AI 풀이 동기 실행 시작 taskId={} problemId={} promptVersion={} requestedAt={}",
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
				task.getAnswerFormat(),
				task.getAnswerValue(),
				task.getAnswerChoiceNo());
			ProblemAiSolveResult solveResult = problemSolveAiClient.solveProblem(prompt);
			String normalizedSolutionText = ensureFinalAnswerLine(solveResult.solutionText(), task);
			LocalDateTime completedAt = LocalDateTime.now(clock);
			task.markReady(solveResult.solutionLatex(), normalizedSolutionText, completedAt);
			log.debug(
				"AI 풀이 동기 실행 성공 taskId={} problemId={} elapsedSeconds={} completedAt={}",
				task.getId(),
				task.getProblem().getId(),
				elapsedSeconds(startedAt, completedAt),
				completedAt);
		} catch (Exception exception) {
			LocalDateTime failedAt = LocalDateTime.now(clock);
			String failureReason = extractFailureReason(exception);
			task.markTerminalFailure(failureReason, failedAt);
			log.debug(
				"AI 풀이 동기 실행 실패 taskId={} problemId={} elapsedSeconds={} failureReason={} exceptionClass={} message={}",
				task.getId(),
				task.getProblem().getId(),
				elapsedSeconds(startedAt, failedAt),
				failureReason,
				exception.getClass().getSimpleName(),
				exception.getMessage());
		}

		return new ProblemAiSolutionRequestResponse(
			task.getId(),
			task.getStatus().name(),
			reusedExistingTask,
			task.getRequestedAt());
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
		if (normalizedSolutionText != null && hasFinalAnswerLine(normalizedSolutionText)) {
			return normalizedSolutionText;
		}

		String finalAnswer = buildExpectedFinalAnswer(task);
		if (finalAnswer == null) {
			return normalizedSolutionText;
		}

		String answerLine = ANSWER_LINE_PREFIX + " " + finalAnswer;
		if (normalizedSolutionText == null || normalizedSolutionText.isBlank()) {
			return answerLine;
		}
		return normalizedSolutionText + "\n\n" + answerLine;
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

	private boolean hasFinalAnswerLine(String solutionText) {
		String[] lines = solutionText.split("\n");
		for (String line : lines) {
			if (normalizeAnswerLinePrefix(line).startsWith(ANSWER_LINE_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	private String normalizeAnswerLinePrefix(String line) {
		if (line == null) {
			return "";
		}
		String normalized = line.trim();
		while (normalized.startsWith("\\")) {
			normalized = normalized.substring(1).trim();
		}
		return normalized;
	}

	private String buildExpectedFinalAnswer(ProblemAiSolutionTask task) {
		if (task.getAnswerValue() != null && !task.getAnswerValue().isBlank()) {
			return task.getAnswerValue().trim();
		}
		if (task.getAnswerChoiceNo() != null) {
			return String.valueOf(task.getAnswerChoiceNo());
		}
		return null;
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
}
