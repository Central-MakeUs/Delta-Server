package cmc.delta.domain.auth.adapter.out.oauth.oidc;

import com.nimbusds.jose.jwk.JWKSet;

@FunctionalInterface
public interface OidcJwkSetLoader {
	JWKSet load(String url) throws Exception;
}
