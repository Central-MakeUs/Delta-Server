package cmc.delta.domain.problem.adapter.in.worker.config;

import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.properties.PurgeWorkerProperties;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class WorkerInfraConfig {

	private static final int MIN_CONCURRENCY = 1;
	private static final int MIN_QUEUE_CAPACITY = 1;
	private static final String OCR_THREAD_PREFIX = "ocr-worker-";
	private static final String AI_THREAD_PREFIX = "ai-worker-";
	private static final String PURGE_THREAD_PREFIX = "purge-worker-";

	@Bean
	public TransactionTemplate workerTxTemplate(PlatformTransactionManager txManager) {
		return new TransactionTemplate(txManager);
	}

	@Bean(name = "ocrExecutor")
	public ThreadPoolTaskExecutor ocrExecutor(OcrWorkerProperties props) {
		return buildExecutor(props.concurrency(), props.batchSize(), OCR_THREAD_PREFIX);
	}

	@Bean(name = "aiExecutor")
	public ThreadPoolTaskExecutor aiExecutor(AiWorkerProperties props) {
		return buildExecutor(props.concurrency(), props.batchSize(), AI_THREAD_PREFIX);
	}

	@Bean(name = "purgeExecutor")
	public ThreadPoolTaskExecutor purgeExecutor(PurgeWorkerProperties props) {
		return buildExecutor(props.concurrency(), props.batchSize(), PURGE_THREAD_PREFIX);
	}

	// 큐가 없으면 batchSize > concurrency 인 배치의 초과분이 CallerRunsPolicy 로 스케줄러 스레드에서 실행된다.
	private ThreadPoolTaskExecutor buildExecutor(int concurrency, int batchSize, String threadPrefix) {
		int poolSize = Math.max(MIN_CONCURRENCY, concurrency);
		int queueCapacity = Math.max(MIN_QUEUE_CAPACITY, batchSize);
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(poolSize);
		executor.setMaxPoolSize(poolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix(threadPrefix);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
