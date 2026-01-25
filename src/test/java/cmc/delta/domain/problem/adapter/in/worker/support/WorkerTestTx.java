package cmc.delta.domain.problem.adapter.in.worker.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public final class WorkerTestTx {

	private WorkerTestTx() {}

	public static TransactionTemplate immediateTx() {
		TransactionTemplate tx = mock(TransactionTemplate.class);

		when(tx.execute(any(TransactionCallback.class)))
			.thenAnswer(inv -> {
				TransactionCallback<?> cb = inv.getArgument(0);
				return cb.doInTransaction(null);
			});

		doAnswer(inv -> {
			@SuppressWarnings("unchecked") Consumer<TransactionStatus> c = inv.getArgument(0, Consumer.class);
			c.accept(null);
			return null;
		}).when(tx).executeWithoutResult(any());

		return tx;
	}
}
