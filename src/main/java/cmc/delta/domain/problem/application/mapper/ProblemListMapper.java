package cmc.delta.domain.problem.application.mapper;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.dto.ProblemListRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemListMapper {

	@Mapping(target = "problemId", source = "row.problemId")
	@Mapping(target = "createdAt", source = "row.createdAt")
	@Mapping(target = "subject", expression = "java(toItem(row.subjectId(), row.subjectName()))")
	@Mapping(target = "unit", expression = "java(toItem(row.unitId(), row.unitName()))")
	@Mapping(target = "type", expression = "java(toItem(row.typeId(), row.typeName()))")
	@Mapping(
		target = "previewImage",
		expression = "java(new ProblemListItemResponse.PreviewImageResponse(row.assetId(), viewUrl))"
	)
	ProblemListItemResponse toResponse(ProblemListRow row, String viewUrl);

	default CurriculumItemResponse toItem(String id, String name) {
		if (id == null) {
			return null;
		}
		return new CurriculumItemResponse(id, name);
	}
}
