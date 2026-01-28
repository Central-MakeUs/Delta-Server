package cmc.delta.domain.problem.adapter.out.persistence.problem.query;

import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.ProblemDetailQuerySupport;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.ProblemListQuerySupport;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.type.ProblemTypeTagQuerySupport;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import cmc.delta.domain.problem.application.port.in.support.PageQuery;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemTypeTagQueryPort;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.application.port.out.support.CursorPageResult;
import cmc.delta.domain.problem.application.port.out.support.PageResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemQueryRepositoryImpl implements ProblemQueryPort, ProblemTypeTagQueryPort {

	private final ProblemListQuerySupport listQuerySupport;
	private final ProblemDetailQuerySupport detailQuerySupport;
	private final ProblemTypeTagQuerySupport typeTagQuerySupport;

	@Override
	public PageResult<ProblemListRow> findMyProblemList(
		Long userId,
		ProblemListCondition condition,
		PageQuery pageQuery) {
		Pageable pageable = PageRequest.of(pageQuery.page(), pageQuery.size());
		Page<ProblemListRow> page = listQuerySupport.findMyProblemList(userId, condition, pageable);
		return new PageResult<>(
			page.getContent(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages());
	}

	@Override
	public CursorPageResult<ProblemListRow> findMyProblemListCursor(
		Long userId,
		ProblemListCondition condition,
		CursorQuery cursorQuery) {
		return listQuerySupport.findMyProblemListCursor(userId, condition, cursorQuery);
	}

	@Override
	public Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId) {
		return detailQuerySupport.findMyProblemDetail(userId, problemId);
	}

	@Override
	public List<ProblemTypeTagRow> findTypeTagsByProblemIds(List<Long> problemIds) {
		return typeTagQuerySupport.findTypeTagsByProblemIds(problemIds);
	}

	@Override
	public List<ProblemTypeTagRow> findTypeTagsByProblemId(Long problemId) {
		return typeTagQuerySupport.findTypeTagsByProblemId(problemId);
	}
}
