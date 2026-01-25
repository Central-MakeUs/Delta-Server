package cmc.delta.domain.problem.adapter.in.web.scan;

import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanCreateResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanDetailResponse;
import cmc.delta.domain.problem.application.port.in.scan.result.ProblemScanSummaryResponse;
import cmc.delta.domain.problem.application.port.in.scan.ProblemScanQueryUseCase;
import cmc.delta.domain.problem.application.port.in.scan.ScanCommandUseCase;
import cmc.delta.domain.problem.application.port.in.scan.command.CreateScanCommand;
import cmc.delta.domain.problem.application.port.in.scan.result.ScanCreateResult;
import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.exception.ProblemValidationException;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "문제 스캔")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/problem-scans")
public class ProblemScanController {

	private final ScanCommandUseCase scanCommandUseCase;
	private final ProblemScanQueryUseCase problemScanQueryUseCase;

	@Operation(summary = "문제 스캔 생성 (업로드 + scan/asset 생성)")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.INVALID_REQUEST,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<ProblemScanCreateResponse> create(
		@CurrentUser UserPrincipal principal,
		@RequestPart("file") MultipartFile file
	) {
		UploadFile uploadFile = toUploadFile(file);
		ScanCreateResult result =
			scanCommandUseCase.createScan(principal.userId(), new CreateScanCommand(uploadFile));
		return ApiResponses.success(SuccessCode.OK, ProblemScanCreateResponse.from(result));
	}

	private UploadFile toUploadFile(MultipartFile file) {
		try {
			return new UploadFile(file.getBytes(), file.getContentType(), file.getOriginalFilename());
		} catch (IOException e) {
			throw new ProblemValidationException("업로드 파일을 읽을 수 없습니다.");
		}
	}

	@Operation(summary = "문제 스캔 상세 조회 (원본 이미지 URL + OCR/LaTeX 상태)")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.PROBLEM_SCAN_NOT_FOUND,
		ErrorCode.PROBLEM_ASSET_NOT_FOUND,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/{scanId}")
	public ApiResponse<ProblemScanDetailResponse> getScanDetail(
		@CurrentUser UserPrincipal principal,
		@PathVariable Long scanId
	) {
		ProblemScanDetailResponse data = problemScanQueryUseCase.getDetail(principal.userId(), scanId);
		return ApiResponses.success(SuccessCode.OK, data);
	}

	@Operation(summary = "문제 스캔 요약 조회 (앱용: 이미지 + 과목/단원/유형)")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.PROBLEM_SCAN_NOT_FOUND,
		ErrorCode.PROBLEM_ASSET_NOT_FOUND,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/{scanId}/summary")
	public ApiResponse<ProblemScanSummaryResponse> getScanSummary(
		@CurrentUser UserPrincipal principal,
		@PathVariable Long scanId
	) {
		ProblemScanSummaryResponse data = problemScanQueryUseCase.getSummary(principal.userId(), scanId);
		return ApiResponses.success(SuccessCode.OK, data);
	}
}
