package cmc.delta.domain.user.model;

import cmc.delta.domain.auth.model.SocialProvider;

public record UserWithProvider(User user, SocialProvider provider) {
}
