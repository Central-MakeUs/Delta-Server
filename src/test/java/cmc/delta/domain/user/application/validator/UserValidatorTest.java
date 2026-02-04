package cmc.delta.domain.user.application.validator;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserNameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserValidatorTest {

	private static final long ONE_DAY = 1L;
	private static final int YEAR_2000 = 2000;
	private static final int MONTH_JAN = 1;
	private static final int DAY_FIRST = 1;
	private static final String FIXED_TIME = "2024-01-01T00:00:00Z";
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse(FIXED_TIME), ZoneOffset.UTC);
	private static final LocalDate BIRTH_DATE = LocalDate.of(YEAR_2000, MONTH_JAN, DAY_FIRST);
	private static final String PROVIDER_USER_ID = "pid";
	private static final String BLANK = "  ";
	private static final String NICKNAME = "홍길동";
	private static final String NAME = "홍길동";

	private final UserValidator validator = new UserValidator(FIXED_CLOCK);

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
		UserOnboardingRequest req = new UserOnboardingRequest(BLANK, BIRTH_DATE, true);

		assertInvalidRequest(() -> validator.validate(req));
	}

	@Test
	@DisplayName("온보딩 검증: birthDate가 null이면 INVALID_REQUEST")
	void validateOnboarding_whenBirthDateNull_thenThrowsInvalidRequest() {
		UserOnboardingRequest req = new UserOnboardingRequest(NICKNAME, null, true);

		assertInvalidRequest(() -> validator.validate(req));
	}

	@Test
	@DisplayName("온보딩 검증: 미래 birthDate면 INVALID_REQUEST")
	void validateOnboarding_whenBirthDateFuture_thenThrowsInvalidRequest() {
		LocalDate tomorrow = LocalDate.now(FIXED_CLOCK).plusDays(ONE_DAY);
		UserOnboardingRequest req = new UserOnboardingRequest(NICKNAME, tomorrow, true);

		assertInvalidRequest(() -> validator.validate(req));
	}

	@Test
	@DisplayName("온보딩 검증: 약관 미동의면 INVALID_REQUEST")
	void validateOnboarding_whenTermsNotAgreed_thenThrowsInvalidRequest() {
		UserOnboardingRequest req = new UserOnboardingRequest(NICKNAME, BIRTH_DATE, false);

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
