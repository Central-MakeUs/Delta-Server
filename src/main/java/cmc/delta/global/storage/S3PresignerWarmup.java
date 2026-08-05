package cmc.delta.global.storage;

import cmc.delta.global.storage.application.StorageService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3PresignerWarmup implements ApplicationRunner {

	private static final String WARMUP_KEY = "warmup/dummy";

	private final StorageService storageService;

	@Override
	public void run(ApplicationArguments args) {
		long start = System.nanoTime();
		try {
			storageService.issueReadUrl(WARMUP_KEY, null);
		} catch (Exception e) {
			// 더미 키라 S3에 없어도 presign 서명 자체는 로컬에서 완료됨
			log.debug("[WARMUP] presign 워밍업 예외 (무시 가능) reason={}", e.getMessage());
		} finally {
			long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
			log.info("[WARMUP] S3Presigner 초기화 완료 ({}ms)", ms);
		}
	}
}
