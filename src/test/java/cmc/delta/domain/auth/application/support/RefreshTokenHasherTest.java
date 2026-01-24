package cmc.delta.domain.auth.application.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

	@Test
	@DisplayName("sha256: 입력이 같으면 해시가 항상 동일하다")
	void sha256_isDeterministic() {
		String a = RefreshTokenHasher.sha256("hello");
		String b = RefreshTokenHasher.sha256("hello");
		assertThat(a).isEqualTo(b);
	}

	@Test
	@DisplayName("sha256: 알려진 입력은 알려진 해시 값을 가진다")
	void sha256_knownVector() {
		assertThat(RefreshTokenHasher.sha256("hello"))
			.isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
	}
}
