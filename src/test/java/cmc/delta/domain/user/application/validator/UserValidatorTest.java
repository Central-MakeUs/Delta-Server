package cmc.delta.domain.user.application.validator;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserValidatorTest {

	private final UserValidator validator = new UserValidator();

	@Test
	@DisplayName("프로비저닝 검증: command가 null이면 INVALID_REQUEST")
	void validateProvision_whenCommandNull_thenThrowsInvalidRequest() {
		// when
		BusinessException ex = catchThrowableOfType(
			() -> validator.validateProvision(null),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("프로비저닝 검증: provider가 null이면 INVALID_REQUEST")
	void validateProvision_whenProviderNull_thenThrowsInvalidRequest() {
		// given
		SocialUserProvisionCommand cmd = new SocialUserProvisionCommand(null, "pid", null, null);

		// when
		BusinessException ex = catchThrowableOfType(
			() -> validator.validateProvision(cmd),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("프로비저닝 검증: providerUserId가 blank면 INVALID_REQUEST")
	void validateProvision_whenProviderUserIdBlank_thenThrowsInvalidRequest() {
		// given
		SocialUserProvisionCommand cmd = new SocialUserProvisionCommand(/*provider*/ null, "  ", null, null);

		// when
		BusinessException ex = catchThrowableOfType(
			() -> validator.validateProvision(cmd),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("온보딩 검증: request가 null이면 INVALID_REQUEST")
	void validateOnboarding_whenRequestNull_thenThrowsInvalidRequest() {
		// when
		BusinessException ex = catchThrowableOfType(
			() -> validator.validate((UserOnboardingRequest)null),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("온보딩 검증: name이 blank면 INVALID_REQUEST")
	void validateOnboarding_whenNameBlank_thenThrowsInvalidRequest() {
		// given
		UserOnboardingRequest req = new UserOnboardingRequest("  ", LocalDate.of(2000, 1, 1), true);

		// when
		BusinessException ex = catchThrowableOfType(() -> validator.validate(req), BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("온보딩 검증: birthDate가 null이면 INVALID_REQUEST")
	void validateOnboarding_whenBirthDateNull_thenThrowsInvalidRequest() {
		// given
		UserOnboardingRequest req = new UserOnboardingRequest("홍길동", null, true);

		// when
		BusinessException ex = catchThrowableOfType(() -> validator.validate(req), BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("온보딩 검증: 미래 birthDate면 INVALID_REQUEST")
	void validateOnboarding_whenBirthDateFuture_thenThrowsInvalidRequest() {
		// given
		UserOnboardingRequest req = new UserOnboardingRequest("홍길동", LocalDate.now().plusDays(1), true);

		// when
		BusinessException ex = catchThrowableOfType(() -> validator.validate(req), BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("온보딩 검증: 약관 미동의면 INVALID_REQUEST")
	void validateOnboarding_whenTermsNotAgreed_thenThrowsInvalidRequest() {
		// given
		UserOnboardingRequest req = new UserOnboardingRequest("홍길동", LocalDate.of(2000, 1, 1), false);

		// when
		BusinessException ex = catchThrowableOfType(() -> validator.validate(req), BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이름 수정 검증: request가 null이면 INVALID_REQUEST")
	void validateNameUpdate_whenRequestNull_thenThrowsInvalidRequest() {
		BusinessException ex = catchThrowableOfType(
			() -> validator.validate((UserNameUpdateRequest)null),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이름 수정 검증: name이 blank면 INVALID_REQUEST")
	void validateNameUpdate_whenNameBlank_thenThrowsInvalidRequest() {
		UserNameUpdateRequest req = new UserNameUpdateRequest("  ");

		BusinessException ex = catchThrowableOfType(() -> validator.validate(req), BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
