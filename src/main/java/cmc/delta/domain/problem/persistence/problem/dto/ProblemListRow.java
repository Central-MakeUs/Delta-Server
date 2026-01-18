package cmc.delta.domain.problem.persistence.problem.dto;

import java.time.LocalDateTime;

public class ProblemListRow {

	private final Long problemId;

	private final String subjectId;
	private final String subjectName;

	private final String unitId;
	private final String unitName;

	private final String typeId;
	private final String typeName;

	private final Long assetId;
	private final String storageKey;

	private final LocalDateTime createdAt;

	public ProblemListRow(
		Long problemId,
		String subjectId,
		String subjectName,
		String unitId,
		String unitName,
		String typeId,
		String typeName,
		Long assetId,
		String storageKey,
		LocalDateTime createdAt
	) {
		this.problemId = problemId;
		this.subjectId = subjectId;
		this.subjectName = subjectName;
		this.unitId = unitId;
		this.unitName = unitName;
		this.typeId = typeId;
		this.typeName = typeName;
		this.assetId = assetId;
		this.storageKey = storageKey;
		this.createdAt = createdAt;
	}

	public Long getProblemId() { return problemId; }
	public String getSubjectId() { return subjectId; }
	public String getSubjectName() { return subjectName; }
	public String getUnitId() { return unitId; }
	public String getUnitName() { return unitName; }
	public String getTypeId() { return typeId; }
	public String getTypeName() { return typeName; }
	public Long getAssetId() { return assetId; }
	public String getStorageKey() { return storageKey; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
