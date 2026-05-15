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
import cmc.delta.domain.problem.application.support.command.SolutionTextNormalizer;
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

	private static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
	private static final String MIME_TYPE_IMAGE_PNG = "image/png";
	private static final String MIME_TYPE_IMAGE_WEBP = "image/webp";
	private static final String MIME_TYPE_IMAGE_HEIC = "image/heic";

	private static final String REPEATED_GARBAGE_SENTENCE = "A$ 에서 $M N$ 에 내린 수선은 외접원의 중심 $O$ 를 지난다.";
	private static final int REPEATED_GARBAGE_THRESHOLD = 10;

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

		problemRepositoryPort.incrementAiSolutionCount(problemId);

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
			return toRequestResponse(taskRepositoryPort.save(createdTask), false);
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
		taskRepositoryPort.findNextPendingForUpdate(LocalDateTime.now(clock))
			.ifPresent(this::executePendingSolve);
	}

	private void executePendingSolve(ProblemAiSolutionTask task) {
		LocalDateTime startedAt = LocalDateTime.now(clock);
		log.debug("AI 풀이 비동기 실행 시작 taskId={} problemId={} promptVersion={} requestedAt={}",
			task.getId(), task.getProblem().getId(), task.getPromptVersion(), task.getRequestedAt());
		task.markProcessing(startedAt);

		try {
			byte[] imageBytes = loadProblemImageBytes(task);
			String imageMimeType = resolveImageMimeType(task);
			ProblemAiSolvePrompt prompt = new ProblemAiSolvePrompt(imageBytes, imageMimeType, null, null, null);
			ProblemAiSolveResult solveResult = problemSolveAiClient.solveProblem(prompt);

			String normalizedText = SolutionTextNormalizer.normalize(solveResult.solutionText());

			LocalDateTime completedAt = LocalDateTime.now(clock);
			task.markReady(solveResult.solutionLatex(), normalizedText, completedAt);
			log.debug("AI 풀이 비동기 실행 성공 taskId={} problemId={} elapsedSeconds={} completedAt={}",
				task.getId(), task.getProblem().getId(), elapsedSeconds(startedAt, completedAt), completedAt);
		} catch (Exception exception) {
			LocalDateTime failedAt = LocalDateTime.now(clock);
			String failureReason = extractFailureReason(exception);
			task.markTerminalFailure(failureReason, failedAt);
			log.debug(
				"AI 풀이 비동기 실행 실패 taskId={} problemId={} elapsedSeconds={} failureReason={} exceptionClass={} message={}",
				task.getId(), task.getProblem().getId(), elapsedSeconds(startedAt, failedAt),
				failureReason, exception.getClass().getSimpleName(), exception.getMessage());
		}
	}

	private boolean shouldRegenerateReadyTask(ProblemAiSolutionTask task) {
		if (!"READY".equals(task.getStatus().name())) {
			return false;
		}
		String solutionText = task.getSolutionText();
		if (solutionText == null || solutionText.isBlank()) {
			return true;
		}
		return SolutionTextNormalizer.looksLikeRawJsonDump(solutionText) || containsSevereKnownRepetition(solutionText);
	}

	private boolean containsSevereKnownRepetition(String solutionText) {
		return countOccurrences(solutionText, REPEATED_GARBAGE_SENTENCE) >= REPEATED_GARBAGE_THRESHOLD;
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

	private String calculateInputHash(Problem problem) {
		String payload = String.join("|",
			normalize(problem.getProblemMarkdown()),
			normalize(problem.getAnswerFormat() == null ? null : problem.getAnswerFormat().name()),
			normalize(problem.getAnswerValue()),
			normalize(problem.getAnswerChoiceNo() == null ? null : String.valueOf(problem.getAnswerChoiceNo())));
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
		}
	}

	private String normalize(String value) {
		return value == null ? NULL_SENTINEL : value;
	}

	private byte[] loadProblemImageBytes(ProblemAiSolutionTask task) {
		String key = task.getProblem().getOriginalStorageKey();
		if (key == null || key.isBlank()) {
			throw ProblemException.originalAssetNotFound();
		}
		return objectStorageReader.readBytes(key);
	}

	private String resolveImageMimeType(ProblemAiSolutionTask task) {
		String key = task.getProblem().getOriginalStorageKey();
		if (key == null || key.isBlank()) {
			return MIME_TYPE_IMAGE_JPEG;
		}
		String lower = key.toLowerCase();
		if (lower.endsWith(".png")) return MIME_TYPE_IMAGE_PNG;
		if (lower.endsWith(".webp")) return MIME_TYPE_IMAGE_WEBP;
		if (lower.endsWith(".heic") || lower.endsWith(".heif")) return MIME_TYPE_IMAGE_HEIC;
		return MIME_TYPE_IMAGE_JPEG;
	}

	private String extractFailureReason(Exception exception) {
		if (exception instanceof BusinessException be && be.getErrorCode() != null) {
			return be.getErrorCode().code();
		}
		if (exception.getMessage() == null || exception.getMessage().isBlank()) {
			return "AI_SOLVE_FAILED";
		}
		return exception.getMessage();
	}

	private long elapsedSeconds(LocalDateTime startedAt, LocalDateTime endedAt) {
		return Duration.between(startedAt, endedAt).toSeconds();
	}

	private ProblemAiSolutionRequestResponse toRequestResponse(ProblemAiSolutionTask task, boolean reused) {
		return new ProblemAiSolutionRequestResponse(task.getId(), task.getStatus().name(), reused,
			task.getRequestedAt());
	}
}
