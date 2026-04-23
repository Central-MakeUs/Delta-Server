package cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail;

import static com.querydsl.core.types.Projections.constructor;
import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.model.problem.QProblemTypeTag;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProblemDetailQuerySupport {

	private final JPAQueryFactory queryFactory;

	public Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");
		QProblemTypeTag tag = QProblemTypeTag.problemTypeTag;
		QProblemType type = QProblemType.problemType;

		Map<Long, ProblemDetailRow> result = queryFactory
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.leftJoin(tag).on(tag.problem.id.eq(problem.id))
			.leftJoin(tag.type, type)
			.where(
				problem.user.id.eq(userId),
				problem.id.eq(problemId))
			.orderBy(type.sortOrder.asc(), type.id.asc())
			.transform(groupBy(problem.id).as(constructor(
				ProblemDetailRow.class,
				problem.id,
				subject.id,
				subject.name,
				unit.id,
				unit.name,
				problem.originalStorageKey,
				problem.answerFormat,
				problem.answerChoiceNo,
				problem.answerValue,
				problem.memoText,
				problem.completedAt,
				problem.createdAt,
				list(constructor(
					ProblemTypeTagRow.class,
					problem.id,
					type.id,
					type.name))
			)));

		return Optional.ofNullable(result.get(problemId));
	}
}
