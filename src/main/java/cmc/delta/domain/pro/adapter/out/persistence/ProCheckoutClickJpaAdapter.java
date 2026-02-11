package cmc.delta.domain.pro.adapter.out.persistence;

import cmc.delta.domain.pro.application.port.out.ProCheckoutClickRepositoryPort;
import cmc.delta.domain.pro.model.ProCheckoutClick;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProCheckoutClickJpaAdapter implements ProCheckoutClickRepositoryPort {

	private final ProCheckoutClickJpaRepository repository;

	@Override
	public void saveClick(Long userId) {
		repository.save(ProCheckoutClick.create(userId));
	}

	@Override
	public long countTotalClicks() {
		return repository.count();
	}

	@Override
	public long countUniqueUsers() {
		return repository.countDistinctUsers();
	}
}
