package cmc.delta.global.api.response;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PagedResponseTest {

	@Test
	@DisplayName("from: Page를 그대로 PagedResponse로 변환")
	void from_whenPage_thenMapsFields() {
		// given
		PageRequest pageable = PageRequest.of(0, 10);
		Page<String> page = new PageImpl<>(List.of("a", "b"), pageable, 25);

		// when
		PagedResponse<String> resp = PagedResponse.from(page);

		// then
		assertThat(resp.content()).containsExactly("a", "b");
		assertThat(resp.page()).isEqualTo(0);
		assertThat(resp.size()).isEqualTo(10);
		assertThat(resp.totalElements()).isEqualTo(25);
		assertThat(resp.totalPages()).isEqualTo(3);
	}

	@Test
	@DisplayName("of(Page, converter): content를 converter로 매핑")
	void of_whenConverter_thenMapsContent() {
		// given
		PageRequest pageable = PageRequest.of(0, 2);
		Page<Integer> page = new PageImpl<>(List.of(1, 2), pageable, 2);

		// when
		PagedResponse<String> resp = PagedResponse.of(page, String::valueOf);

		// then
		assertThat(resp.content()).containsExactly("1", "2");
		assertThat(resp.page()).isEqualTo(0);
		assertThat(resp.size()).isEqualTo(2);
		assertThat(resp.totalElements()).isEqualTo(2);
		assertThat(resp.totalPages()).isEqualTo(1);
	}

	@Test
	@DisplayName("of(Page, content): Page meta는 유지하고 content만 교체")
	void of_whenProvidedContent_thenKeepsMeta() {
		// given
		PageRequest pageable = PageRequest.of(1, 3);
		Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), pageable, 10);

		// when
		PagedResponse<String> resp = PagedResponse.of(page, List.of("x"));

		// then
		assertThat(resp.content()).containsExactly("x");
		assertThat(resp.page()).isEqualTo(1);
		assertThat(resp.size()).isEqualTo(3);
		assertThat(resp.totalElements()).isEqualTo(10);
		assertThat(resp.totalPages()).isEqualTo(4);
	}
}
