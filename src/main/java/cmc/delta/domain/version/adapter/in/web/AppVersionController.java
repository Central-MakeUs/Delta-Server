package cmc.delta.domain.version.adapter.in.web;

import cmc.delta.domain.version.application.port.in.AppVersionQueryUseCase;
import cmc.delta.domain.version.application.port.in.result.AppVersionResponse;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.swagger.AppVersionApiDocs;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "앱 버전")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app-version")
public class AppVersionController {

	private final AppVersionQueryUseCase appVersionQueryUseCase;

	@Operation(summary = "앱 버전 정책 조회", description = AppVersionApiDocs.GET_APP_VERSION)
	@ApiErrorCodeExamples({
		ErrorCode.INVALID_REQUEST,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping
	public ApiResponse<AppVersionResponse> getAppVersion(
		@Parameter(
			description = "현재 설치된 앱 버전입니다. major.minor.patch 형식으로 전달합니다.",
			example = "1.2.3",
			required = true)
		@RequestParam(value = "currentVersion", required = false)
		String currentVersion) {
		AppVersionResponse data = appVersionQueryUseCase.getAppVersion(currentVersion);
		return ApiResponses.success(SuccessCode.OK, data);
	}
}
