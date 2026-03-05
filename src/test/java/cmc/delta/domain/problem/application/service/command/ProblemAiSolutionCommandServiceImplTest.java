package cmc.delta.domain.problem.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemAiSolutionRequestResponse;
import cmc.delta.domain.problem.application.port.out.ai.ProblemSolveAiClient;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolvePrompt;
import cmc.delta.domain.problem.application.port.out.ai.dto.ProblemAiSolveResult;
import cmc.delta.domain.problem.application.port.out.problem.ProblemAiSolutionTaskRepositoryPort;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.application.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.problem.ProblemAiSolutionTask;
import cmc.delta.domain.user.model.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemAiSolutionCommandServiceImplTest {

	private final ProblemRepositoryPort problemRepositoryPort = mock(ProblemRepositoryPort.class);
	private final ProblemAiSolutionTaskRepositoryPort taskRepositoryPort = mock(ProblemAiSolutionTaskRepositoryPort.class);
	private final ProblemSolveAiClient problemSolveAiClient = mock(ProblemSolveAiClient.class);
	private final ObjectStorageReader objectStorageReader = mock(ObjectStorageReader.class);
	private final Clock fixedClock = Clock.fixed(Instant.parse("2026-03-05T00:00:00Z"), ZoneOffset.UTC);

	private final ProblemAiSolutionCommandServiceImpl service = new ProblemAiSolutionCommandServiceImpl(
		problemRepositoryPort,
		taskRepositoryPort,
		problemSolveAiClient,
		objectStorageReader,
		fixedClock);

	@Test
	@DisplayName("AI 풀이 요청은 즉시 PENDING 응답을 반환한다")
	void requestMyProblemAiSolution_returnsPendingImmediately() {
		Problem problem = createProblem("2");
		when(problemRepositoryPort.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(problem));
		when(taskRepositoryPort.findByProblemIdForUpdate(10L)).thenReturn(Optional.empty());
		when(taskRepositoryPort.save(any(ProblemAiSolutionTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProblemAiSolutionRequestResponse response = service.requestMyProblemAiSolution(1L, 10L);

		assertThat(response.status()).isEqualTo("PENDING");
		verify(problemSolveAiClient, never()).solveProblem(any(ProblemAiSolvePrompt.class));
	}

	@Test
	@DisplayName("워커 처리 단계에서 성공 시 READY로 저장된다")
	void processNextPendingTask_marksReady() {
		ProblemAiSolutionTask task = pendingTask(createProblem("2"));
		when(taskRepositoryPort.findNextPendingForUpdate(any(LocalDateTime.class))).thenReturn(Optional.of(task));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenReturn(new ProblemAiSolveResult(
			null,
			"풀이 본문\n\n정답: 2\n\n**정답:** 2"));

		service.processNextPendingTask();

		assertThat(task.getStatus().name()).isEqualTo("READY");
		assertThat(task.getSolutionText()).isEqualTo("풀이 본문\n\n정답: 2");
	}

	@Test
	@DisplayName("워커 처리 단계에서 예외가 나면 FAILED로 저장된다")
	void processNextPendingTask_marksFailed() {
		ProblemAiSolutionTask task = pendingTask(createProblem("2"));
		when(taskRepositoryPort.findNextPendingForUpdate(any(LocalDateTime.class))).thenReturn(Optional.of(task));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenThrow(new RuntimeException("boom"));

		service.processNextPendingTask();

		assertThat(task.getStatus().name()).isEqualTo("FAILED");
		assertThat(task.getFailureReason()).isEqualTo("boom");
	}

	private ProblemAiSolutionTask pendingTask(Problem problem) {
		return ProblemAiSolutionTask.createPending(
			problem,
			"v1",
			"hash",
			"문제 본문",
			AnswerFormat.TEXT,
			"2",
			null,
			LocalDateTime.now(fixedClock));
	}

	private Problem createProblem(String answerValue) {
		return Problem.create(
			mock(User.class),
			null,
			"problems/1.jpg",
			mock(Unit.class),
			mock(ProblemType.class),
			RenderMode.IMAGE_ONLY,
			"문제 본문",
			AnswerFormat.TEXT,
			answerValue,
			null,
			null);
	}
}
