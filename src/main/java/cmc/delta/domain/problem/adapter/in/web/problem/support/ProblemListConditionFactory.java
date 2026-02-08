package cmc.delta.domain.problem.adapter.in.web.problem.support;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.request.MyProblemScrollRequest;
import cmc.delta.domain.problem.application.port.in.problem.query.ProblemListCondition;
import cmc.delta.domain.problem.model.enums.ProblemListSort;
import cmc.delta.domain.problem.model.enums.ProblemStatusFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProblemListConditionFactory {

	public ProblemListCondition from(MyProblemListRequest query) {
		ProblemListSort sort = (query.sort() == null) ? ProblemListSort.RECENT : query.sort();
		ProblemStatusFilter status = (query.status() == null) ? ProblemStatusFilter.ALL : query.status();

		return new ProblemListCondition(
			normalizeIds(query.subjectIds()),
			normalizeIds(query.unitIds()),
			normalizeIds(query.typeIds()),
			sort,
			status);
	}

	public ProblemListCondition from(MyProblemScrollRequest query) {
		ProblemListSort sort = (query.sort() == null) ? ProblemListSort.RECENT : query.sort();
		ProblemStatusFilter status = (query.status() == null) ? ProblemStatusFilter.ALL : query.status();

		return new ProblemListCondition(
			normalizeIds(query.subjectIds()),
			normalizeIds(query.unitIds()),
			normalizeIds(query.typeIds()),
			sort,
			status);
	}

	private List<String> normalizeIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return ids.stream()
			.filter(v -> v != null)
			.flatMap(v -> expandJsonArrayString(v).stream())
			.map(String::trim)
			.filter(v -> !v.isEmpty())
			.distinct()
			.sorted()
			.toList();
	}

	private List<String> expandJsonArrayString(String raw) {
		String t = raw == null ? "" : raw.trim();
		if (!looksLikeJsonArrayString(t)) {
			return List.of(t);
		}
		String inner = t.substring(1, t.length() - 1).trim();
		if (inner.isEmpty()) {
			return List.of();
		}

		String[] parts = inner.split(",");
		List<String> out = new ArrayList<>(parts.length);
		for (String part : parts) {
			String item = stripDoubleQuotes(part.trim());
			if (!item.isEmpty()) {
				out.add(item);
			}
		}
		return out;
	}

	private boolean looksLikeJsonArrayString(String t) {
		return t.length() >= 2 && t.charAt(0) == '[' && t.charAt(t.length() - 1) == ']';
	}

	private String stripDoubleQuotes(String t) {
		if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
			return t.substring(1, t.length() - 1);
		}
		return t;
	}
}
