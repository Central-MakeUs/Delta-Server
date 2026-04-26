package cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail;

import static com.querydsl.core.types.Projections.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.querydsl.jpa.impl.JPAQueryFactory;

import cmc.delta.domain.curriculum.model.QProblemType;
import cmc.delta.domain.curriculum.model.QUnit;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemTypeTagRow;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.problem.QProblem;
import cmc.delta.domain.problem.model.problem.QProblemTypeTag;
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

		List<FlatRow> rows = queryFactory
			.select(constructor(
				FlatRow.class,
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
				type.id, type.name))
			.from(problem)
			.join(problem.finalUnit, unit)
			.leftJoin(unit.parent, subject)
			.leftJoin(tag).on(tag.problem.id.eq(problem.id))
			.leftJoin(tag.type, type)
			.where(
				problem.user.id.eq(userId),
				problem.id.eq(problemId))
			.orderBy(type.sortOrder.asc(), type.id.asc())
			.fetch();

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		List<ProblemTypeTagRow> types = rows.stream()
			.filter(r -> r.typeId() != null)
			.map(r -> new ProblemTypeTagRow(problemId, r.typeId(), r.typeName()))
			.toList();

		return Optional.of(rows.get(0).toProblemDetailRow(types));
	}

	private record FlatRow(
		Long problemId,
		String subjectId, String subjectName,
		String unitId, String unitName,
		String storageKey,
		AnswerFormat answerFormat,
		Integer answerChoiceNo,
		String answerValue,
		String memoText,
		LocalDateTime completedAt,
		LocalDateTime createdAt,
		String typeId, String typeName) {

		ProblemDetailRow toProblemDetailRow(List<ProblemTypeTagRow> types) {
			return new ProblemDetailRow(
				problemId,
				subjectId, subjectName,
				unitId, unitName,
				storageKey,
				answerFormat,
				answerChoiceNo,
				answerValue,
				memoText,
				completedAt,
				createdAt,
				types);
		}
	}
}
