package cmc.delta.domain.problem.application.query.mapper;

import cmc.delta.domain.problem.api.problem.dto.response.CurriculumItemResponse;
import cmc.delta.domain.problem.api.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.persistence.problem.query.dto.ProblemDetailRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemDetailMapper {

	@Mapping(target = "problemId", source = "row.problemId")
	@Mapping(target = "answerFormat", source = "row.answerFormat")
	@Mapping(target = "answerChoiceNo", source = "row.answerChoiceNo")
	@Mapping(target = "answerValue", source = "row.answerValue")
	@Mapping(target = "solutionText", source = "row.solutionText")
	@Mapping(target = "completedAt", source = "row.completedAt")
	@Mapping(target = "createdAt", source = "row.createdAt")

	@Mapping(target = "subject", expression = "java(toItem(row.subjectId(), row.subjectName()))")
	@Mapping(target = "unit", expression = "java(toItem(row.unitId(), row.unitName()))")
	@Mapping(target = "type", expression = "java(toItem(row.typeId(), row.typeName()))")

	@Mapping(
		target = "originalImage",
		expression = "java(new ProblemDetailResponse.OriginalImageResponse(row.assetId(), viewUrl))"
	)
	@Mapping(
		target = "completed",
		expression = "java(row.completedAt() != null)"
	)
	ProblemDetailResponse toResponse(ProblemDetailRow row, String viewUrl);

	default CurriculumItemResponse toItem(String id, String name) {
		if (id == null) {
			return null;
		}
		return new CurriculumItemResponse(id, name);
	}
}
