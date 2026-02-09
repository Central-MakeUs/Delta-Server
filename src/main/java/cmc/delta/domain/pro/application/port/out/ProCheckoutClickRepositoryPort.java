package cmc.delta.domain.pro.application.port.out;

public interface ProCheckoutClickRepositoryPort {

	void saveClick(Long userId);

	long countTotalClicks();

	long countUniqueUsers();
}
