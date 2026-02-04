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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceImplTest {

	private static final long USER_ID = 999L;
	private static final int DELETE_CALLS = 1;
	private static final String NICKNAME_KIM = "김철수";
	private static final String NICKNAME_HONG = "홍길동";
	private static final int YEAR_2000 = 2000;
	private static final int MONTH_JAN = 1;
	private static final int DAY_FIRST = 1;
	private static final String FIXED_TIME = "2024-01-01T00:00:00Z";
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse(FIXED_TIME), ZoneOffset.UTC);
	private static final LocalDate BIRTH_DATE = LocalDate.of(YEAR_2000, MONTH_JAN, DAY_FIRST);
	private static final String PROVIDER_USER_ID = "pid";

	private FakeUserRepositoryPort userRepositoryPort;
	private cmc.delta.domain.auth.application.support.FakeSocialAccountRepositoryPort socialAccountRepositoryPort;
	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userRepositoryPort = FakeUserRepositoryPort.create();
		socialAccountRepositoryPort = cmc.delta.domain.auth.application.support.FakeSocialAccountRepositoryPort.create();
		userService = new UserServiceImpl(userRepositoryPort, socialAccountRepositoryPort, new UserValidator(FIXED_CLOCK));
	}

	@Test
	@DisplayName("내 프로필 조회: 유저가 있으면 UserMeData를 반환함")
	void getMyProfile_whenUserExists_thenReturnsUserMeData() {
		User user = givenActiveUser();

		UserMeData result = userService.getMyProfile(user.getId());

		assertThat(result.userId()).isEqualTo(user.getId());
		assertThat(result.email()).isEqualTo(user.getEmail());
		assertThat(result.nickname()).isEqualTo(user.getNickname());
		assertThat(result.oauthProvider()).isNull();
	}

    @Test
    @DisplayName("내 프로필 조회: 소셜 계정이 연결되어 있으면 oauthProvider를 반환함")
	void getMyProfile_whenSocialAccountExists_thenReturnsProvider() {
		User user = givenActiveUser();
		cmc.delta.domain.auth.model.SocialAccount account = cmc.delta.domain.auth.model.SocialAccount.link(
			cmc.delta.domain.auth.model.SocialProvider.KAKAO,
			PROVIDER_USER_ID,
			user);
		socialAccountRepositoryPort.put(account);

		UserMeData result = userService.getMyProfile(user.getId());

		assertThat(result.userId()).isEqualTo(user.getId());
		assertThat(result.oauthProvider()).isEqualTo(cmc.delta.domain.auth.model.SocialProvider.KAKAO);
	}

	@Test
	@DisplayName("내 프로필 조회: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void getMyProfile_whenUserMissing_thenThrowsUserNotFound() {
		BusinessException ex = catchThrowableOfType(
			() -> userService.getMyProfile(USER_ID),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("회원 탈퇴: 유저가 있으면 delete가 호출되고 유저가 삭제됨")
	void withdrawAccount_whenUserExists_thenDeletesUser() {
		User user = givenActiveUser();

		userService.withdrawAccount(user.getId());

		assertThat(userRepositoryPort.deleteCallCount()).isEqualTo(DELETE_CALLS);
		assertThat(userRepositoryPort.deletedIds()).containsExactly(user.getId());
		assertThat(userRepositoryPort.findById(user.getId())).isEmpty();
	}

	@Test
	@DisplayName("회원 탈퇴: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void withdrawAccount_whenUserMissing_thenThrowsUserNotFound() {
		BusinessException ex = catchThrowableOfType(
			() -> userService.withdrawAccount(USER_ID),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("회원 탈퇴: 이미 삭제된 유저를 다시 탈퇴하면 USER_NOT_FOUND가 발생함(하드 삭제 정책)")
	void withdrawAccount_whenAlreadyDeleted_thenThrowsUserNotFound() {
		User user = givenActiveUser();
		userService.withdrawAccount(user.getId());

		BusinessException ex = catchThrowableOfType(
			() -> userService.withdrawAccount(user.getId()),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("온보딩 완료: 요청이 유효하고 유저가 ONBOARDING_REQUIRED면 ACTIVE로 전환되고 프로필이 저장됨")
	void completeOnboarding_whenValidRequest_thenCompletesOnboarding() {
		User user = givenActiveUser();
		UserOnboardingRequest request = new UserOnboardingRequest(NICKNAME_HONG, BIRTH_DATE, true);

		userService.completeOnboarding(user.getId(), request);

		User updated = userRepositoryPort.getReferenceById(user.getId());
		assertThat(updated.getNickname()).isEqualTo(NICKNAME_HONG);
		assertThat(updated.getBirthDate()).isEqualTo(BIRTH_DATE);
		assertThat(updated.getTermsAgreedAt()).isNotNull();
		assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("온보딩 완료: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void completeOnboarding_whenUserMissing_thenThrowsUserNotFound() {
		UserOnboardingRequest request = new UserOnboardingRequest(NICKNAME_HONG, BIRTH_DATE, true);
		BusinessException ex = catchThrowableOfType(
			() -> userService.completeOnboarding(USER_ID, request),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("온보딩 완료: 탈퇴한 유저면 USER_WITHDRAWN이 발생함")
	void completeOnboarding_whenUserWithdrawn_thenThrowsUserWithdrawn() {
		User user = givenWithdrawnUser();
		UserOnboardingRequest request = new UserOnboardingRequest(NICKNAME_HONG, BIRTH_DATE, true);
		BusinessException ex = catchThrowableOfType(
			() -> userService.completeOnboarding(user.getId(), request),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN);
	}

	@Test
	@DisplayName("온보딩 완료: 요청이 null이면 INVALID_REQUEST가 발생함")
	void completeOnboarding_whenRequestNull_thenThrowsInvalidRequest() {
		User user = givenActiveUser();
		BusinessException ex = catchThrowableOfType(
			() -> userService.completeOnboarding(user.getId(), null),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("닉네임 수정: 요청이 유효하면 nickname이 변경됨")
	void updateMyNickname_whenValidRequest_thenUpdatesNickname() {
		User user = givenActiveUser();
		userService.updateMyNickname(user.getId(), new UserNicknameUpdateRequest(NICKNAME_KIM));

		User updated = userRepositoryPort.getReferenceById(user.getId());
		assertThat(updated.getNickname()).isEqualTo(NICKNAME_KIM);
	}

	@Test
	@DisplayName("닉네임 수정: 유저가 없으면 USER_NOT_FOUND가 발생함")
	void updateMyNickname_whenUserMissing_thenThrowsUserNotFound() {
		BusinessException ex = catchThrowableOfType(
			() -> userService.updateMyNickname(USER_ID, new UserNicknameUpdateRequest(NICKNAME_KIM)),
			BusinessException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	private User givenActiveUser() {
		return userRepositoryPort.save(UserFixtures.activeUser());
	}

	private User givenWithdrawnUser() {
		return userRepositoryPort.save(UserFixtures.withdrawnUser());
	}
}
