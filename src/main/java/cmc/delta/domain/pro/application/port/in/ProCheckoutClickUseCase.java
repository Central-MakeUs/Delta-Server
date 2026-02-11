package cmc.delta.domain.pro.application.port.in;

import cmc.delta.domain.pro.application.port.in.result.ProCheckoutClickStatsResponse;

public interface ProCheckoutClickUseCase {

	void trackCheckoutClick(Long userId);

	ProCheckoutClickStatsResponse getCheckoutClickStats();
}
