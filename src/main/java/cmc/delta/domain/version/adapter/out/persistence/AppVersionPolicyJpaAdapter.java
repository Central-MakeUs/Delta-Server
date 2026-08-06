package cmc.delta.domain.version.adapter.out.persistence;

import cmc.delta.domain.version.application.port.out.AppVersionPolicyRepositoryPort;
import cmc.delta.domain.version.model.AppVersionPolicy;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AppVersionPolicyJpaAdapter implements AppVersionPolicyRepositoryPort {

	private final AppVersionPolicyJpaRepository repository;

	@Override
	public Optional<AppVersionPolicy> findDefaultPolicy() {
		return repository.findById(AppVersionPolicy.DEFAULT_POLICY_ID);
	}

	@Override
	public AppVersionPolicy save(AppVersionPolicy policy) {
		return repository.save(policy);
	}
}
