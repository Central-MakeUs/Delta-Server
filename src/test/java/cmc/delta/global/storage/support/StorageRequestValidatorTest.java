package cmc.delta.global.storage.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.global.storage.exception.StorageException;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class StorageRequestValidatorTest {

	private StorageRequestValidator sut;

	@BeforeEach
	void setUp() {
		sut = new StorageRequestValidator();
	}

	@Test
	@DisplayName("validateUploadFile: 정상 파일이면 예외 없음")
	void validateUploadFile_whenValid_doesNotThrow() {
		// given
		MockMultipartFile file = new MockMultipartFile("f", "img.jpg", "image/jpeg", new byte[100]);

		// when/then
		assertThatCode(() -> sut.validateUploadFile(file, 1024)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateUploadFile: 파일이 null이면 INVALID_REQUEST")
	void validateUploadFile_whenNull_throwsInvalidRequest() {
		// when
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadFile(null, 1024),
			StorageException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateUploadFile: 파일이 비어있으면 INVALID_REQUEST")
	void validateUploadFile_whenEmpty_throwsInvalidRequest() {
		// given
		MockMultipartFile file = new MockMultipartFile("f", "img.jpg", "image/jpeg", new byte[0]);

		// when
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadFile(file, 1024),
			StorageException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateUploadFile: 파일 크기가 maxUploadBytes를 초과하면 INVALID_REQUEST")
	void validateUploadFile_whenExceedsMaxBytes_throwsInvalidRequest() {
		// given
		MockMultipartFile file = new MockMultipartFile("f", "img.jpg", "image/jpeg", new byte[1025]);

		// when
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadFile(file, 1024),
			StorageException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateUploadFile: 파일 크기가 정확히 maxUploadBytes이면 예외 없음")
	void validateUploadFile_whenExactMaxBytes_doesNotThrow() {
		// given
		MockMultipartFile file = new MockMultipartFile("f", "img.jpg", "image/jpeg", new byte[1024]);

		// when/then
		assertThatCode(() -> sut.validateUploadFile(file, 1024)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateUploadFile: maxUploadBytes가 0 이하이면 INTERNAL_ERROR")
	void validateUploadFile_whenMaxBytesZero_throwsInternalError() {
		// given
		MockMultipartFile file = new MockMultipartFile("f", "img.jpg", "image/jpeg", new byte[1]);

		// when
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadFile(file, 0),
			StorageException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
	}

	@Test
	@DisplayName("validateStorageKey: 정상 key이면 예외 없음")
	void validateStorageKey_whenValid_doesNotThrow() {
		assertThatCode(() -> sut.validateStorageKey("uploads/user/image.jpg")).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateStorageKey: null이면 INVALID_REQUEST")
	void validateStorageKey_whenNull_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateStorageKey(null),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateStorageKey: 빈 문자열이면 INVALID_REQUEST")
	void validateStorageKey_whenBlank_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateStorageKey("   "),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateStorageKey: '..' 포함 시 경로 탐색 공격 차단 - INVALID_REQUEST")
	void validateStorageKey_whenPathTraversal_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateStorageKey("uploads/../secret/file.jpg"),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateStorageKey: 백슬래시 포함 시 경로 조작 차단 - INVALID_REQUEST")
	void validateStorageKey_whenBackslash_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateStorageKey("uploads\\secret\\file.jpg"),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	// ── resolveTtlSeconds ─────────────────────────────────────────────────────

	@Test
	@DisplayName("resolveTtlSeconds: null 입력 시 defaultTtlSeconds 반환")
	void resolveTtlSeconds_whenNull_returnsDefault() {
		int result = sut.resolveTtlSeconds(null, 3600);

		assertThat(result).isEqualTo(3600);
	}

	@Test
	@DisplayName("resolveTtlSeconds: 값 입력 시 해당 값 반환")
	void resolveTtlSeconds_whenProvided_returnsGiven() {
		int result = sut.resolveTtlSeconds(120, 3600);

		assertThat(result).isEqualTo(120);
	}

	@Test
	@DisplayName("resolveTtlSeconds: 60 미만이면 INVALID_REQUEST")
	void resolveTtlSeconds_whenBelowMin_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.resolveTtlSeconds(59, 3600),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("resolveTtlSeconds: 정확히 60이면 예외 없음")
	void resolveTtlSeconds_whenExactMin_doesNotThrow() {
		assertThatCode(() -> sut.resolveTtlSeconds(60, 3600)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("resolveTtlSeconds: default가 60 미만이면 INVALID_REQUEST")
	void resolveTtlSeconds_whenDefaultBelowMin_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.resolveTtlSeconds(null, 30),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateUploadBytes: 정상 바이트이면 예외 없음")
	void validateUploadBytes_whenValid_doesNotThrow() {
		assertThatCode(() -> sut.validateUploadBytes(new byte[]{1, 2, 3}, 1024)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("validateUploadBytes: null이면 INVALID_REQUEST")
	void validateUploadBytes_whenNull_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadBytes(null, 1024),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateUploadBytes: 비어있는 배열이면 INVALID_REQUEST")
	void validateUploadBytes_whenEmpty_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadBytes(new byte[0], 1024),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("validateUploadBytes: maxBytes 초과하면 INVALID_REQUEST")
	void validateUploadBytes_whenExceedsMax_throwsInvalidRequest() {
		StorageException ex = catchThrowableOfType(
			() -> sut.validateUploadBytes(new byte[1025], 1024),
			StorageException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
