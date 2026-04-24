package cmc.delta.domain.stats.adapter.in.scheduler;

import cmc.delta.domain.stats.adapter.out.discord.DailyStatsDiscordNotifier;
import cmc.delta.domain.stats.application.dto.DailyStatsReport;
import cmc.delta.domain.stats.application.service.DailyStatsQueryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatsScheduler {

	private final DailyStatsQueryService statsQueryService;
	private final DailyStatsDiscordNotifier discordNotifier;

	@PostConstruct
	public void init() {
		tick();
	}

	@Scheduled(cron = "${stats.daily.cron:0 0 21 * * *}", zone = "Asia/Seoul")
	public void tick() {
		log.info("일일 통계 배치 시작");
		DailyStatsReport report = statsQueryService.generate();
		discordNotifier.send(report);
		log.info("일일 통계 배치 완료");
	}
}
