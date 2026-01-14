package cmc.delta.domain.problem.application.port.ai;

public record AiCurriculumResult(
	String predictedSubjectId,
	String predictedUnitId,
	String predictedTypeId,
	double confidence,
	String subjectCandidatesJson,
	String unitCandidatesJson,
	String typeCandidatesJson,
	String aiDraftJson
) {}
