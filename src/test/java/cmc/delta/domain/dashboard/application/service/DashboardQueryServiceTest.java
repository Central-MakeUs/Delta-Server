package cmc.delta.domain.dashboard.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import cmc.delta.domain.dashboard.application.dto.DashboardProblemDetailResponse;
import cmc.delta.domain.dashboard.application.dto.DashboardProblemDetailRow;
import cmc.delta.domain.dashboard.application.dto.DashboardUsersResponse;
import cmc.delta.domain.dashboard.application.exception.DashboardException;
import cmc.delta.domain.dashboard.application.port.out.DashboardMonthlyAccessQueryPort;
import cmc.delta.domain.dashboard.application.port.out.DashboardProblemQueryPort;
import cmc.delta.domain.dashboard.application.port.out.DashboardUserQueryPort;
import cmc.delta.domain.dashboard.model.enums.DashboardSortDirection;
import cmc.delta.domain.dashboard.model.enums.DashboardUserSortBy;
import cmc.delta.domain.user.model.enums.UserRole;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.storage.port.out.StoragePort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.PageRequest;

class DashboardQueryServiceTest {

	private DashboardUserQueryPort dashboardUserQueryPort;
	private DashboardMonthlyAccessQueryPort dashboardMonthlyAccessQueryPort;
	private DashboardProblemQueryPort dashboardProblemQueryPort;
	private StoragePort storagePort;

	private DashboardQueryService sut;

	@BeforeEach
	void setUp() {
		dashboardUserQueryPort = mock(DashboardUserQueryPort.class);
		dashboardMonthlyAccessQueryPort = mock(DashboardMonthlyAccessQueryPort.class);
		dashboardProblemQueryPort = mock(DashboardProblemQueryPort.class);
		storagePort = mock(StoragePort.class);

		sut = new DashboardQueryService(
			dashboardUserQueryPort,
			dashboardMonthlyAccessQueryPort,
			dashboardProblemQueryPort,
			storagePort);
	}

	@Test
	@DisplayName("getUsers: 정렬 조건을 사용자 조회 포트에 전달한다")
	void getUsers_passesSortOptionsToPort() {
		// given
		PageRequest pageable = PageRequest.of(1, 10);
		when(dashboardUserQueryPort.findUsers(
			pageable,
			DashboardUserSortBy.ACCESS_COUNT,
			DashboardSortDirection.ASC))
			.thenReturn(List.of());
		when(dashboardUserQueryPort.countUsers()).thenReturn(0L);

		// when
		DashboardUsersResponse response = sut.getUsers(
			pageable,
			DashboardUserSortBy.ACCESS_COUNT,
			DashboardSortDirection.ASC);

		// then
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(10);
		verify(dashboardUserQueryPort).findUsers(
			pageable,
			DashboardUserSortBy.ACCESS_COUNT,
			DashboardSortDirection.ASC);
		verify(dashboardUserQueryPort).countUsers();
	}

	@Test
	@DisplayName("getProblemDetail: storageKey로 이미지 조회 URL을 발급해 imageUrl로 응답한다")
	void getProblemDetail_ok_includesOriginalImageViewUrl() {
		// given
		LocalDateTime registeredAt = LocalDateTime.of(2026, 7, 2, 12, 30);
		DashboardProblemDetailRow row = new DashboardProblemDetailRow(
			1L,
			"일차방정식",
			"방정식",
			"계산",
			3L,
			7L,
			registeredAt,
			true,
			UserRole.USER,
			"problems/original.png");

		when(dashboardProblemQueryPort.findProblemDetail(1L)).thenReturn(Optional.of(row));
		when(storagePort.issueReadUrl("problems/original.png")).thenReturn("https://read/original.png");

		// when
		DashboardProblemDetailResponse response = sut.getProblemDetail(1L);

		// then
		assertThat(response.problemId()).isEqualTo(1L);
		assertThat(response.problemName()).isEqualTo("일차방정식");
		assertThat(response.unitName()).isEqualTo("방정식");
		assertThat(response.problemType()).isEqualTo("계산");
		assertThat(response.aiSolutionCount()).isEqualTo(3L);
		assertThat(response.viewCount()).isEqualTo(7L);
		assertThat(response.registeredAt()).isEqualTo(registeredAt);
		assertThat(response.wrongAnswerCompleted()).isTrue();
		assertThat(response.userRole()).isEqualTo(UserRole.USER);
		assertThat(response.imageUrl()).isEqualTo("https://read/original.png");

		verify(dashboardProblemQueryPort).findProblemDetail(1L);
		verify(storagePort).issueReadUrl("problems/original.png");
		verifyNoMoreInteractions(dashboardProblemQueryPort, storagePort);
	}

	@Test
	@DisplayName("getProblemDetail: 문제가 없으면 PROBLEM_NOT_FOUND 예외를 던진다")
	void getProblemDetail_notFound_throwsProblemNotFound() {
		// given
		when(dashboardProblemQueryPort.findProblemDetail(404L)).thenReturn(Optional.empty());

		// when
		DashboardException exception = catchThrowableOfType(
			() -> sut.getProblemDetail(404L),
			DashboardException.class);

		// then
		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROBLEM_NOT_FOUND);
		verify(dashboardProblemQueryPort).findProblemDetail(404L);
		verifyNoInteractions(storagePort);
	}
}
