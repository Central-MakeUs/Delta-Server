package cmc.delta.domain.problem.adapter.out.persistence.problem.query.list;

import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.NumberExpression;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProblemListOrderResolver {

	private final ProblemListCountExpressions countExpressions;

	public OrderSpecifier<?>[] resolve(Long userId, ProblemListCondition condition, ProblemListQuerySupport.Paths p) {
		ProblemListSort sort = (condition.sort() == null) ? ProblemListSort.RECENT : condition.sort();

		List<OrderSpecifier<?>> orders = new ArrayList<>(3);

		switch (sort) {
			case OLDEST -> {
				orders.add(p.problem.createdAt.asc());
				orders.add(p.problem.id.asc());
			}
			case UNIT_MOST, UNIT_LEAST -> {
				NumberExpression<Long> subjectCnt = countExpressions.subjectCount(userId, condition, p.subject.id);
				orders.add(sort == ProblemListSort.UNIT_MOST ? subjectCnt.desc() : subjectCnt.asc());
				orders.add(p.problem.createdAt.desc());
				orders.add(p.problem.id.desc());
			}
			case TYPE_MOST, TYPE_LEAST -> {
				NumberExpression<Long> typeCnt = countExpressions.typeCount(userId, condition, p.type.id);
				orders.add(sort == ProblemListSort.TYPE_MOST ? typeCnt.desc() : typeCnt.asc());
				orders.add(p.problem.createdAt.desc());
				orders.add(p.problem.id.desc());
			}
			case RECENT -> {
				orders.add(p.problem.createdAt.desc());
				orders.add(p.problem.id.desc());
			}
		}

		return orders.toArray(new OrderSpecifier<?>[0]);
	}
}
