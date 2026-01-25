package cmc.delta.domain.problem.model.scan;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class ProblemScanTypePredictionId implements Serializable {

	@Column(name = "scan_id", nullable = false)
	private Long scanId;

	@Column(name = "type_id", length = 50, nullable = false)
	private String typeId;

	public ProblemScanTypePredictionId(Long scanId, String typeId) {
		this.scanId = scanId;
		this.typeId = typeId;
	}

	public Long getScanId() {
		return scanId;
	}

	public String getTypeId() {
		return typeId;
	}
}
