package cmc.delta.domain.problem.application.query.mapper;

import cmc.delta.domain.problem.api.problem.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemListItemResponse;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemListRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemListMapper {

	@Mapping(target = "problemId", source = "row.problemId")
	@Mapping(target = "createdAt", source = "row.createdAt")
	@Mapping(target = "subject", expression = "java(toItem(row.getSubjectId(), row.getSubjectName()))")
	@Mapping(target = "unit", expression = "java(toItem(row.getUnitId(), row.getUnitName()))")
	@Mapping(target = "type", expression = "java(toItem(row.getTypeId(), row.getTypeName()))")
	@Mapping(target = "previewImage",
		expression = "java(new ProblemListItemResponse.PreviewImageResponse(row.getAssetId(), viewUrl))")
	ProblemListItemResponse toResponse(ProblemListRow row, String viewUrl);

	default CurriculumItemResponse toItem(String id, String name) {
		if (id == null) {
			return null;
		}
		return new CurriculumItemResponse(id, name);
	}
}
