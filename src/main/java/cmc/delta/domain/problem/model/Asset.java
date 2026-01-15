package cmc.delta.domain.problem.model;

import cmc.delta.domain.problem.model.enums.AssetType;
import cmc.delta.global.persistence.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
	name = "asset",
	indexes = {
		@Index(name = "idx_asset_storage_key", columnList = "storage_key", unique = true),
		@Index(name = "idx_asset_scan_type", columnList = "scan_id, asset_type")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_asset_scan_type_slot", columnNames = {"scan_id", "asset_type", "slot"})
	})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends BaseCreatedEntity {

	private static final int DEFAULT_SLOT = 0;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "scan_id", nullable = false)
	private ProblemScan scan;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false, length = 30)
	private AssetType assetType;

	@Column(nullable = false)
	private int slot;

	@Column(name = "storage_key", nullable = false, length = 255)
	private String storageKey;

	private Integer width;
	private Integer height;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "json")
	private String meta;

	private Asset(
		ProblemScan scan,
		AssetType assetType,
		int slot,
		String storageKey,
		Integer width,
		Integer height,
		String meta
	) {
		this.scan = scan;
		this.assetType = assetType;
		this.slot = slot;
		this.storageKey = storageKey;
		this.width = width;
		this.height = height;
		this.meta = meta;
	}

	public static Asset createOriginal(ProblemScan scan, String storageKey, Integer width, Integer height) {
		return new Asset(
			scan,
			AssetType.ORIGINAL,
			DEFAULT_SLOT,
			storageKey,
			width,
			height,
			null
		);
	}
}
