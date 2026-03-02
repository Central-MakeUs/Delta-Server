package cmc.delta.domain.problem.application.support.cache;

import cmc.delta.domain.problem.application.port.in.problem.query.ProblemStatsCondition;
import cmc.delta.domain.problem.model.enums.ProblemStatsSort;
import org.springframework.stereotype.Component;

@Component("problemStatsCacheKeyFactory")
public class ProblemStatsCacheKeyFactory {

	private static final String VERSION = "v=1";
	private static final String EMPTY = "-";

	private final ProblemStatsCacheEpochStore epochStore;

	public ProblemStatsCacheKeyFactory(ProblemStatsCacheEpochStore epochStore) {
		this.epochStore = epochStore;
	}

	public String unitStatsKey(Long userId, ProblemStatsCondition condition) {
		long epoch = epochStore.getEpoch(userId);
		return VERSION
			+ ":u=" + userId
			+ ":e=" + epoch
			+ ":subject=" + normalize(condition == null ? null : condition.subjectId())
			+ ":unit=" + normalize(condition == null ? null : condition.unitId())
			+ ":sort=" + normalizeSort(condition == null ? null : condition.sort());
	}

	public String typeStatsKey(Long userId, ProblemStatsCondition condition) {
		long epoch = epochStore.getEpoch(userId);
		return VERSION
			+ ":u=" + userId
			+ ":e=" + epoch
			+ ":subject=" + normalize(condition == null ? null : condition.subjectId())
			+ ":unit=" + normalize(condition == null ? null : condition.unitId())
			+ ":type=" + normalize(condition == null ? null : condition.typeId())
			+ ":sort=" + normalizeSort(condition == null ? null : condition.sort());
	}

	public String monthlyProgressKey(Long userId, Integer year, Integer month) {
		long epoch = epochStore.getEpoch(userId);
		return VERSION
			+ ":u=" + userId
			+ ":e=" + epoch
			+ ":year=" + normalizeNumber(year)
			+ ":month=" + normalizeNumber(month);
	}

	private String normalize(String value) {
		if (value == null) {
			return EMPTY;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return EMPTY;
		}
		return trimmed;
	}

	private String normalizeSort(ProblemStatsSort sort) {
		if (sort == null) {
			return EMPTY;
		}
		return sort.name();
	}

	private String normalizeNumber(Integer value) {
		if (value == null) {
			return EMPTY;
		}
		return String.valueOf(value);
	}
}
