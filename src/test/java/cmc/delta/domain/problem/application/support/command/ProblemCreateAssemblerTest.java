package cmc.delta.domain.problem.application.support.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.domain.problem.application.port.in.problem.command.CreateWrongAnswerCardCommand;
import cmc.delta.domain.problem.application.port.out.asset.AssetRepositoryPort;
import cmc.delta.domain.problem.application.validation.command.ProblemCreateScanValidator;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.enums.AnswerFormat;
import cmc.delta.domain.problem.model.enums.RenderMode;
import cmc.delta.domain.problem.model.problem.Problem;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemCreateAssemblerTest {

	@Test
	@DisplayName("Problem assemble: renderMode가 null이면 PROBLEM_SCAN_RENDER_MODE_MISSING")
	void assemble_whenRenderModeNull_thenThrows() {
		// given
		ProblemCreateAssembler sut = new ProblemCreateAssembler(
			mock(ProblemCreateScanValidator.class),
			mock(AssetRepositoryPort.class));
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getRenderMode()).thenReturn(null);

		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			"ans",
			"sol");

		// when
		ProblemStateException ex = catchThrowableOfType(
			() -> sut.assemble(mock(User.class), scan, mock(Unit.class), mock(ProblemType.class), cmd),
			ProblemStateException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_SCAN_RENDER_MODE_MISSING);
	}

	@Test
	@DisplayName("Problem assemble: OCR 텍스트가 null/blank면 fallback markdown을 사용")
	void assemble_whenOcrTextNullOrBlank_thenFallbackMarkdown() {
		// given
		AssetRepositoryPort assetRepositoryPort = mock(AssetRepositoryPort.class);
		ProblemCreateAssembler sut = new ProblemCreateAssembler(
			mock(ProblemCreateScanValidator.class),
			assetRepositoryPort);
		ProblemScan scan1 = mock(ProblemScan.class);
		when(scan1.getId()).thenReturn(1L);
		when(scan1.getRenderMode()).thenReturn(RenderMode.LATEX);
		when(scan1.getOcrPlainText()).thenReturn(null);
		Asset a1 = mock(Asset.class);
		when(a1.getStorageKey()).thenReturn("s3/k1");
		when(assetRepositoryPort.findOriginalByScanId(1L)).thenReturn(java.util.Optional.of(a1));

		ProblemScan scan2 = mock(ProblemScan.class);
		when(scan2.getId()).thenReturn(2L);
		when(scan2.getRenderMode()).thenReturn(RenderMode.LATEX);
		when(scan2.getOcrPlainText()).thenReturn("   ");
		Asset a2 = mock(Asset.class);
		when(a2.getStorageKey()).thenReturn("s3/k2");
		when(assetRepositoryPort.findOriginalByScanId(2L)).thenReturn(java.util.Optional.of(a2));

		CreateWrongAnswerCardCommand cmd = new CreateWrongAnswerCardCommand(
			1L,
			"U1",
			java.util.List.of("T1"),
			AnswerFormat.TEXT,
			null,
			"ans",
			"sol");

		// when
		Problem p1 = sut.assemble(mock(User.class), scan1, mock(Unit.class), mock(ProblemType.class), cmd);
		Problem p2 = sut.assemble(mock(User.class), scan2, mock(Unit.class), mock(ProblemType.class), cmd);

		// then
		assertThat(p1.getProblemMarkdown()).isEqualTo("(문제 텍스트 없음)");
		assertThat(p2.getProblemMarkdown()).isEqualTo("(문제 텍스트 없음)");
	}
}
