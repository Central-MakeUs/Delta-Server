package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.persistence.ProblemTypeJpaRepository;
import cmc.delta.domain.curriculum.persistence.UnitJpaRepository;
import cmc.delta.domain.problem.application.port.ai.AiClient;
import cmc.delta.domain.problem.persistence.ProblemScanJpaRepository;
import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;

public record AiWorkerDoubles(
	ProblemScanJpaRepository scanRepo,
	UnitJpaRepository unitRepo,
	ProblemTypeJpaRepository typeRepo,
	AiClient aiClient,
	TransactionTemplate tx
) {
	public static AiWorkerDoubles create() {
		AiWorkerDoubles d = new AiWorkerDoubles(
			mock(ProblemScanJpaRepository.class),
			mock(UnitJpaRepository.class),
			mock(ProblemTypeJpaRepository.class),
			mock(AiClient.class),
			WorkerTestTx.immediateTx()
		);

		when(d.unitRepo().findAllRootUnitsActive()).thenReturn(List.of());
		when(d.unitRepo().findAllChildUnitsActive()).thenReturn(List.of());
		when(d.typeRepo().findAllActiveForUser(anyLong())).thenReturn(List.of());

		return d;
	}
}
