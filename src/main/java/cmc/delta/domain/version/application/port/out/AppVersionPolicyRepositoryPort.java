package cmc.delta.domain.version.application.port.out;

import cmc.delta.domain.version.model.AppVersionPolicy;
import java.util.Optional;

public interface AppVersionPolicyRepositoryPort {

	Optional<AppVersionPolicy> findDefaultPolicy();

	AppVersionPolicy save(AppVersionPolicy policy);
}
