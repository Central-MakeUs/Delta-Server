package cmc.delta.domain.problem.adapter.in.worker.support.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.adapter.in.worker.exception.OriginalAssetNotFoundException;
import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.model.asset.Asset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OcrScanValidatorTest {

	@Test
	@DisplayName("원본 Asset이 있으면 그대로 반환한다")
	void requireOriginalAsset_returnsAsset() {
		// given
		AssetJpaRepository repo = mock(AssetJpaRepository.class);
		OcrScanValidator sut = new OcrScanValidator(repo);

		Long scanId = 1L;
		Asset asset = mock(Asset.class);
		when(repo.findOriginalByScanId(scanId)).thenReturn(Optional.of(asset));

		// when
		Asset result = sut.requireOriginalAsset(scanId);

		// then
		assertThat(result).isSameAs(asset);
		verify(repo).findOriginalByScanId(scanId);
	}

	@Test
	@DisplayName("원본 Asset이 없으면 OriginalAssetNotFoundException을 던진다")
	void requireOriginalAsset_whenMissing_throws() {
		// given
		AssetJpaRepository repo = mock(AssetJpaRepository.class);
		OcrScanValidator sut = new OcrScanValidator(repo);

		Long scanId = 99L;
		when(repo.findOriginalByScanId(scanId)).thenReturn(Optional.empty());

		// when
		OriginalAssetNotFoundException ex = catchThrowableOfType(
			() -> sut.requireOriginalAsset(scanId),
			OriginalAssetNotFoundException.class
		);

		// then
		assertThat(ex).isNotNull();
		verify(repo).findOriginalByScanId(scanId);
	}
}
