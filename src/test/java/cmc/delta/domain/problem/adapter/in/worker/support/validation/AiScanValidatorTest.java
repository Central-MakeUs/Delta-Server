package cmc.delta.domain.problem.adapter.in.worker.support.validation;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.adapter.in.worker.exception.OcrTextEmptyException;
import cmc.delta.domain.problem.adapter.in.worker.support.WorkerFixtures;
import cmc.delta.domain.problem.adapter.in.worker.support.ocr.LineDataSignalExtractor;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiScanValidatorTest {

	private final AiScanValidator sut = new AiScanValidator(
		new LineDataSignalExtractor(new com.fasterxml.jackson.databind.ObjectMapper()));

	@Test
	@DisplayName("OCR 텍스트는 공백을 하나로 정규화하고 trim한다")
	void normalize_whitespace() {
		// given
		ProblemScan scan = WorkerFixtures.ocrDone(WorkerFixtures.user(1L), "  a \n  b\t\tc  ");

		// when
		AiScanValidator.AiValidatedInput out = sut.validateAndNormalize(1L, scan);

		// then
		assertThat(out.normalizedOcrText()).isEqualTo("a b c");
	}

	@Test
	@DisplayName("OCR 텍스트가 비어있으면 OcrTextEmptyException")
	void empty_throws() {
		// given
		ProblemScan scan = WorkerFixtures.ocrDone(WorkerFixtures.user(1L), "   ");

		// when & then
		assertThatThrownBy(() -> sut.validateAndNormalize(1L, scan))
			.isInstanceOf(OcrTextEmptyException.class);
	}
}
