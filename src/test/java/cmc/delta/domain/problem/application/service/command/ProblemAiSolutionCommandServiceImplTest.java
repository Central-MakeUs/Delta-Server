package cmc.delta.domain.problem.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
	@DisplayName("AI 응답 끝에 정답 라인이 여러 개여도 저장 시 최종 정답 라인은 하나로 통일된다")
	void requestMyProblemAiSolution_stripsTrailingDuplicateAnswerLines() {
		Problem problem = createProblem("2");
		when(problemRepositoryPort.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(problem));
		when(taskRepositoryPort.findByProblemIdForUpdate(10L)).thenReturn(Optional.empty());
		when(taskRepositoryPort.save(any(ProblemAiSolutionTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenReturn(new ProblemAiSolveResult(
			null,
			"풀이 본문\n\n정답: 2\n\n**정답:** 2"));

		ProblemAiSolutionRequestResponse response = service.requestMyProblemAiSolution(1L, 10L);

		ArgumentCaptor<ProblemAiSolutionTask> taskCaptor = ArgumentCaptor.forClass(ProblemAiSolutionTask.class);
		org.mockito.Mockito.verify(taskRepositoryPort).save(taskCaptor.capture());
		ProblemAiSolutionTask savedTask = taskCaptor.getValue();

		assertThat(response.status()).isEqualTo("READY");
		assertThat(savedTask.getSolutionText()).isEqualTo("풀이 본문\n\n정답: 2");
	}

	@Test
	@DisplayName("AI 응답 본문에서 긴 문장 반복이 과도하면 저장 전에 반복을 1회로 줄인다")
	void requestMyProblemAiSolution_reducesExcessiveSentenceRepetition() {
		Problem problem = createProblem("2");
		when(problemRepositoryPort.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(problem));
		when(taskRepositoryPort.findByProblemIdForUpdate(10L)).thenReturn(Optional.empty());
		when(taskRepositoryPort.save(any(ProblemAiSolutionTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});

		String repeatedSentence = "이 문장은 반복 제거 기능의 동작을 검증하기 위해 의도적으로 길게 작성한 테스트용 문장입니다.";
		String repeatedBody = String.join(" ",
			repeatedSentence,
			repeatedSentence,
			repeatedSentence,
			repeatedSentence,
			repeatedSentence);
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenReturn(
			new ProblemAiSolveResult(null, repeatedBody));

		service.requestMyProblemAiSolution(1L, 10L);

		ArgumentCaptor<ProblemAiSolutionTask> taskCaptor = ArgumentCaptor.forClass(ProblemAiSolutionTask.class);
		org.mockito.Mockito.verify(taskRepositoryPort).save(taskCaptor.capture());
		ProblemAiSolutionTask savedTask = taskCaptor.getValue();

		assertThat(countOccurrences(savedTask.getSolutionText(), repeatedSentence)).isEqualTo(1);
		assertThat(savedTask.getSolutionText()).doesNotContain("정답: 2");
	}

	@Test
	@DisplayName("AI 응답에서 연속 중복 줄은 저장 전에 하나로 정리한다")
	void requestMyProblemAiSolution_deduplicatesConsecutiveLines() {
		Problem problem = createProblem("2");
		when(problemRepositoryPort.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(problem));
		when(taskRepositoryPort.findByProblemIdForUpdate(10L)).thenReturn(Optional.empty());
		when(taskRepositoryPort.save(any(ProblemAiSolutionTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});

		String duplicatedLine = "이 줄은 중복 제거 테스트를 위해 충분히 길게 작성한 동일 문장입니다.";
		String body = duplicatedLine + "\n" + duplicatedLine + "\n다음 줄 설명입니다.";
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenReturn(
			new ProblemAiSolveResult(null, body));

		service.requestMyProblemAiSolution(1L, 10L);

		ArgumentCaptor<ProblemAiSolutionTask> taskCaptor = ArgumentCaptor.forClass(ProblemAiSolutionTask.class);
		org.mockito.Mockito.verify(taskRepositoryPort).save(taskCaptor.capture());
		ProblemAiSolutionTask savedTask = taskCaptor.getValue();

		assertThat(countOccurrences(savedTask.getSolutionText(), duplicatedLine)).isEqualTo(1);
	}

	@Test
	@DisplayName("AI 응답이 재검토/모순 루프로 흐르면 꼬리 구간을 잘라 저장한다")
	void requestMyProblemAiSolution_truncatesReasoningLoopTail() {
		Problem problem = createProblem("2");
		when(problemRepositoryPort.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(problem));
		when(taskRepositoryPort.findByProblemIdForUpdate(10L)).thenReturn(Optional.empty());
		when(taskRepositoryPort.save(any(ProblemAiSolutionTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});

		String body = String.join("\n",
			"문제 조건을 정리한다.",
			"핵심 식을 세운다.",
			"중간 계산 1.",
			"중간 계산 2.",
			"중간 계산 3.",
			"중간 계산 4.",
			"중간 계산 5.",
			"중간 계산 6.",
			"중간 계산 7.",
			"중간 계산 8.",
			"중간 계산 9.",
			"중간 계산 10.",
			"Let's recheck this assumption.",
			"추가 계산을 시도한다.",
			"This is impossible under current assumption.",
			"이 줄은 잘려야 한다.");
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenReturn(
			new ProblemAiSolveResult(null, body));

		service.requestMyProblemAiSolution(1L, 10L);

		ArgumentCaptor<ProblemAiSolutionTask> taskCaptor = ArgumentCaptor.forClass(ProblemAiSolutionTask.class);
		org.mockito.Mockito.verify(taskRepositoryPort).save(taskCaptor.capture());
		ProblemAiSolutionTask savedTask = taskCaptor.getValue();

		assertThat(savedTask.getSolutionText()).doesNotContain("This is impossible");
		assertThat(savedTask.getSolutionText()).doesNotContain("이 줄은 잘려야 한다");
		assertThat(savedTask.getSolutionText()).doesNotContain("정답: 2");
	}

	@Test
	@DisplayName("DB 정답 값과 무관하게 AI 본문에 정답 라인이 없으면 강제 추가하지 않는다")
	void requestMyProblemAiSolution_doesNotForceDbAnswerLine() {
		Problem problem = createProblem("string");
		when(problemRepositoryPort.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(problem));
		when(taskRepositoryPort.findByProblemIdForUpdate(10L)).thenReturn(Optional.empty());
		when(taskRepositoryPort.save(any(ProblemAiSolutionTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(objectStorageReader.readBytes("problems/1.jpg")).thenReturn(new byte[] {1, 2, 3});
		when(problemSolveAiClient.solveProblem(any(ProblemAiSolvePrompt.class))).thenReturn(
			new ProblemAiSolveResult(null, "풀이 본문입니다."));

		service.requestMyProblemAiSolution(1L, 10L);

		ArgumentCaptor<ProblemAiSolutionTask> taskCaptor = ArgumentCaptor.forClass(ProblemAiSolutionTask.class);
		org.mockito.Mockito.verify(taskRepositoryPort).save(taskCaptor.capture());
		ProblemAiSolutionTask savedTask = taskCaptor.getValue();

		assertThat(savedTask.getSolutionText()).isEqualTo("풀이 본문입니다.");
		assertThat(savedTask.getSolutionText()).doesNotContain("정답:");
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

	private int countOccurrences(String source, String target) {
		int count = 0;
		int fromIndex = 0;
		while (true) {
			int found = source.indexOf(target, fromIndex);
			if (found < 0) {
				return count;
			}
			count += 1;
			fromIndex = found + target.length();
		}
	}
}
