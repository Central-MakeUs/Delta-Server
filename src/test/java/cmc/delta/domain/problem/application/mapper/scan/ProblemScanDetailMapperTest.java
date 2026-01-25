package cmc.delta.domain.problem.application.mapper.scan;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse.PredictedTypeResponse;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanDetailProjection;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanDetailMapperTest {

	private final ProblemScanDetailMapper mapper = new ProblemScanDetailMapper();

	@Test
	@DisplayName("scan detail 매핑: enum은 name으로 변환되고 이미지/본문/시간 필드가 매핑")
	void toDetailResponse_mapsFields() {
		// given
		ScanDetailProjection p = mock(ScanDetailProjection.class);
		when(p.getScanId()).thenReturn(10L);
		when(p.getStatus()).thenReturn(ScanStatus.AI_DONE);
		when(p.getHasFigure()).thenReturn(true);
		when(p.getRenderMode()).thenReturn(RenderMode.LATEX);
		when(p.getAssetId()).thenReturn(100L);
		when(p.getWidth()).thenReturn(1200);
		when(p.getHeight()).thenReturn(800);
		when(p.getOcrPlainText()).thenReturn("ocr");
		when(p.getAiProblemLatex()).thenReturn("p");
		when(p.getAiSolutionLatex()).thenReturn("s");
		when(p.getCreatedAt()).thenReturn(LocalDateTime.now());
		when(p.getOcrCompletedAt()).thenReturn(LocalDateTime.now());
		when(p.getAiCompletedAt()).thenReturn(LocalDateTime.now());
		when(p.getFailReason()).thenReturn(null);

		ProblemScanDetailResponse.AiClassification ai = mapper.toAiClassification(
			p,
			new SubjectInfo("S1", "대단원"),
			List.of(new PredictedTypeResponse("T1", "유형", 1, java.math.BigDecimal.ONE)));

		// when
		ProblemScanDetailResponse res = mapper.toDetailResponse(p, "https://read", ai);

		// then
		assertThat(res.scanId()).isEqualTo(10L);
		assertThat(res.status()).isEqualTo("AI_DONE");
		assertThat(res.renderMode()).isEqualTo("LATEX");
		assertThat(res.hasFigure()).isTrue();
		assertThat(res.originalImage().assetId()).isEqualTo(100L);
		assertThat(res.originalImage().viewUrl()).isEqualTo("https://read");
		assertThat(res.originalImage().width()).isEqualTo(1200);
		assertThat(res.originalImage().height()).isEqualTo(800);
		assertThat(res.ocrPlainText()).isEqualTo("ocr");
		assertThat(res.ai().subjectId()).isEqualTo("S1");
		assertThat(res.ai().predictedTypes()).hasSize(1);
	}

	@Test
	@DisplayName("scan detail 매핑: predictedTypes가 null이면 빈 리스트로 처리")
	void toAiClassification_whenPredictedTypesNull_thenEmptyList() {
		// given
		ScanDetailProjection p = mock(ScanDetailProjection.class);
		when(p.getPredictedUnitId()).thenReturn("U1");
		when(p.getPredictedUnitName()).thenReturn("단원");

		// when
		ProblemScanDetailResponse.AiClassification ai = mapper.toAiClassification(p, SubjectInfo.empty(), null);

		// then
		assertThat(ai.predictedTypes()).isEmpty();
	}
}
