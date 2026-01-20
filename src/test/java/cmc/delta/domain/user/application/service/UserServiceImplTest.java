package cmc.delta.domain.user.application.service;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.user.adapter.in.web.dto.response.UserMeData;
import cmc.delta.domain.user.application.support.FakeUserJpaRepository;
import cmc.delta.domain.user.application.support.UserFixtures;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.domain.user.adapter.out.persistence.UserJpaRepository;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceImplTest {

	private UserJpaRepository userJpaRepository;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userJpaRepository = FakeUserJpaRepository.create();
		userService = new UserServiceImpl(userJpaRepository);
	}

	@Test
	@DisplayName("내 프로필 조회: 유저가 있으면 UserMeData를 반환함")
	void getMyProfile_whenUserExists_thenReturnsUserMeData() {
		// given
		User user = userJpaRepository.save(UserFixtures.activeUser());

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
	@DisplayName("회원 탈퇴: ACTIVE 유저면 status가 WITHDRAWN으로 변경됨")
	void withdrawAccount_whenActiveUser_thenStatusBecomesWithdrawn() {
		// given
		User user = userJpaRepository.save(UserFixtures.activeUser());

		// when
		userService.withdrawAccount(user.getId());

		// then
		User reloaded = userJpaRepository.findById(user.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
	}

	@Test
	@DisplayName("회원 탈퇴: 이미 탈퇴한 유저면 USER_WITHDRAWN이 발생함")
	void withdrawAccount_whenAlreadyWithdrawn_thenThrowsUserWithdrawn() {
		// given
		User user = userJpaRepository.save(UserFixtures.withdrawnUser());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.withdrawAccount(user.getId()),
			BusinessException.class
		);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN);
	}
}
