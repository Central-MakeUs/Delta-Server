package cmc.delta.domain.auth.application.service.provisioning;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.auth.application.port.in.provisioning.SocialUserProvisionCommand;
import cmc.delta.domain.auth.application.port.in.provisioning.UserProvisioningUseCase;
import cmc.delta.domain.auth.application.support.FakeSocialAccountRepositoryPort;
import cmc.delta.domain.auth.application.validation.SocialUserProvisionValidator;
import cmc.delta.domain.auth.model.SocialAccount;
import cmc.delta.domain.auth.model.SocialProvider;
import cmc.delta.domain.user.application.exception.UserException;
import cmc.delta.domain.user.application.support.FakeUserRepositoryPort;
import cmc.delta.domain.user.model.User;
import cmc.delta.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProvisioningServiceImplTest {

	private FakeUserRepositoryPort userRepositoryPort;
	private FakeSocialAccountRepositoryPort socialAccountRepositoryPort;
	private UserProvisioningServiceImpl sut;

	@BeforeEach
	void setUp() {
		userRepositoryPort = FakeUserRepositoryPort.create();
		socialAccountRepositoryPort = FakeSocialAccountRepositoryPort.create();
		sut = new UserProvisioningServiceImpl(userRepositoryPort, socialAccountRepositoryPort,
			new SocialUserProvisionValidator());
	}

	@Test
	@DisplayName("프로비저닝: 기존 소셜 계정이 있으면 기존 유저를 반환하고 isNewUser=false")
	void provisionSocialUser_whenAccountExists_thenReturnsExistingUser() {
		User user = userRepositoryPort.save(User.createProvisioned("e@e.com", "nick"));
		SocialAccount account = SocialAccount.link(SocialProvider.KAKAO, "pid", user);
		socialAccountRepositoryPort.put(account);

		UserProvisioningUseCase.ProvisioningResult out = sut.provisionSocialUser(
			new SocialUserProvisionCommand(SocialProvider.KAKAO, "pid", null, null));

		assertThat(out.userId()).isEqualTo(user.getId());
		assertThat(out.email()).isEqualTo(user.getEmail());
		assertThat(out.nickname()).isEqualTo(user.getNickname());
		assertThat(out.isNewUser()).isFalse();
	}

	@Test
	@DisplayName("프로비저닝: 계정이 없으면 유저를 생성하고 계정을 링크하고 isNewUser=true")
	void provisionSocialUser_whenMissing_thenCreatesUser() {
		UserProvisioningUseCase.ProvisioningResult out = sut.provisionSocialUser(
			new SocialUserProvisionCommand(SocialProvider.KAKAO, "pid", "e@e.com", "nick"));

		assertThat(out.userId()).isPositive();
		assertThat(out.email()).isEqualTo("e@e.com");
		assertThat(out.nickname()).isEqualTo("nick");
		assertThat(out.isNewUser()).isTrue();
		assertThat(socialAccountRepositoryPort.findByProviderAndProviderUserId(SocialProvider.KAKAO, "pid"))
			.isPresent();
	}

	@Test
	@DisplayName("프로비저닝: 신규 생성 시 email이 없으면 INVALID_REQUEST")
	void provisionSocialUser_whenCreateMissingEmail_thenInvalidRequest() {
		UserException ex = catchThrowableOfType(
			() -> sut.provisionSocialUser(new SocialUserProvisionCommand(SocialProvider.KAKAO, "pid", " ", "nick")),
			UserException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("프로비저닝: 기존 유저가 탈퇴 상태면 USER_WITHDRAWN")
	void provisionSocialUser_whenWithdrawn_thenUserWithdrawn() {
		User user = userRepositoryPort.save(User.createProvisioned("e@e.com", "nick"));
		user.withdraw();
		SocialAccount account = SocialAccount.link(SocialProvider.KAKAO, "pid", user);
		socialAccountRepositoryPort.put(account);

		UserException ex = catchThrowableOfType(
			() -> sut.provisionSocialUser(new SocialUserProvisionCommand(SocialProvider.KAKAO, "pid", null, null)),
			UserException.class);
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN);
	}

	@Test
	@DisplayName("프로비저닝: 저장 중 중복이 발생하면 재조회 후 기존 유저를 반환하고 프로필을 동기화")
	void provisionSocialUser_whenDuplicateRace_thenSyncExistingUser() {
		User existingUser = userRepositoryPort.save(User.createProvisioned("old@e.com", "old"));
		SocialAccount existingAccount = SocialAccount.link(SocialProvider.KAKAO, "pid", existingUser);
		socialAccountRepositoryPort.triggerDuplicateOnNextSave(existingAccount);

		UserProvisioningUseCase.ProvisioningResult out = sut.provisionSocialUser(
			new SocialUserProvisionCommand(SocialProvider.KAKAO, "pid", "new@e.com", "new"));

		assertThat(out.userId()).isEqualTo(existingUser.getId());
		assertThat(out.isNewUser()).isFalse();
		assertThat(existingUser.getEmail()).isEqualTo("new@e.com");
		assertThat(existingUser.getNickname()).isEqualTo("new");
	}
}
