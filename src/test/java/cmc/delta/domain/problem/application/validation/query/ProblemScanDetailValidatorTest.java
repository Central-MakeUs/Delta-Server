package cmc.delta.domain.problem.application.validation.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanDetailValidatorTest {

	private final ProblemScanDetailValidator validator = new ProblemScanDetailValidator();

	@Test
	@DisplayName("scan detail 검증: assetId/storageKey가 없거나 blank면 PROBLEM_ASSET_NOT_FOUND")
	void validateOriginalAsset_whenMissing_thenThrows() {
		// given
		ScanDetailProjection p = mock(ScanDetailProjection.class);
		when(p.getAssetId()).thenReturn(null);
		when(p.getStorageKey()).thenReturn(" ");

		// when
		BusinessException ex = catchThrowableOfType(
			() -> validator.validateOriginalAsset(p),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_ASSET_NOT_FOUND);
	}
}
