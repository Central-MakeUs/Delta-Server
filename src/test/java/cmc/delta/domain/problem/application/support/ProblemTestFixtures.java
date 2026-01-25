package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.user.model.User;

public final class ProblemTestFixtures {

	private ProblemTestFixtures() {}

	public static UploadFile file() {
		return new UploadFile("x".getBytes(), "image/png", "a.png");
	}

	public static User user() {
		return User.createProvisioned("user@example.com", "delta");
	}
}
