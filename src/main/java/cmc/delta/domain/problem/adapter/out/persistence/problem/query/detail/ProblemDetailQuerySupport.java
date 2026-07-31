package cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail;

import static com.querydsl.core.types.Projections.*;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.model.problem.QProblemTypeTag;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemDetailQuerySupport {

	private final JPAQueryFactory queryFactory;

	public Optional<ProblemDetailRow> findMyProblemDetail(Long userId, Long problemId) {
		List<ProblemDetailFlatRow> rows = fetchDetailFlatRows(userId, problemId);

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		List<ProblemTypeTagRow> types = collectTypeTags(rows, problemId);
		return Optional.of(rows.get(0).toProblemDetailRow(types));
	}

	private List<ProblemDetailFlatRow> fetchDetailFlatRows(Long userId, Long problemId) {
		QProblem problem = QProblem.problem;
		QUnit unit = QUnit.unit;
		QUnit subject = new QUnit("subject");
		QProblemTypeTag tag = QProblemTypeTag.problemTypeTag;
		QProblemType type = QProblemType.problemType;

		return buildDetailFlatQuery(problem, unit, subject, tag, type)
			.where(
				problem.user.id.eq(userId),
				problem.id.eq(problemId))
			.orderBy(type.sortOrder.asc(), type.id.asc())
			.fetch();
	}

	private JPAQuery<ProblemDetailFlatRow> buildDetailFlatQuery(
		QProblem problem,
		QUnit unit,
		QUnit subject,
		QProblemTypeTag tag,
		QProblemType type) {
		return queryFactory
			.select(detailFlatRowProjection(problem, unit, subject, type))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.leftJoin(tag).on(tag.problem.id.eq(problem.id))
			.leftJoin(tag.type, type);
	}

	private ConstructorExpression<ProblemDetailFlatRow> detailFlatRowProjection(
		QProblem problem,
		QUnit unit,
		QUnit subject,
		QProblemType type) {
		return constructor(
			ProblemDetailFlatRow.class,
			problem.id,
			subject.id, subject.name,
			unit.id, unit.name,
			problem.originalStorageKey,
			problem.answerFormat,
			problem.answerChoiceNo,
			problem.answerValue,
			problem.memoText,
			problem.completedAt,
			problem.createdAt,
			type.id, type.name);
	}

	private List<ProblemTypeTagRow> collectTypeTags(List<ProblemDetailFlatRow> rows, Long problemId) {
		return rows.stream()
			.filter(r -> r.typeId() != null)
			.map(r -> new ProblemTypeTagRow(problemId, r.typeId(), r.typeName()))
			.toList();
	}
}
