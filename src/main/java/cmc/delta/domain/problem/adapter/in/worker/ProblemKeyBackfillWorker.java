package cmc.delta.domain.problem.adapter.in.worker;

import cmc.delta.domain.problem.adapter.out.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.adapter.out.persistence.problem.ProblemJpaRepository;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.problem.Problem;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProblemKeyBackfillWorker {

	private final TransactionTemplate workerTxTemplate;
	private final ProblemJpaRepository problemRepository;
	private final AssetJpaRepository assetRepository;

	public int runOnce(int batchSize) {
		return workerTxTemplate.execute(status -> {
			List<Problem> candidates = problemRepository.findKeyBackfillCandidates(PageRequest.of(0, batchSize));
			for (Problem p : candidates) {
				backfillOne(p);
			}
			return candidates.size();
		});
	}

	private void backfillOne(Problem p) {
		if (p.getScan() == null) {
			return;
		}
		Long scanId = p.getScan().getId();
		if (scanId == null) {
			return;
		}

		Optional<Asset> original = assetRepository.findOriginalByScanId(scanId);
		if (original.isEmpty()) {
			return;
		}

		p.attachOriginalStorageKeyIfEmpty(original.get().getStorageKey());
		p.detachScan();
		problemRepository.saveAndFlush(p);
		log.info("problem key backfill 완료 problemId={} scanId={}", p.getId(), scanId);
	}
}
