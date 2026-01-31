package cmc.delta.domain.auth.adapter.out.oauth.redis;

import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;

/**
 * Payload stored in Redis for a temporary login key.
 */
public record LoginKeyPayload(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {
}
