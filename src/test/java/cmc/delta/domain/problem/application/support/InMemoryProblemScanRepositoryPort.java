package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.problem.application.port.out.scan.ProblemScanRepositoryPort;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryProblemScanRepositoryPort implements ProblemScanRepositoryPort {

	private final Map<Long, ProblemScan> store = new HashMap<>();
	private final AtomicLong seq = new AtomicLong(0);

	@Override
	public ProblemScan save(ProblemScan scan) {
		if (scan.getId() == null) {
			ReflectionIds.setId(scan, seq.incrementAndGet());
		}
		store.put(scan.getId(), scan);
		return scan;
	}

	@Override
	public Optional<ProblemScan> findOwnedByForUpdate(Long scanId, Long userId) {
		ProblemScan s = store.get(scanId);
		if (s == null) return Optional.empty();
		Long ownerId = s.getUser().getId();
		return ownerId != null && ownerId.equals(userId) ? Optional.of(s) : Optional.empty();
	}

	public int count() {
		return store.size();
	}

	public ProblemScan get(Long id) {
		ProblemScan s = store.get(id);
		if (s == null) throw new IllegalStateException("scan not found id=" + id);
		return s;
	}
}
