package cmc.delta.domain.problem.adapter.out.persistence.problem.query.type;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.model.problem.QProblemTypeTag;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemTypeTagQuerySupport {

	private final JPAQueryFactory queryFactory;

	public List<ProblemTypeTagRow> findTypeTagsByProblemIds(List<Long> problemIds) {
		if (problemIds == null || problemIds.isEmpty()) return Collections.emptyList();

		QProblemTypeTag tag = QProblemTypeTag.problemTypeTag;
		QProblemType type = QProblemType.problemType;

		return queryFactory
			.select(constructor(
				ProblemTypeTagRow.class,
				tag.problem.id,
				type.id,
				type.name
			))
			.from(tag)
			.join(tag.type, type)
			.where(tag.problem.id.in(problemIds))
			.orderBy(tag.problem.id.asc(), type.sortOrder.asc(), type.id.asc())
			.fetch();
	}

	public List<ProblemTypeTagRow> findTypeTagsByProblemId(Long problemId) {
		QProblemTypeTag tag = QProblemTypeTag.problemTypeTag;
		QProblemType type = QProblemType.problemType;

		return queryFactory
			.select(constructor(
				ProblemTypeTagRow.class,
				tag.problem.id,
				type.id,
				type.name
			))
			.from(tag)
			.join(tag.type, type)
			.where(tag.problem.id.eq(problemId))
			.orderBy(type.sortOrder.asc(), type.id.asc())
			.fetch();
	}
}
