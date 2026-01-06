package cmc.delta.global.api.storage;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.api.storage.dto.StoragePresignedGetData;
import cmc.delta.global.api.storage.dto.StorageUploadData;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "스토리지")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storage")
public class StorageController {

	private final StorageService storageService;

	@Operation(summary = "이미지 업로드 (S3)")
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<StorageUploadData> uploadImage(
		@RequestPart("file") MultipartFile file,
		@RequestParam(value = "directory", required = false) String directory
	) {
		StorageService.UploadImageResult result = storageService.uploadImage(file, directory);

		StorageUploadData data = new StorageUploadData(
			result.storageKey(),
			result.viewUrl(),
			result.contentType(),
			result.sizeBytes(),
			result.width(),
			result.height()
		);

		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "이미지 조회용 Presigned GET URL 발급")
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/images/presigned-get")
	public ApiResponse<StoragePresignedGetData> presignGet(
		@RequestParam("key") String storageKey,
		@RequestParam(value = "ttlSeconds", required = false) Integer ttlSeconds
	) {
		StorageService.PresignedGetUrlResult result = storageService.issueReadUrl(storageKey, ttlSeconds);
		StoragePresignedGetData data = new StoragePresignedGetData(result.url(), result.expiresInSeconds());
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "이미지 삭제 (S3)")
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.INTERNAL_ERROR
	})
	@DeleteMapping("/images")
	public ApiResponse<Void> deleteImage(@RequestParam("key") String storageKey) {
		storageService.deleteImage(storageKey);
		return ApiResponses.success(SuccessCode.OK);
	}
}
