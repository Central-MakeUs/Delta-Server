package cmc.delta.domain.problem.application.service.query;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.port.in.problem.ProblemAiSolutionQueryUseCase;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemAiSolutionDetailResponse;
import cmc.delta.domain.problem.application.port.out.problem.ProblemAiSolutionTaskRepositoryPort;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.model.problem.ProblemAiSolutionTask;
import cmc.delta.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemAiSolutionQueryServiceImpl implements ProblemAiSolutionQueryUseCase {

	private final ProblemRepositoryPort problemRepositoryPort;
	private final ProblemAiSolutionTaskRepositoryPort taskRepositoryPort;

	@Override
	public ProblemAiSolutionDetailResponse getMyProblemAiSolution(Long userId, Long problemId) {
		problemRepositoryPort.findByIdAndUserId(problemId, userId)
			.orElseThrow(() -> new ProblemException(ErrorCode.PROBLEM_NOT_FOUND));

		return taskRepositoryPort.findByProblemId(problemId)
			.map(this::toDetailResponse)
			.orElseGet(ProblemAiSolutionDetailResponse::notRequested);
	}

	private ProblemAiSolutionDetailResponse toDetailResponse(ProblemAiSolutionTask task) {
		return new ProblemAiSolutionDetailResponse(
			task.getId(),
			task.getStatus().name(),
			task.getFailureReason(),
			task.getRequestedAt(),
			task.getStartedAt(),
			task.getCompletedAt(),
			toSolutionContent(task));
	}

	private ProblemAiSolutionDetailResponse.SolutionContent toSolutionContent(ProblemAiSolutionTask task) {
		if (task.getSolutionText() == null) {
			return null;
		}
		return new ProblemAiSolutionDetailResponse.SolutionContent(task.getSolutionText());
	}
}
