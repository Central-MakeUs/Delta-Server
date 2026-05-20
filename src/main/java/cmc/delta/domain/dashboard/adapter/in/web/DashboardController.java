package cmc.delta.domain.dashboard.adapter.in.web;

import cmc.delta.domain.dashboard.adapter.in.web.dto.request.DashboardMonthlyAccessRequest;
import cmc.delta.domain.dashboard.adapter.in.web.dto.request.DashboardProblemsRequest;
import cmc.delta.domain.dashboard.adapter.in.web.dto.request.DashboardUsersRequest;
import cmc.delta.domain.dashboard.application.dto.DashboardMonthlyAccessResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemsResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.application.port.in.DashboardQueryUseCase;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.swagger.DashboardApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대시보드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {

	private final DashboardQueryUseCase dashboardQueryUseCase;

	@Operation(summary = "사용자 관리 목록 조회", description = DashboardApiDocs.GET_USERS)
	@GetMapping("/users")
	public ApiResponse<DashboardUsersResponse> getUsers(@ModelAttribute DashboardUsersRequest request) {
		return ApiResponses.success(SuccessCode.OK,
			dashboardQueryUseCase.getUsers(PageRequest.of(request.page(), request.size())));
	}

	@Operation(summary = "달별 일별 접속자 수 조회", description = DashboardApiDocs.GET_MONTHLY_ACCESS)
	@GetMapping("/access/monthly")
	public ApiResponse<DashboardMonthlyAccessResponse> getMonthlyAccess(
		@ModelAttribute DashboardMonthlyAccessRequest request) {
		return ApiResponses.success(SuccessCode.OK,
			dashboardQueryUseCase.getMonthlyAccess(request.toYearMonth()));
	}

	@Operation(summary = "문제 등록 현황 조회", description = DashboardApiDocs.GET_PROBLEMS)
	@GetMapping("/problems")
	public ApiResponse<DashboardProblemsResponse> getProblems(@ModelAttribute DashboardProblemsRequest request) {
		return ApiResponses.success(SuccessCode.OK,
			dashboardQueryUseCase.getProblems(PageRequest.of(request.page(), request.size())));
	}
}
