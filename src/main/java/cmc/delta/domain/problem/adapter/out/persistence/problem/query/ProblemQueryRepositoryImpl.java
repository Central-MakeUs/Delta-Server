package cmc.delta.domain.problem.adapter.out.persistence.problem.query;

import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.ProblemDetailQuerySupport;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.dto.ProblemDetailRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListCondition;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.dto.ProblemListRow;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.list.ProblemListQuerySupport;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.type.ProblemTypeTagQuerySupport;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.type.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.application.port.out.problem.query.ProblemQueryPort;

import java.util.List;
import java.util.Optional;

import cmc.delta.domain.problem.application.port.out.problem.query.ProblemTypeTagQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemQueryRepositoryImpl implements ProblemQueryPort, ProblemTypeTagQueryPort {

	private final ProblemListQuerySupport listQuerySupport;
	private final ProblemDetailQuerySupport detailQuerySupport;
	private final ProblemTypeTagQuerySupport typeTagQuerySupport;

	@Override
	public Page<ProblemListRow> findMyProblemList(Long userId, ProblemListCondition condition, Pageable pageable) {
		return listQuerySupport.findMyProblemList(userId, condition, pageable);
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
