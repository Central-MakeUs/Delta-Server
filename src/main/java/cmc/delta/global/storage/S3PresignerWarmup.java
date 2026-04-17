package cmc.delta.global.storage;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import cmc.delta.global.storage.application.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3PresignerWarmup implements ApplicationRunner {

	private final StorageService storageService;

	@Override
	public void run(ApplicationArguments args) {
		long start = System.nanoTime();
		try {
			storageService.issueReadUrl("warmup/dummy", null);
		} catch (Exception e) {
			// 더미 키라 S3에 없어도 presign 서명 자체는 로컬에서 완료됨
		} finally {
			long ms = (System.nanoTime() - start) / 1_000_000;
			log.info("[WARMUP] S3Presigner 초기화 완료 ({}ms)", ms);
		}
	}
}
