package cmc.delta.domain.problem.application.port.out.problem.query;

import cmc.delta.domain.problem.adapter.out.persistence.problem.query.type.dto.ProblemTypeTagRow;
import java.util.List;

public interface ProblemTypeTagQueryPort {
	List<ProblemTypeTagRow> findTypeTagsByProblemIds(List<Long> problemIds);
	List<ProblemTypeTagRow> findTypeTagsByProblemId(Long problemId);
}
