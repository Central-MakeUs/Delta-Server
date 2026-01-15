package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.ai.AiClient;
import cmc.delta.domain.problem.application.port.ocr.ObjectStorageReader;
import cmc.delta.domain.problem.application.port.ocr.OcrClient;
import cmc.delta.domain.problem.persistence.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
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
