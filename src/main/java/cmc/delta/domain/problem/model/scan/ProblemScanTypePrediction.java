package cmc.delta.domain.problem.model.scan;

import cmc.delta.domain.curriculum.model.ProblemType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_scan_type_prediction", indexes = {
	@Index(name = "idx_scan_rank", columnList = "scan_id, rank_no")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uk_scan_rank", columnNames = {"scan_id", "rank_no"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemScanTypePrediction {

	@EmbeddedId
	private ProblemScanTypePredictionId id;

	@MapsId("scanId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "scan_id", nullable = false)
	private ProblemScan scan;

	@MapsId("typeId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", nullable = false)
	private ProblemType type;

	@Column(name = "rank_no", nullable = false)
	private int rankNo;

	@Column(name = "confidence", precision = 5, scale = 4)
	private BigDecimal confidence;

	public ProblemScanTypePrediction(ProblemScan scan, ProblemType type, int rankNo, BigDecimal confidence) {
		this.scan = scan;
		this.type = type;
		this.rankNo = rankNo;
		this.confidence = confidence;
		this.id = new ProblemScanTypePredictionId(scan.getId(), type.getId());
	}
}
