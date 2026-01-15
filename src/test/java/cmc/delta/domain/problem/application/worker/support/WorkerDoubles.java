package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.scan.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.scan.port.out.storage.ObjectStorageReader;
import cmc.delta.domain.problem.application.scan.port.out.ocr.OcrClient;
import cmc.delta.domain.problem.persistence.asset.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import org.springframework.transaction.support.TransactionTemplate;

public record WorkerDoubles(
	ProblemScanJpaRepository scanRepo,
	AssetJpaRepository assetRepo,
	ObjectStorageReader storageReader,
	OcrClient ocrClient,
	AiClient aiClient,
	TransactionTemplate tx
) {
	public static WorkerDoubles create() {
		return new WorkerDoubles(
			mock(ProblemScanJpaRepository.class),
			mock(AssetJpaRepository.class),
			mock(ObjectStorageReader.class),
			mock(OcrClient.class),
			mock(AiClient.class),
			WorkerTestTx.immediateTx()
		);
	}
}
