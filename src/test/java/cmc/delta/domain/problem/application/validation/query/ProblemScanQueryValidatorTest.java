package cmc.delta.domain.problem.application.validation.query;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.out.persistence.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanQueryValidatorTest {

	private final ProblemScanQueryValidator validator = new ProblemScanQueryValidator();

	@Test
	@DisplayName("scan summary 검증: assetId/storageKey가 없으면 PROBLEM_ASSET_NOT_FOUND")
	void validateHasOriginalAsset_whenMissing_thenThrows() {
		// given
		ScanListRow row = new ScanListRow(
			1L,
			1L,
			ScanStatus.UPLOADED,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		// when
		ProblemException ex = catchThrowableOfType(
			() -> validator.validateHasOriginalAsset(row),
			ProblemException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_ASSET_NOT_FOUND);
	}
}
