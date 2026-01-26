package cmc.delta.domain.problem.application.mapper.problem;

import cmc.delta.domain.problem.application.mapper.support.ProblemCurriculumItemSupport;
import cmc.delta.domain.problem.application.port.in.problem.result.ProblemListItemResponse;
import cmc.delta.domain.problem.application.port.out.problem.query.dto.ProblemListRow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProblemListMapper extends ProblemCurriculumItemSupport {

	@Mapping(target = "problemId", source = "row.problemId")
	@Mapping(target = "createdAt", source = "row.createdAt")
	@Mapping(target = "subject", expression = "java(toItem(row.subjectId(), row.subjectName()))")
	@Mapping(target = "unit", expression = "java(toItem(row.unitId(), row.unitName()))")
	@Mapping(target = "types", expression = "java(List.of())")
	@Mapping(target = "previewImage", expression = "java(new ProblemListItemResponse.PreviewImageResponse(row.assetId(), viewUrl))")
	@Mapping(target = "isCompleted", expression = "java(row.completedAt() != null)")
	ProblemListItemResponse toResponse(ProblemListRow row, String viewUrl);
}
