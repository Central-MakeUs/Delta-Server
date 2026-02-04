package cmc.delta.domain.problem.application.mapper.support;

public record SubjectInfo(String subjectId, String subjectName) {

	public static SubjectInfo empty() {
		return new SubjectInfo(null, null);
	}
}
