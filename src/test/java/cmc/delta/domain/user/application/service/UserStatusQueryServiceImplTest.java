package cmc.delta.domain.user.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.user.application.port.out.UserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserStatusQueryServiceImplTest {

	@Test
	@DisplayName("유저 상태 조회: 유저가 있으면 status를 반환")
	void getStatus_whenUserExists_thenReturnsStatus() {
		// given
		UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
		UserStatusQueryServiceImpl sut = new UserStatusQueryServiceImpl(userRepositoryPort);

		User user = User.createProvisioned("user@example.com", "delta");
		when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(user));

		// when
		UserStatus status = sut.getStatus(10L);

		// then
		assertThat(status).isEqualTo(UserStatus.ONBOARDING_REQUIRED);
	}

	@Test
	@DisplayName("유저 상태 조회: 유저가 없으면 USER_NOT_FOUND가 발생")
	void getStatus_whenUserMissing_thenThrowsUserNotFound() {
		// given
		UserRepositoryPort userRepositoryPort = mock(UserRepositoryPort.class);
		UserStatusQueryServiceImpl sut = new UserStatusQueryServiceImpl(userRepositoryPort);

		when(userRepositoryPort.findById(999L)).thenReturn(Optional.empty());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> sut.getStatus(999L),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}
}
