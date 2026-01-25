package cmc.delta.domain.problem.application.port.out.problem.query;

import java.util.Optional;

import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.domain.problem.application.port.out.support.PageResult;

public interface ProblemQueryPort {

	PageResult<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, PageQuery pageQuery);

	Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId);
}
