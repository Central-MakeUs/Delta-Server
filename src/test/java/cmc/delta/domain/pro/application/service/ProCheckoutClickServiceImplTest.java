package cmc.delta.domain.pro.application.service;

import static org.assertj.core.api.Assertions.*;

import cmc.delta.domain.pro.application.port.in.result.ProCheckoutClickStatsResponse;
import cmc.delta.domain.pro.application.port.out.ProCheckoutClickRepositoryPort;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProCheckoutClickServiceImplTest {

	@Test
	@DisplayName("결제 버튼 클릭: 총 클릭수는 누적, 유니크 유저수는 계정당 1회만 카운팅")
	void trackCheckoutClick_countsTotalAndUniqueUsers() {
		// given
		FakeRepo repo = new FakeRepo();
		ProCheckoutClickServiceImpl service = new ProCheckoutClickServiceImpl(repo);

		// when
		service.trackCheckoutClick(10L);
		service.trackCheckoutClick(10L);
		service.trackCheckoutClick(10L);
		service.trackCheckoutClick(20L);

		ProCheckoutClickStatsResponse stats = service.getCheckoutClickStats();

		// then
		assertThat(stats.totalClicks()).isEqualTo(4L);
		assertThat(stats.uniqueUsers()).isEqualTo(2L);
	}

	private static class FakeRepo implements ProCheckoutClickRepositoryPort {

		private final List<Long> clicks = new ArrayList<>();

		@Override
		public void saveClick(Long userId) {
			clicks.add(userId);
		}

		@Override
		public long countTotalClicks() {
			return clicks.size();
		}

		@Override
		public long countUniqueUsers() {
			Set<Long> unique = new HashSet<>(clicks);
			return unique.size();
		}
	}
}
