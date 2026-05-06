package cmc.delta.domain.dashboard.adapter.in.web;

import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.application.port.in.GetDashboardUsersUseCase;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.swagger.DashboardApiDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "대시보드")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {

	private final GetDashboardUsersUseCase getDashboardUsersUseCase;

	@Operation(summary = "사용자 관리 목록 조회", description = DashboardApiDocs.GET_USERS)
	@GetMapping("/users")
	public ApiResponse<DashboardUsersResponse> getUsers(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return ApiResponses.success(SuccessCode.OK, getDashboardUsersUseCase.getUsers(page, size));
	}
}
