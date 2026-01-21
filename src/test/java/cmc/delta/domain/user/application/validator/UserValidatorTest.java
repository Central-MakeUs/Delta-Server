package cmc.delta.domain.user.application.validator;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
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
			BusinessException.class
		);

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
			BusinessException.class
		);

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
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
