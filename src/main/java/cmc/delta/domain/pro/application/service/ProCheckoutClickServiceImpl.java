package cmc.delta.domain.pro.application.service;

import cmc.delta.domain.pro.application.port.in.ProCheckoutClickUseCase;
import cmc.delta.domain.pro.application.port.in.result.ProCheckoutClickStatsResponse;
import cmc.delta.domain.pro.application.port.out.ProCheckoutClickRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProCheckoutClickServiceImpl implements ProCheckoutClickUseCase {

	private final ProCheckoutClickRepositoryPort repositoryPort;

	@Override
	@Transactional
	public void trackCheckoutClick(Long userId) {
		repositoryPort.saveClick(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public ProCheckoutClickStatsResponse getCheckoutClickStats() {
		long total = repositoryPort.countTotalClicks();
		long unique = repositoryPort.countUniqueUsers();
		return new ProCheckoutClickStatsResponse(total, unique);
	}
}
