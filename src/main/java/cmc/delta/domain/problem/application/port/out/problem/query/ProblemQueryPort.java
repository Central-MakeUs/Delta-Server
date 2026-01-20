package cmc.delta.domain.problem.application.port.out.problem.query;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListRow;

public interface ProblemQueryPort {

	Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable);

	Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId);
}
