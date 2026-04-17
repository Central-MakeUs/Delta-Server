package cmc.delta.domain.problem.application.validation.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.exception.ProblemException;
import cmc.delta.domain.problem.application.exception.ProblemStateException;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.problem.ProblemRepositoryPort;
import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.model.enums.ScanStatus;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.global.error.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemCreateScanValidatorTest {

	private ProblemScanRepositoryPort scanRepositoryPort;
	private ProblemRepositoryPort problemRepositoryPort;
	private ProblemCreateScanValidator sut;

	@BeforeEach
	void setUp() {
		scanRepositoryPort = mock(ProblemScanRepositoryPort.class);
		problemRepositoryPort = mock(ProblemRepositoryPort.class);
		sut = new ProblemCreateScanValidator(scanRepositoryPort, problemRepositoryPort);
	}

	@Test
	@DisplayName("getOwnedScan: 본인 소유 스캔이면 반환한다")
	void getOwnedScan_whenOwned_returnsScan() {
		// given
		Long userId = 1L;
		Long scanId = 10L;
		ProblemScan scan = mock(ProblemScan.class);
		when(scanRepositoryPort.findOwnedById(scanId, userId)).thenReturn(Optional.of(scan));

		// when
		ProblemScan result = sut.getOwnedScan(userId, scanId);

		// then
		assertThat(result).isSameAs(scan);
	}

	@Test
	@DisplayName("getOwnedScan: 스캔이 없거나 다른 유저 소유이면 PROBLEM_SCAN_NOT_FOUND")
	void getOwnedScan_whenNotOwned_throwsScanNotFound() {
		// given
		Long userId = 1L;
		Long scanId = 10L;
		when(scanRepositoryPort.findOwnedById(scanId, userId)).thenReturn(Optional.empty());

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.getOwnedScan(userId, scanId),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_SCAN_NOT_FOUND);
	}

	@Test
	@DisplayName("validateScanIsAiDone: AI_DONE 상태이면 예외 없음")
	void validateScanIsAiDone_whenAiDone_doesNotThrow() {
		// given
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getStatus()).thenReturn(ScanStatus.AI_DONE);

		// when/then
		assertThatCode(() -> sut.validateScanIsAiDone(scan)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateScanIsAiDone: UPLOADED 상태이면 PROBLEM_SCAN_NOT_READY")
	void validateScanIsAiDone_whenUploaded_throwsNotReady() {
		// given
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getStatus()).thenReturn(ScanStatus.UPLOADED);

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.validateScanIsAiDone(scan),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_SCAN_NOT_READY);
	}

	@Test
	@DisplayName("validateScanIsAiDone: OCR_DONE 상태이면 PROBLEM_SCAN_NOT_READY")
	void validateScanIsAiDone_whenOcrDone_throwsNotReady() {
		// given
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getStatus()).thenReturn(ScanStatus.OCR_DONE);

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.validateScanIsAiDone(scan),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_SCAN_NOT_READY);
	}

	@Test
	@DisplayName("validateScanIsAiDone: FAILED 상태이면 PROBLEM_SCAN_NOT_READY")
	void validateScanIsAiDone_whenFailed_throwsNotReady() {
		// given
		ProblemScan scan = mock(ProblemScan.class);
		when(scan.getStatus()).thenReturn(ScanStatus.FAILED);

		// when
		ProblemException ex = catchThrowableOfType(
			() -> sut.validateScanIsAiDone(scan),
			ProblemException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_SCAN_NOT_READY);
	}

	@Test
	@DisplayName("validateProblemNotAlreadyCreated: 문제가 없으면 예외 없음")
	void validateProblemNotAlreadyCreated_whenNotExists_doesNotThrow() {
		// given
		Long scanId = 10L;
		when(problemRepositoryPort.existsByScanId(scanId)).thenReturn(false);

		// when/then
		assertThatCode(() -> sut.validateProblemNotAlreadyCreated(scanId)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateProblemNotAlreadyCreated: 이미 문제가 생성되어 있으면 PROBLEM_ALREADY_CREATED")
	void validateProblemNotAlreadyCreated_whenExists_throwsAlreadyCreated() {
		// given
		Long scanId = 10L;
		when(problemRepositoryPort.existsByScanId(scanId)).thenReturn(true);

		// when
		ProblemStateException ex = catchThrowableOfType(
			() -> sut.validateProblemNotAlreadyCreated(scanId),
			ProblemStateException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_ALREADY_CREATED);
	}

	@Test
	@DisplayName("validateFileNotEmpty: 파일이 있으면 예외 없음")
	void validateFileNotEmpty_whenFilePresent_doesNotThrow() {
		// given
		UploadFile file = mock(UploadFile.class);
		when(file.isEmpty()).thenReturn(false);

		// when/then
		assertThatCode(() -> sut.validateFileNotEmpty(file)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateFileNotEmpty: 파일이 null이면 ProblemValidationException")
	void validateFileNotEmpty_whenNull_throwsValidation() {
		// when/then
		assertThatThrownBy(() -> sut.validateFileNotEmpty(null))
			.isInstanceOf(ProblemValidationException.class);
	}

	@Test
	@DisplayName("validateFileNotEmpty: 파일이 비어있으면 ProblemValidationException")
	void validateFileNotEmpty_whenEmpty_throwsValidation() {
		// given
		UploadFile file = mock(UploadFile.class);
		when(file.isEmpty()).thenReturn(true);

		// when/then
		assertThatThrownBy(() -> sut.validateFileNotEmpty(file))
			.isInstanceOf(ProblemValidationException.class);
	}
}
