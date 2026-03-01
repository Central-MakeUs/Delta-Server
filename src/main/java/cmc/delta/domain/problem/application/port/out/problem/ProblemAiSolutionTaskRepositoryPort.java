package cmc.delta.domain.problem.application.port.out.problem;

import cmc.delta.domain.problem.model.problem.ProblemAiSolutionTask;
import java.util.Optional;

public interface ProblemAiSolutionTaskRepositoryPort {

	ProblemAiSolutionTask save(ProblemAiSolutionTask task);

	Optional<ProblemAiSolutionTask> findByProblemId(Long problemId);

	Optional<ProblemAiSolutionTask> findByProblemIdForUpdate(Long problemId);

	Optional<ProblemAiSolutionTask> findById(Long id);
}
