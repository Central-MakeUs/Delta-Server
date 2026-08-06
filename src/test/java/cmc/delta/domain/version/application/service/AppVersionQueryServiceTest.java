package cmc.delta.domain.version.application.service;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.version.application.port.in.result.AppVersionResponse;
import cmc.delta.domain.version.application.port.out.AppVersionPolicyRepositoryPort;
import cmc.delta.domain.version.model.AppVersionPolicy;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppVersionQueryServiceTest {

	@Test
	@DisplayName("현재 버전이 minimum/latest와 같으면 업데이트가 필요하지 않음")
	void getAppVersion_whenCurrentEqualsPolicy_thenNoUpdate() {
		// given
		AppVersionQueryService service = new AppVersionQueryService(new FakeRepo(policy("1.0.0", "1.0.0")));

		// when
		AppVersionResponse response = service.getAppVersion("1.0.0");

		// then
		assertThat(response.minimumVersion()).isEqualTo("1.0.0");
		assertThat(response.latestVersion()).isEqualTo("1.0.0");
		assertThat(response.forceUpdate()).isFalse();
		assertThat(response.updateAvailable()).isFalse();
	}

	@Test
	@DisplayName("현재 버전이 minimum보다 낮으면 강제 업데이트 대상")
	void getAppVersion_whenCurrentLowerThanMinimum_thenForceUpdate() {
		// given
		AppVersionQueryService service = new AppVersionQueryService(new FakeRepo(policy("1.0.0", "1.1.0")));

		// when
		AppVersionResponse response = service.getAppVersion("0.9.9");

		// then
		assertThat(response.forceUpdate()).isTrue();
		assertThat(response.updateAvailable()).isTrue();
	}

	@Test
	@DisplayName("현재 버전이 latest보다 낮으면 선택 업데이트 대상")
	void getAppVersion_whenCurrentLowerThanLatest_thenUpdateAvailable() {
		// given
		AppVersionQueryService service = new AppVersionQueryService(new FakeRepo(policy("1.0.0", "1.1.0")));

		// when
		AppVersionResponse response = service.getAppVersion("1.0.0");

		// then
		assertThat(response.forceUpdate()).isFalse();
		assertThat(response.updateAvailable()).isTrue();
	}

	@Test
	@DisplayName("버전은 문자열이 아닌 숫자 단위로 비교")
	void getAppVersion_comparesVersionNumbers() {
		// given
		AppVersionQueryService service = new AppVersionQueryService(new FakeRepo(policy("1.0.0", "1.2.0")));

		// when
		AppVersionResponse response = service.getAppVersion("1.10.0");

		// then
		assertThat(response.forceUpdate()).isFalse();
		assertThat(response.updateAvailable()).isFalse();
	}

	@Test
	@DisplayName("잘못된 버전 형식이면 INVALID_REQUEST 예외")
	void getAppVersion_whenInvalidVersion_thenThrowsInvalidRequest() {
		// given
		AppVersionQueryService service = new AppVersionQueryService(new FakeRepo(policy("1.0.0", "1.0.0")));

		// when
		assertThatThrownBy(() -> service.getAppVersion("1.0"))
			.isInstanceOfSatisfying(
				BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
	}

	@Test
	@DisplayName("기본 정책이 없으면 1.0.0 정책을 생성")
	void getAppVersion_whenPolicyMissing_thenCreatesDefaultPolicy() {
		// given
		FakeRepo repo = new FakeRepo(null);
		AppVersionQueryService service = new AppVersionQueryService(repo);

		// when
		AppVersionResponse response = service.getAppVersion("1.0.0");

		// then
		assertThat(repo.saved).isNotNull();
		assertThat(response.minimumVersion()).isEqualTo("1.0.0");
		assertThat(response.latestVersion()).isEqualTo("1.0.0");
	}

	private AppVersionPolicy policy(String minimumVersion, String latestVersion) {
		return AppVersionPolicy.create(AppVersionPolicy.DEFAULT_POLICY_ID, minimumVersion, latestVersion);
	}

	private static class FakeRepo implements AppVersionPolicyRepositoryPort {

		private AppVersionPolicy policy;
		private AppVersionPolicy saved;

		private FakeRepo(AppVersionPolicy policy) {
			this.policy = policy;
		}

		@Override
		public Optional<AppVersionPolicy> findDefaultPolicy() {
			return Optional.ofNullable(policy);
		}

		@Override
		public AppVersionPolicy save(AppVersionPolicy policy) {
			this.policy = policy;
			this.saved = policy;
			return policy;
		}
	}
}
