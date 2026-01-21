package cmc.delta.domain.problem.application.mapper.support;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.CurriculumItemResponse;

public interface ProblemCurriculumItemSupport {

	default CurriculumItemResponse toItem(String id, String name) {
		if (id == null) return null;
		return new CurriculumItemResponse(id, name);
	}
}
