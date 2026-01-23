package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.user.model.User;
import org.springframework.web.multipart.MultipartFile;
import static org.mockito.Mockito.*;

public final class ProblemTestFixtures {

	private ProblemTestFixtures() {}

	public static MultipartFile file() {
		return mock(MultipartFile.class);
	}

	public static User user() {
		return User.createProvisioned("user@example.com", "delta");
	}
}
