package cmc.delta.domain.problem.adapter.in.worker.support;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LockOwnerProvider {

	private final String lockOwner = "worker-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

	public String get() {
		return lockOwner;
	}
}
