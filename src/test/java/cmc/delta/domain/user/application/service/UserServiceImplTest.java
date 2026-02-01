package cmc.delta.domain.user.application.service;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.user.adapter.in.dto.request.UserNicknameUpdateRequest;
import cmc.delta.domain.user.adapter.in.dto.request.UserOnboardingRequest;
import cmc.delta.domain.user.adapter.in.dto.response.UserMeData;
import cmc.delta.domain.user.application.support.FakeUserRepositoryPort;
import cmc.delta.domain.user.application.support.UserFixtures;
import cmc.delta.domain.user.application.validator.UserValidator;
import cmc.delta.domain.user.model.User;
import cmc.delta.domain.user.model.enums.UserStatus;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceImplTest {

    private FakeUserRepositoryPort userRepositoryPort;
    private cmc.delta.domain.auth.application.support.FakeSocialAccountRepositoryPort socialAccountRepositoryPort;
    private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
        userRepositoryPort = FakeUserRepositoryPort.create();
        socialAccountRepositoryPort = cmc.delta.domain.auth.application.support.FakeSocialAccountRepositoryPort.create();
        userService = new UserServiceImpl(userRepositoryPort, socialAccountRepositoryPort, new UserValidator());
	}

	@Test
	@DisplayName("내 프로필 조회: 유저가 있으면 UserMeData를 반환함")
    void getMyProfile_whenUserExists_thenReturnsUserMeData() {
        // given
        User user = userRepositoryPort.save(UserFixtures.activeUser());
        // no social account -> oauthProvider should be null

        // when
        UserMeData result = userService.getMyProfile(user.getId());

        // then
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.email()).isEqualTo(user.getEmail());
        assertThat(result.nickname()).isEqualTo(user.getNickname());
        assertThat(result.oauthProvider()).isNull();
    }

    @Test
    @DisplayName("내 프로필 조회: 소셜 계정이 연결되어 있으면 oauthProvider를 반환함")
    void getMyProfile_whenSocialAccountExists_thenReturnsProvider() {
        // given
        User user = userRepositoryPort.save(UserFixtures.activeUser());
        cmc.delta.domain.auth.model.SocialAccount account = cmc.delta.domain.auth.model.SocialAccount.link(cmc.delta.domain.auth.model.SocialProvider.KAKAO, "pid", user);
        socialAccountRepositoryPort.put(account);

        // when
        UserMeData result = userService.getMyProfile(user.getId());

        // then
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.oauthProvider()).isEqualTo(cmc.delta.domain.auth.model.SocialProvider.KAKAO);
    }

	@Test
	@DisplayName("내 프로필 조회: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void getMyProfile_whenUserMissing_thenThrowsUserNotFound() {
		// given
		long userId = 999L;

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.getMyProfile(userId),
			BusinessException.class);

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
			BusinessException.class);

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
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("온보딩 완료: 요청이 유효하고 유저가 ONBOARDING_REQUIRED면 ACTIVE로 전환되고 프로필이 저장됨")
	void completeOnboarding_whenValidRequest_thenCompletesOnboarding() {
		// given
		User user = userRepositoryPort.save(UserFixtures.activeUser());
		UserOnboardingRequest request = new UserOnboardingRequest("홍길동", LocalDate.of(2000, 1, 1), true);

		// when
		userService.completeOnboarding(user.getId(), request);

		// then
		User updated = userRepositoryPort.getReferenceById(user.getId());
		assertThat(updated.getNickname()).isEqualTo("홍길동");
		assertThat(updated.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
		assertThat(updated.getTermsAgreedAt()).isNotNull();
		assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("온보딩 완료: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void completeOnboarding_whenUserMissing_thenThrowsUserNotFound() {
		// given
		long userId = 999L;
		UserOnboardingRequest request = new UserOnboardingRequest("홍길동", LocalDate.of(2000, 1, 1), true);

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.completeOnboarding(userId, request),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("온보딩 완료: 탈퇴한 유저면 USER_WITHDRAWN이 발생함")
	void completeOnboarding_whenUserWithdrawn_thenThrowsUserWithdrawn() {
		// given
		User user = userRepositoryPort.save(UserFixtures.withdrawnUser());
		UserOnboardingRequest request = new UserOnboardingRequest("홍길동", LocalDate.of(2000, 1, 1), true);

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.completeOnboarding(user.getId(), request),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN);
	}

	@Test
	@DisplayName("온보딩 완료: 요청이 null이면 INVALID_REQUEST가 발생함")
	void completeOnboarding_whenRequestNull_thenThrowsInvalidRequest() {
		// given
		User user = userRepositoryPort.save(UserFixtures.activeUser());

		// when
		BusinessException ex = catchThrowableOfType(
			() -> userService.completeOnboarding(user.getId(), null),
			BusinessException.class);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("닉네임 수정: 요청이 유효하면 nickname이 변경됨")
	void updateMyNickname_whenValidRequest_thenUpdatesNickname() {
		User user = userRepositoryPort.save(UserFixtures.activeUser());
		userService.updateMyNickname(user.getId(), new UserNicknameUpdateRequest("김철수"));

		User updated = userRepositoryPort.getReferenceById(user.getId());
		assertThat(updated.getNickname()).isEqualTo("김철수");
	}

	@Test
	@DisplayName("닉네임 수정: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void updateMyNickname_whenUserMissing_thenThrowsUserNotFound() {
		BusinessException ex = catchThrowableOfType(
			() -> userService.updateMyNickname(999L, new UserNicknameUpdateRequest("김철수")),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}
}
