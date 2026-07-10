package cmc.delta.domain.version.application.service;

import cmc.delta.domain.version.application.port.in.AppVersionQueryUseCase;
import cmc.delta.domain.version.application.port.in.result.AppVersionResponse;
import cmc.delta.domain.version.application.port.out.AppVersionPolicyRepositoryPort;
import cmc.delta.domain.version.model.AppVersionPolicy;
import cmc.delta.domain.version.model.SemVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppVersionQueryService implements AppVersionQueryUseCase {

	private final AppVersionPolicyRepositoryPort repositoryPort;

	@Override
	@Transactional
	public AppVersionResponse getAppVersion(String currentVersion) {
		SemVersion current = SemVersion.parse(currentVersion);
		AppVersionPolicy policy = getOrCreateDefaultPolicy();

		SemVersion minimum = SemVersion.parse(policy.getMinimumVersion());
		SemVersion latest = SemVersion.parse(policy.getLatestVersion());

		boolean forceUpdate = current.compareTo(minimum) < 0;
		boolean updateAvailable = current.compareTo(latest) < 0;
		return new AppVersionResponse(
			policy.getMinimumVersion(),
			policy.getLatestVersion(),
			forceUpdate,
			updateAvailable);
	}

	@Transactional
	public void initializeDefaultPolicy() {
		getOrCreateDefaultPolicy();
	}

	private AppVersionPolicy getOrCreateDefaultPolicy() {
		return repositoryPort.findDefaultPolicy()
			.orElseGet(() -> repositoryPort.save(AppVersionPolicy.createDefault()));
	}
}
