package cmc.delta.domain.problem.application.mapper.problem;

import cmc.delta.domain.problem.adapter.in.web.problem.dto.response.ProblemDetailResponse;
import cmc.delta.domain.problem.adapter.out.persistence.problem.query.detail.dto.ProblemDetailRow;
import cmc.delta.domain.problem.application.mapper.support.ProblemCurriculumItemSupport;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemDetailMapper extends ProblemCurriculumItemSupport {

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
	@Mapping(target = "completed", expression = "java(row.completedAt() != null)")
	ProblemDetailResponse toResponse(ProblemDetailRow row, String viewUrl);
}
