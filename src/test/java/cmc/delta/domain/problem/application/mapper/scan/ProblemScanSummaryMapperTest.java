package cmc.delta.domain.problem.application.mapper.scan;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.problem.application.mapper.support.SubjectInfo;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.port.in.support.CurriculumItemResponse;
import cmc.delta.domain.problem.application.port.out.scan.query.dto.ScanListRow;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemScanSummaryMapperTest {

	private final ProblemScanSummaryMapper mapper = new ProblemScanSummaryMapper();

	@Test
	@DisplayName("scan summary 매핑: row/viewUrl/subject/types로 응답을 생성")
	void toSummaryResponse_mapsFields() {
		// given
		ScanListRow row = new ScanListRow(
			10L,
			1L,
			ScanStatus.OCR_DONE,
			100L,
			"s3/k.png",
			"U1",
			"단원",
			"T1",
			"유형",
			true);
		SubjectInfo subject = new SubjectInfo("S1", "대단원");
		List<CurriculumItemResponse> types = List.of(new CurriculumItemResponse("T1", "유형"));

		// when
		ProblemScanSummaryResponse res = mapper.toSummaryResponse(row, "https://read/s3/k.png", subject, types);

		// then
		assertThat(res.scanId()).isEqualTo(10L);
		assertThat(res.status()).isEqualTo(ScanStatus.OCR_DONE);
		assertThat(res.originalImage().assetId()).isEqualTo(100L);
		assertThat(res.originalImage().viewUrl()).isEqualTo("https://read/s3/k.png");
		assertThat(res.classification().subject()).isEqualTo(new CurriculumItemResponse("S1", "대단원"));
		assertThat(res.classification().unit()).isEqualTo(new CurriculumItemResponse("U1", "단원"));
		assertThat(res.classification().types()).containsExactly(new CurriculumItemResponse("T1", "유형"));
		assertThat(res.classification().needsReview()).isTrue();
	}

	@Test
	@DisplayName("scan summary 매핑: unitId가 null이면 unit=null")
	void toSummaryResponse_whenUnitIdNull_thenUnitNull() {
		// given
		ScanListRow row = new ScanListRow(
			10L,
			1L,
			ScanStatus.OCR_DONE,
			100L,
			"s3/k.png",
			null,
			null,
			null,
			null,
			null);
		SubjectInfo subject = SubjectInfo.empty();

		// when
		ProblemScanSummaryResponse res = mapper.toSummaryResponse(row, "u", subject, List.of());

		// then
		assertThat(res.classification().unit()).isNull();
		assertThat(res.classification().subject()).isNull();
		assertThat(res.classification().needsReview()).isFalse();
	}
}
