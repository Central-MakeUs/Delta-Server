package cmc.delta.domain.report.adapter.in.web;

import cmc.delta.domain.report.application.dto.ReportDetailResult;
import cmc.delta.domain.report.application.dto.ReportRequestResult;
import cmc.delta.domain.report.application.port.in.ReportCommandUseCase;
import cmc.delta.domain.report.application.port.in.ReportQueryUseCase;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "취약점 분석 리포트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

	private final ReportCommandUseCase reportCommandUseCase;
	private final ReportQueryUseCase reportQueryUseCase;

	@Operation(summary = "취약점 분석 리포트 생성 요청", description = "오답 5개 이상일 때 비동기 리포트 생성을 접수한다. reusedExisting=true면 기존 리포트를 재사용한다.")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.REPORT_NOT_ENOUGH_PROBLEMS,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping
	public ApiResponse<ReportRequestResult> requestReport(@CurrentUser
	UserPrincipal principal) {
		ReportRequestResult result = reportCommandUseCase.requestMyReport(principal.userId());
		return ApiResponses.success(SuccessCode.ACCEPTED, result);
	}

	@Operation(summary = "취약점 분석 리포트 조회", description = "리포트 상태와 완료 시 분석 결과를 반환한다. (폴링용)")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.REPORT_NOT_FOUND,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/{reportId}")
	public ApiResponse<ReportDetailResult> getReport(
		@CurrentUser
		UserPrincipal principal,
		@PathVariable
		Long reportId) {
		ReportDetailResult result = reportQueryUseCase.getMyReport(principal.userId(), reportId);
		return ApiResponses.success(SuccessCode.OK, result);
	}

	@Operation(summary = "최신 취약점 분석 리포트 조회", description = "가장 최근 생성된 리포트를 반환한다.")
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.REPORT_NOT_FOUND,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/latest")
	public ApiResponse<ReportDetailResult> getLatestReport(@CurrentUser
	UserPrincipal principal) {
		ReportDetailResult result = reportQueryUseCase.getMyLatestReport(principal.userId());
		return ApiResponses.success(SuccessCode.OK, result);
	}
}
