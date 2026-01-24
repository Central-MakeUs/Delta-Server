package cmc.delta.domain.auth.application.validation;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialUserProvisionValidatorTest {

	private final SocialUserProvisionValidator validator = new SocialUserProvisionValidator();

	@Test
	@DisplayName("프로비저닝 검증: command가 null이면 INVALID_REQUEST")
	void validate_whenCommandNull_thenInvalidRequest() {
		UserException ex = catchThrowableOfType(() -> validator.validate(null), UserException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("프로비저닝 검증: provider가 null이면 INVALID_REQUEST")
	void validate_whenProviderNull_thenInvalidRequest() {
		SocialUserProvisionCommand cmd = new SocialUserProvisionCommand(null, "pid", null, null);
		UserException ex = catchThrowableOfType(() -> validator.validate(cmd), UserException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("프로비저닝 검증: providerUserId가 blank면 INVALID_REQUEST")
	void validate_whenProviderUserIdBlank_thenInvalidRequest() {
		SocialUserProvisionCommand cmd = new SocialUserProvisionCommand(SocialProvider.KAKAO, "  ", null, null);
		UserException ex = catchThrowableOfType(() -> validator.validate(cmd), UserException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}
}
