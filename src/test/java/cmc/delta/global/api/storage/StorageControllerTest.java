package cmc.delta.global.api.storage;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.storage.application.StorageService;
import cmc.delta.global.storage.support.StorageFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class StorageControllerTest {

	private StorageService storageService;
	private StorageController controller;

	@BeforeEach
	void setUp() {
		storageService = new FakeStorageService();
		controller = new StorageController(storageService);
	}

	@Test
	@DisplayName("업로드 API: 서비스 결과를 StorageUploadData로 반환함")
	void upload_ok_mapsData() {
		// given
		MultipartFile file = StorageFixtures.png("a.png", 10, 20);

		// when
		ApiResponse<StorageUploadData> response = controller.uploadImage(file, "profiles");

		// then
		StorageUploadData data = response.data();
		assertThat(data.storageKey()).isEqualTo("profiles/abc.png");
		assertThat(data.viewUrl()).isEqualTo("https://view/abc");
		assertThat(data.width()).isEqualTo(10);
		assertThat(data.height()).isEqualTo(20);
	}

	@Test
	@DisplayName("Presigned GET API: 서비스 결과를 StoragePresignedGetData로 반환함")
	void presign_ok_mapsData() {
		// given
		String key = "profiles/abc.png";

		// when
		ApiResponse<StoragePresignedGetData> response = controller.presignGet(key, 30);

		// then
		StoragePresignedGetData data = response.data();
		assertThat(data.url()).isEqualTo("https://presigned/get");
		assertThat(data.expiresInSeconds()).isEqualTo(30);
	}

	@Test
	@DisplayName("삭제 API: 성공 응답을 반환함")
	void delete_ok_returnsOk() {
		// given
		String key = "profiles/abc.png";

		// when
		ApiResponse<Void> response = controller.deleteImage(key);

		// then
		assertThat(response).isNotNull();
	}

	private static class FakeStorageService extends StorageService {

		FakeStorageService() {
			super(null, null, null, null, null);
		}

		@Override
		public StorageUploadData uploadImage(MultipartFile file, String directory) {
			return new StorageUploadData(
				"profiles/abc.png",
				"https://view/abc",
				"image/png",
				123L,
				10,
				20);
		}

		@Override
		public StoragePresignedGetData issueReadUrl(String storageKey, Integer ttlSeconds) {
			int ttl = (ttlSeconds == null ? 60 : ttlSeconds);
			return new StoragePresignedGetData("https://presigned/get", ttl);
		}

		@Override
		public void deleteImage(String storageKey) {}
	}
}
