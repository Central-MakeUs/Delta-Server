package cmc.delta.domain.user.adapter.in;

import cmc.delta.domain.user.application.port.in.UserProfileImageUseCase;
import cmc.delta.domain.user.application.port.in.dto.ProfileImageUploadCommand;
import cmc.delta.domain.user.application.port.in.dto.UserProfileImageResult;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.exception.StorageException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "유저")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserProfileImageController {

	private final UserProfileImageUseCase useCase;

	@Operation(summary = "내 프로필 이미지 업로드/교체")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<UserProfileImageResult> upload(
		@CurrentUser UserPrincipal principal,
		@RequestPart("file") MultipartFile file
	) {
		ProfileImageUploadCommand command = new ProfileImageUploadCommand(
			readBytes(file),
			file.getContentType(),
			file.getOriginalFilename()
		);

		UserProfileImageResult result = useCase.uploadMyProfileImage(principal.userId(), command);
		return ApiResponses.success(SuccessCode.OK, result);
	}

	@Operation(summary = "내 프로필 이미지 조회")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN
	})
	@GetMapping("/profile-image")
	public ApiResponse<UserProfileImageResult> get(@CurrentUser UserPrincipal principal) {
		UserProfileImageResult result = useCase.getMyProfileImage(principal.userId());
		return ApiResponses.success(SuccessCode.OK, result);
	}

	@Operation(summary = "내 프로필 이미지 삭제")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN
	})
	@DeleteMapping("/profile-image")
	public ApiResponse<Void> delete(@CurrentUser UserPrincipal principal) {
		useCase.deleteMyProfileImage(principal.userId());
		return ApiResponses.success(SuccessCode.OK);
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw StorageException.internalError("파일을 읽는 중 오류가 발생했습니다.", e);
		}
	}
}
