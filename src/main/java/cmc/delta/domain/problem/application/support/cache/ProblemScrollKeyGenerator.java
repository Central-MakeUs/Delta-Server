package cmc.delta.domain.problem.application.support.cache;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.application.port.in.support.CursorQuery;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component("problemScrollKeyGenerator")
public class ProblemScrollKeyGenerator implements KeyGenerator {

	private static final String VERSION = "v=6";

	private final ProblemScrollCacheEpochStore epochStore;

	public ProblemScrollKeyGenerator(ProblemScrollCacheEpochStore epochStore) {
		this.epochStore = epochStore;
	}

	@Override
	public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
		Long userId = (Long)params[0];
		ProblemListCondition condition = (ProblemListCondition)params[1];
		CursorQuery cursorQuery = (CursorQuery)params[2];

		long epoch = epochStore.getEpoch(userId);

		String subjectIds = safeIds(condition.subjectIds());
		String unitIds = safeIds(condition.unitIds());
		String typeIds = safeIds(condition.typeIds());
		String sort = (condition.sort() == null) ? "-" : condition.sort().name();
		String status = (condition.status() == null) ? "-" : condition.status().name();

		String lastId = (cursorQuery.lastId() == null) ? "-" : String.valueOf(cursorQuery.lastId());
		String lastCreatedAt = time(cursorQuery.lastCreatedAt());
		String size = String.valueOf(cursorQuery.size());

		return VERSION
			+ ":u=" + userId
			+ ":e=" + epoch
			+ ":subs=" + subjectIds
			+ ":units=" + unitIds
			+ ":types=" + typeIds
			+ ":sort=" + sort
			+ ":status=" + status
			+ ":lastId=" + lastId
			+ ":lastAt=" + lastCreatedAt
			+ ":size=" + size;
	}

	private String safeIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return "-";
		}
		String joined = String.join(",", ids.stream()
			.filter(v -> v != null && !v.isBlank())
			.map(String::trim)
			.filter(v -> !v.isEmpty())
			.distinct()
			.sorted()
			.toList());
		return joined.isEmpty() ? "-" : joined;
	}

	private String time(LocalDateTime t) {
		return (t == null) ? "-" : t.toString();
	}
}
