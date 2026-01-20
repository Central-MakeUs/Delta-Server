package cmc.delta.domain.problem.application.mapper.command;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemCreateResponse;
import cmc.delta.domain.problem.model.problem.Problem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemCreateMapper {

	@Mapping(target = "problemId", source = "id")
	@Mapping(target = "scanId", source = "scan.id")
	ProblemCreateResponse toResponse(Problem problem);
}
