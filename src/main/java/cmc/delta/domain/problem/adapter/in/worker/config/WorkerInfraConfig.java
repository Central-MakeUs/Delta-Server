package cmc.delta.domain.problem.adapter.in.worker.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cmc.delta.domain.problem.adapter.in.worker.properties.AiWorkerProperties;
import cmc.delta.domain.problem.adapter.in.worker.properties.OcrWorkerProperties;

@Configuration
public class WorkerInfraConfig {

	@Bean
	public TransactionTemplate workerTxTemplate(PlatformTransactionManager txManager) {
		return new TransactionTemplate(txManager);
	}

	@Bean(name = "ocrExecutor")
	public ThreadPoolTaskExecutor ocrExecutor(OcrWorkerProperties props) {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(props.concurrency());
		ex.setMaxPoolSize(props.concurrency());
		ex.setQueueCapacity(0);
		ex.setThreadNamePrefix("ocr-worker-");
		ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 과부하 시 현재 쓰레드 처리
		ex.initialize();
		return ex;
	}

	@Bean(name = "aiExecutor")
	public ThreadPoolTaskExecutor aiExecutor(AiWorkerProperties props) {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(props.concurrency());
		ex.setMaxPoolSize(props.concurrency());
		ex.setQueueCapacity(0);
		ex.setThreadNamePrefix("ai-worker-");
		ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		ex.initialize();
		return ex;
	}
}
