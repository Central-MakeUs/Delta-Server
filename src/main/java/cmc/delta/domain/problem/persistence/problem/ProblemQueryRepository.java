package cmc.delta.domain.problem.persistence.problem;

import cmc.delta.domain.problem.persistence.problem.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.dto.ProblemListRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProblemQueryRepository {
	Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable);
}
