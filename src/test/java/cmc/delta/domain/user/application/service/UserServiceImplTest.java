package cmc.delta.domain.user.application.service;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.support.FakeUserRepositoryPort;
import cmc.delta.domain.user.application.support.UserFixtures;
import cmc.delta.domain.user.application.validator.UserValidator;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceImplTest {

	private FakeUserRepositoryPort userRepositoryPort;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userRepositoryPort = FakeUserRepositoryPort.create();
		userService = new UserServiceImpl(userRepositoryPort, new UserValidator());
	}

	@Test
	@DisplayName("내 프로필 조회: 유저가 있으면 UserMeData를 반환함")
	void getMyProfile_whenUserExists_thenReturnsUserMeData() {
		// given
		User user = userRepositoryPort.save(UserFixtures.activeUser());

		// when
		UserMeData result = userService.getMyProfile(user.getId());

		// then
		assertThat(result.userId()).isEqualTo(user.getId());
		assertThat(result.email()).isEqualTo(user.getEmail());
		assertThat(result.nickname()).isEqualTo(user.getNickname());
	}

	@Test
	@DisplayName("내 프로필 조회: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void getMyProfile_whenUserMissing_thenThrowsUserNotFound() {
		// given
		long userId = 999L;

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.getMyProfile(userId),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("회원 탈퇴: 유저가 있으면 delete가 호출되고 유저가 삭제됨")
	void withdrawAccount_whenUserExists_thenDeletesUser() {
		// given
		User user = userRepositoryPort.save(UserFixtures.activeUser());

		// when
		userService.withdrawAccount(user.getId());

		// then
		assertThat(userRepositoryPort.deleteCallCount()).isEqualTo(1);
		assertThat(userRepositoryPort.deletedIds()).containsExactly(user.getId());
		assertThat(userRepositoryPort.findById(user.getId())).isEmpty();
	}

	@Test
	@DisplayName("회원 탈퇴: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void withdrawAccount_whenUserMissing_thenThrowsUserNotFound() {
		// given
		long userId = 999L;

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.withdrawAccount(userId),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("회원 탈퇴: 이미 삭제된 유저를 다시 탈퇴하면 USER_NOT_FOUND가 발생함(하드 삭제 정책)")
	void withdrawAccount_whenAlreadyDeleted_thenThrowsUserNotFound() {
		// given
		User user = userRepositoryPort.save(UserFixtures.activeUser());
		userService.withdrawAccount(user.getId());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.withdrawAccount(user.getId()),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}
}
