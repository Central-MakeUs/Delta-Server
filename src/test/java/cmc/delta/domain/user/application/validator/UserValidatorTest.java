package cmc.delta.domain.user.application.validator;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserValidatorTest {

	private static final String PROVIDER_USER_ID = "pid";
	private static final String BLANK = "  ";
	private static final String NICKNAME = "홍길동";
	private static final String NAME = "홍길동";

	private final UserValidator validator = new UserValidator();

	@Test
	@DisplayName("프로비저닝 검증: command가 null이면 INVALID_REQUEST")
	void validateProvision_whenCommandNull_thenThrowsInvalidRequest() {
		assertInvalidRequest(() -> validator.validateProvision(null));
	}

	@Test
	@DisplayName("프로비저닝 검증: provider가 null이면 INVALID_REQUEST")
	void validateProvision_whenProviderNull_thenThrowsInvalidRequest() {
		SocialUserProvisionCommand cmd = new SocialUserProvisionCommand(null, PROVIDER_USER_ID, null, null);

		assertInvalidRequest(() -> validator.validateProvision(cmd));
	}

	@Test
	@DisplayName("프로비저닝 검증: providerUserId가 blank면 INVALID_REQUEST")
	void validateProvision_whenProviderUserIdBlank_thenThrowsInvalidRequest() {
		SocialUserProvisionCommand cmd = new SocialUserProvisionCommand(null, BLANK, null, null);

		assertInvalidRequest(() -> validator.validateProvision(cmd));
	}

	@Test
	@DisplayName("온보딩 검증: request가 null이면 INVALID_REQUEST")
	void validateOnboarding_whenRequestNull_thenThrowsInvalidRequest() {
		assertInvalidRequest(() -> validator.validate((UserOnboardingRequest)null));
	}

	@Test
	@DisplayName("온보딩 검증: name이 blank면 INVALID_REQUEST")
	void validateOnboarding_whenNameBlank_thenThrowsInvalidRequest() {
		UserOnboardingRequest req = new UserOnboardingRequest(BLANK, true);

		assertInvalidRequest(() -> validator.validate(req));
	}

	@Test
	@DisplayName("온보딩 검증: 약관 미동의면 INVALID_REQUEST")
	void validateOnboarding_whenTermsNotAgreed_thenThrowsInvalidRequest() {
		UserOnboardingRequest req = new UserOnboardingRequest(NICKNAME, false);

		assertInvalidRequest(() -> validator.validate(req));
	}

	@Test
	@DisplayName("이름 수정 검증: request가 null이면 INVALID_REQUEST")
	void validateNameUpdate_whenRequestNull_thenThrowsInvalidRequest() {
		assertInvalidRequest(() -> validator.validate((UserNameUpdateRequest)null));
	}

	@Test
	@DisplayName("이름 수정 검증: name이 blank면 INVALID_REQUEST")
	void validateNameUpdate_whenNameBlank_thenThrowsInvalidRequest() {
		UserNameUpdateRequest req = new UserNameUpdateRequest(BLANK);

		assertInvalidRequest(() -> validator.validate(req));
	}

	@Test
	@DisplayName("닉네임 수정 검증: request가 null이면 INVALID_REQUEST")
	void validateNicknameUpdate_whenRequestNull_thenThrowsInvalidRequest() {
		assertInvalidRequest(() -> validator.validate((UserNicknameUpdateRequest)null));
	}

	@Test
	@DisplayName("닉네임 수정 검증: nickname이 blank면 INVALID_REQUEST")
	void validateNicknameUpdate_whenNicknameBlank_thenThrowsInvalidRequest() {
		UserNicknameUpdateRequest req = new UserNicknameUpdateRequest(BLANK);
		assertInvalidRequest(() -> validator.validate(req));
	}

	private void assertInvalidRequest(Runnable runnable) {
		BusinessException ex = catchThrowableOfType(runnable::run, BusinessException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
