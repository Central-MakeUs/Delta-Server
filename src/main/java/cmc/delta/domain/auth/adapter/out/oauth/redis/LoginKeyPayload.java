package cmc.delta.domain.auth.adapter.out.oauth.redis;

import cmc.delta.domain.auth.application.port.in.social.SocialLoginData;
import cmc.delta.domain.auth.application.port.out.TokenIssuer;

/**
 * Redis에 저장되는 임시 로그인 키 페이로드입니다.
 *
 * 이 레코드는 소셜 로그인으로 생성된 사용자 데이터(SocialLoginData)와
 * 발급된 토큰(TokenIssuer.IssuedTokens)을 함께 보관하기 위해 사용됩니다.
 */
public record LoginKeyPayload(SocialLoginData data, TokenIssuer.IssuedTokens tokens) {
}
