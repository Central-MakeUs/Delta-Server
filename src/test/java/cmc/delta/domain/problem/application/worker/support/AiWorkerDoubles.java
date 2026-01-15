package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.scan.port.out.ai.AiClient;
import cmc.delta.domain.problem.application.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.persistence.scan.ProblemScanJpaRepository;
import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;

public record AiWorkerDoubles(
	ProblemScanJpaRepository scanRepo,
	UnitJpaRepository unitRepo,
	ProblemTypeJpaRepository typeRepo,
	AiClient aiClient,
	TransactionTemplate tx,
	AiWorkerProperties props
) {
	public static AiWorkerDoubles create() {
		AiWorkerProperties props = new AiWorkerProperties(2000L, 10, 30L, 1, 1);

		AiWorkerDoubles d = new AiWorkerDoubles(
			mock(ProblemScanJpaRepository.class),
			mock(UnitJpaRepository.class),
			mock(ProblemTypeJpaRepository.class),
			mock(AiClient.class),
			WorkerTestTx.immediateTx(),
			props
		);

		when(d.unitRepo().findAllRootUnitsActive()).thenReturn(List.of());
		when(d.unitRepo().findAllChildUnitsActive()).thenReturn(List.of());
		when(d.typeRepo().findAllActiveForUser(anyLong())).thenReturn(List.of());

		return d;
	}
}
