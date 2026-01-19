package cmc.delta.domain.problem.application.query.mapper;

import cmc.delta.domain.problem.api.problem.dto.request.MyProblemListRequest;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListCondition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemListConditionMapper {

	@Mapping(target = "subjectId", expression = "java(trimToNull(req.subjectId()))")
	@Mapping(target = "unitId", expression = "java(trimToNull(req.unitId()))")
	@Mapping(target = "typeId", expression = "java(trimToNull(req.typeId()))")
	@Mapping(target = "sort", source = "sort")
	@Mapping(target = "status", source = "status")
	ProblemListCondition toCondition(MyProblemListRequest req);

	default String trimToNull(String v) {
		if (v == null) return null;
		String t = v.trim();
		return t.isEmpty() ? null : t;
	}
}
