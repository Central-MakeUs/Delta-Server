package cmc.delta.domain.user.application.support;

import cmc.delta.domain.user.model.User;

public final class UserFixtures {

	private UserFixtures() {}

	public static User activeUser() {
		return User.create("user@example.com", "delta");
	}

	public static User withdrawnUser() {
		User user = activeUser();
		user.withdraw();
		return user;
	}
}
