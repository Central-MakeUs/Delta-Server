package cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail;

import static com.querydsl.core.types.Projections.constructor;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.model.problem.QProblem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemDetailQuerySupport {

	private final JPAQueryFactory queryFactory;

	public Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");
		QProblemType type = QProblemType.problemType;

		ProblemDetailRow row = queryFactory
			.select(constructor(
				ProblemDetailRow.class,
				problem.id,

				subject.id,
				subject.name,

				unit.id,
				unit.name,

				type.id,
				type.name,
				problem.originalStorageKey,

				problem.answerFormat,
				problem.answerChoiceNo,
				problem.answerValue,
				problem.memoText,

				problem.completedAt,
				problem.createdAt))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.join(problem.finalType, type)
			.where(
				problem.user.id.eq(userId),
				problem.id.eq(problemId))
			.fetchOne();

		return Optional.ofNullable(row);
	}
}
