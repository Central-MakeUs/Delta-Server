package cmc.delta.domain.problem.application.port.out.problem.query;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.support.CursorPageResult;
import cmc.delta.domain.problem.application.port.out.support.PageResult;
import java.util.Optional;

public interface ProblemQueryPort {

	PageResult<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, PageQuery pageQuery);

	CursorPageResult<ProblemListRow> findMyProblemListCursor(Long userId, ProblemListCondition condition,
		CursorQuery cursorQuery);

	Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId);
}
