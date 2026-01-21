package cmc.delta.domain.problem.adapter.in.web.problem.dto.request;

public enum ProblemListSort {
	RECENT,
	OLDEST,

	UNIT_MOST,   // 단원 최다 등록순
	UNIT_LEAST,  // 단원 최소 등록순
	TYPE_MOST,   // 유형 최다 등록순
	TYPE_LEAST   // 유형 최소 등록순
}
