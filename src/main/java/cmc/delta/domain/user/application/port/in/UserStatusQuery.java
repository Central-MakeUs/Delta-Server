package cmc.delta.domain.user.application.port.in;

import cmc.delta.domain.user.model.enums.UserStatus;

public interface UserStatusQuery {
	UserStatus getStatus(long userId);
}
