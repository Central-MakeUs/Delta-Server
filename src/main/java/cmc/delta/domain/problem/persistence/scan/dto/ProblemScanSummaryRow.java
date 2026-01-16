package cmc.delta.domain.problem.persistence.scan.dto;

import cmc.delta.domain.problem.model.enums.ScanStatus;

public class ProblemScanSummaryRow {

	private final Long scanId;
	private final Long userId;
	private final ScanStatus status;

	private final Long assetId;
	private final String storageKey;

	private final String unitId;
	private final String unitName;

	private final String typeId;
	private final String typeName;

	private final Boolean needsReview;

	public ProblemScanSummaryRow(
		Long scanId,
		Long userId,
		ScanStatus status,
		Long assetId,
		String storageKey,
		String unitId,
		String unitName,
		String typeId,
		String typeName,
		Boolean needsReview
	) {
		this.scanId = scanId;
		this.userId = userId;
		this.status = status;
		this.assetId = assetId;
		this.storageKey = storageKey;
		this.unitId = unitId;
		this.unitName = unitName;
		this.typeId = typeId;
		this.typeName = typeName;
		this.needsReview = needsReview;
	}

	public Long getScanId() { return scanId; }
	public Long getUserId() { return userId; }
	public ScanStatus getStatus() { return status; }

	public Long getAssetId() { return assetId; }
	public String getStorageKey() { return storageKey; }

	public String getUnitId() { return unitId; }
	public String getUnitName() { return unitName; }

	public String getTypeId() { return typeId; }
	public String getTypeName() { return typeName; }

	public Boolean getNeedsReview() { return needsReview; }
}
