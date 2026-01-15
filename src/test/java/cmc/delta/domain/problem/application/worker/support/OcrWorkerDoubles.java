package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.problem.application.port.ocr.ObjectStorageReader;
import cmc.delta.domain.problem.application.port.ocr.OcrClient;
import cmc.delta.domain.problem.persistence.AssetJpaRepository;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import org.springframework.transaction.support.TransactionTemplate;

public record OcrWorkerDoubles(
	ProblemScanJpaRepository scanRepo,
	AssetJpaRepository assetRepo,
	ObjectStorageReader storageReader,
	OcrClient ocrClient,
	TransactionTemplate tx
) {
	public static OcrWorkerDoubles create() {
		return new OcrWorkerDoubles(
			mock(ProblemScanJpaRepository.class),
			mock(AssetJpaRepository.class),
			mock(ObjectStorageReader.class),
			mock(OcrClient.class),
			WorkerTestTx.immediateTx()
		);
	}
}
