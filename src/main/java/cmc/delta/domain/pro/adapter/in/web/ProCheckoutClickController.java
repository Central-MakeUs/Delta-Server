package cmc.delta.domain.pro.adapter.in.web;

import cmc.delta.domain.pro.application.port.in.ProCheckoutClickUseCase;
import cmc.delta.domain.pro.application.port.in.result.ProCheckoutClickStatsResponse;
import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.api.response.SuccessCode;
import cmc.delta.global.config.security.principal.CurrentUser;
import cmc.delta.global.config.security.principal.UserPrincipal;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.config.swagger.ProApiDocs;
import cmc.delta.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pro")
@RestController
@RequestMapping("/api/v1/pro")
public class ProCheckoutClickController {

	private final ProCheckoutClickUseCase useCase;

	public ProCheckoutClickController(ProCheckoutClickUseCase useCase) {
		this.useCase = useCase;
	}

	@Operation(summary = "Pro 결제 버튼 클릭 기록", description = ProApiDocs.TRACK_CHECKOUT_CLICK)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@PostMapping("/checkout-click")
	public ApiResponse<Void> trackCheckoutClick(
		@CurrentUser
		UserPrincipal principal) {
		useCase.trackCheckoutClick(principal.userId());
		return ApiResponses.success(SuccessCode.OK);
	}

	@Operation(summary = "Pro 결제 버튼 클릭 통계 조회", description = ProApiDocs.CHECKOUT_CLICK_STATS)
	@ApiErrorCodeExamples({
		ErrorCode.AUTHENTICATION_FAILED,
		ErrorCode.TOKEN_REQUIRED,
		ErrorCode.USER_NOT_FOUND,
		ErrorCode.USER_WITHDRAWN,
		ErrorCode.INTERNAL_ERROR
	})
	@GetMapping("/checkout-click/stats")
	public ApiResponse<ProCheckoutClickStatsResponse> getCheckoutClickStats() {
		ProCheckoutClickStatsResponse data = useCase.getCheckoutClickStats();
		return ApiResponses.success(SuccessCode.OK, data);
	}
}
