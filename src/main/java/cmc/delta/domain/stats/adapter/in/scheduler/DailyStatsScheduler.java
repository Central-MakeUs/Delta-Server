package cmc.delta.domain.stats.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;

import cmc.delta.domain.stats.adapter.out.discord.DailyStatsDiscordNotifier;
import cmc.delta.domain.stats.application.dto.DailyStatsReport;
import cmc.delta.domain.stats.application.service.DailyStatsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DailyStatsScheduler {

	private final DailyStatsQueryService statsQueryService;
	private final DailyStatsDiscordNotifier discordNotifier;

	@Scheduled(cron = "${stats.daily.cron:0 0 21 * * *}", zone = "Asia/Seoul")
	public void tick() {
		log.info("일일 통계 배치 시작");
		DailyStatsReport report = statsQueryService.generate();
		discordNotifier.send(report);
		log.info("일일 통계 배치 완료");
	}
}
