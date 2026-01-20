package cmc.delta.global.storage.service;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import cmc.delta.global.storage.application.StorageService;
import cmc.delta.global.storage.adapter.out.s3.S3Properties;
import cmc.delta.global.storage.support.FakeObjectStorage;
import cmc.delta.global.storage.support.StorageFixtures;
import cmc.delta.global.storage.support.StorageKeyGenerator;
import cmc.delta.global.storage.support.StorageRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class StorageServiceTest {

	private FakeObjectStorage objectStorage;
	private StorageService storageService;
	private S3Properties properties;

	@BeforeEach
	void setUp() {
		objectStorage = new FakeObjectStorage();
		properties = props();

		StorageRequestValidator validator = new StorageRequestValidator();

		StorageKeyGenerator keyGenerator = new StorageKeyGenerator(properties);
		ReflectionTestUtils.invokeMethod(keyGenerator, "init");

		storageService = new StorageService(objectStorage, properties, validator, keyGenerator);
	}

	@Test
	@DisplayName("이미지 업로드: 정상 파일이면 업로드 결과(키/URL/사이즈)를 반환함")
	void upload_ok_returnsResult() {
		// given
		MultipartFile file = StorageFixtures.png("a.png", 12, 34);

		// when
		StorageUploadData result = storageService.uploadImage(file, "profiles");

		// then
		// keyPrefix가 붙으니까 startsWith("profiles/")는 실패함 -> contains로 체크
		assertThat(result.storageKey()).contains("/profiles/");
		assertThat(result.viewUrl()).contains(result.storageKey());
		assertThat(result.sizeBytes()).isPositive();
		assertThat(result.width()).isEqualTo(12);
		assertThat(result.height()).isEqualTo(34);
		assertThat(objectStorage.exists(result.storageKey())).isTrue();
	}

	@Test
	@DisplayName("이미지 업로드: 파일이 null이면 INVALID_REQUEST가 발생함")
	void upload_whenNull_thenInvalidRequest() {
		// given
		MultipartFile file = null;

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.uploadImage(file, "profiles"),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이미지 업로드: 빈 파일이면 INVALID_REQUEST가 발생함")
	void upload_whenEmpty_thenInvalidRequest() {
		// given
		MultipartFile file = StorageFixtures.empty("a.png");

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.uploadImage(file, "profiles"),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이미지 업로드: content-type이 이미지가 아니면 INVALID_REQUEST가 발생함")
	void upload_whenNotImage_thenInvalidRequest() {
		// given
		MultipartFile file = StorageFixtures.text("a.txt", "hello");

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.uploadImage(file, "profiles"),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이미지 업로드: 이미지로 읽을 수 없는 바이트면 INVALID_REQUEST가 발생함")
	void upload_whenCorruptImage_thenInvalidRequest() {
		// given
		MultipartFile file = StorageFixtures.corruptImage("a.png");

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.uploadImage(file, "profiles"),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("Presigned GET 발급: ttl을 주면 해당 ttl로 URL을 발급함")
	void presign_withTtl_returnsUrl() {
		// given
		String key = properties.keyPrefix() + "/profiles/test.png";

		// when
		StoragePresignedGetData result = storageService.issueReadUrl(key, 60);

		// then
		assertThat(result.url()).contains(key);
		assertThat(result.expiresInSeconds()).isEqualTo(60);
	}

	@Test
	@DisplayName("Presigned GET 발급: key가 비어있으면 INVALID_REQUEST가 발생함")
	void presign_whenBlankKey_thenInvalidRequest() {
		// given
		String key = " ";

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.issueReadUrl(key, 59),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("Presigned GET 발급: ttl이 0 이하이면 INVALID_REQUEST가 발생함")
	void presign_whenBadTtl_thenInvalidRequest() {
		// given
		String key = properties.keyPrefix() + "/profiles/test.png";

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.issueReadUrl(key, 0),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이미지 삭제: 키가 있으면 스토리지에서 삭제됨")
	void delete_ok_removesObject() {
		// given
		MultipartFile file = StorageFixtures.png("a.png", 1, 1);
		StorageUploadData uploaded = storageService.uploadImage(file, "temp");
		assertThat(objectStorage.exists(uploaded.storageKey())).isTrue();

		// when
		storageService.deleteImage(uploaded.storageKey());

		// then
		assertThat(objectStorage.exists(uploaded.storageKey())).isFalse();
	}

	@Test
	@DisplayName("이미지 삭제: key가 비어있으면 INVALID_REQUEST가 발생함")
	void delete_whenBlankKey_thenInvalidRequest() {
		// given
		String key = "";

		// when
		BusinessException ex = catchThrowableOfType(
			() -> storageService.deleteImage(key),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	private static S3Properties props() {
		return new S3Properties(
			"test-bucket",
			"ap-northeast-2",
			"test-prefix",
			60,
			5_000_000L,
			"dummy-1",
			"dummy-2"
		);
	}
}
