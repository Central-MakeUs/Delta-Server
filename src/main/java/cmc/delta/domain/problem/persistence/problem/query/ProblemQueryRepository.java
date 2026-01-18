package cmc.delta.domain.problem.persistence.problem.query;

import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProblemQueryRepository {
	Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable);
}
