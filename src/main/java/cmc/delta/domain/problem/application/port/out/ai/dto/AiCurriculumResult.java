package cmc.delta.domain.problem.application.port.out.ai.dto;

public record AiCurriculumResult(
	boolean isMathProblem,
	String predictedSubjectId,
	String predictedUnitId,
	String predictedTypeId,
	double confidence,
	String subjectCandidatesJson,
	String unitCandidatesJson,
	String typeCandidatesJson,
	String aiDraftJson) {
}
