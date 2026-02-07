package cmc.delta.global.config.swagger;

public final class ProblemApiDocs {

	private ProblemApiDocs() {}

	public static final String CREATE_WRONG_ANSWER_CARD = """
		오답카드를 생성합니다. (scanId 기반)

		필드 설명:
		- scanId: AI_DONE 상태의 스캔 작업카드 ID
		- finalUnitId: 최종 단원(Unit) ID (자식 단원) 예) U_CALC2_DIFF
		- finalTypeId: 최종 문제 유형(ProblemType) ID 예) T_SENTENCE

		정답 입력 형식(answerFormat):
		- CHOICE: 객관식 (answerChoiceNo 사용)
		- TEXT: 텍스트 단답/서술 (answerValue 사용)
		- NUMBER: 숫자 (answerValue 사용)
		- EXPRESSION: 수식(텍스트 표현) (answerValue 사용)

		상태 정책:
		- 오답카드 생성은 'UNSOLVED(오답 완료 전)'로 시작합니다.
		- 'SOLVED/UNSOLVED' 판정은 풀이/정답 입력 여부가 아니라, 오답 완료 처리 API 호출 여부(완료 상태값) 기준입니다.
		""";

	public static final String LIST_MY_PROBLEMS = """
		내 오답카드를 페이징 조회합니다.

		필터(선택):
		- subjectId: 과목(Unit) ID (부모 단원, parent_id = null) 예) U_COMMON_1
		- unitId: 단원(Unit) ID (자식 단원, parent_id = subjectId) 예) U_C1_POLY
		- typeId: 문제 유형(ProblemType) ID 예) T_SENTENCE

		정렬(sort):
		- RECENT: 최신순(기본)
		- OLDEST: 오래된순
		- UNIT_MOST: 단원별 최다 등록순 (완료 여부 무관, 동률이면 최신/ID 순)
		- UNIT_LEAST: 단원별 최소 등록순 (완료 여부 무관, 동률이면 최신/ID 순)
		- TYPE_MOST: 유형별 최다 등록순 (완료 여부 무관, 동률이면 최신/ID 순)
		- TYPE_LEAST: 유형별 최소 등록순 (완료 여부 무관, 동률이면 최신/ID 순)

		상태(status): '오답 완료 버튼' 기준
		- ALL: 전체(기본)
		- UNSOLVED: 오답 완료 전
		- SOLVED: 오답 완료

		정렬 동작 설명:
		- UNIT_MOST/UNIT_LEAST는 '같은 단원(finalUnitId)'으로 등록된 오답카드 개수를 기준으로 정렬합니다.
		- TYPE_MOST/TYPE_LEAST는 '같은 유형(finalTypeId)'으로 등록된 오답카드 개수를 기준으로 정렬합니다.
		- 위 4개 정렬은 완료/미완료(status)와 관계없이 "등록된 개수"로만 정렬합니다.
		  (단, status 필터를 함께 주면 목록 자체는 필터링되지만, 정렬 기준이 되는 "개수"는 전체 기준으로 계산됩니다.)
		- 최다/최소 정렬에서 동률이면 최신(createdAt desc) → problemId desc 순으로 정렬합니다.

		페이징:
		- page: 0부터 시작
		- size: 페이지 크기

		주의:
		- 필터 미적용은 파라미터를 생략하거나 빈값으로 보내면 됩니다.
		- Swagger 기본 예시값 "string"은 실제 필터로 동작하므로, 필터를 끄려면 입력값을 지우거나 빈값으로 보내세요.
		""";

	public static final String LIST_MY_PROBLEMS_SCROLL = """
		내 오답카드를 무한 스크롤(커서 기반, No-Offset) 방식으로 조회합니다.

		이 API는 offset/page를 사용하지 않고, 직전 응답의 커서(nextCursor)를 그대로 넘겨 다음 목록을 가져옵니다.

		요청:
		- GET /api/v1/problems/scroll

		필터(선택):
		- subjectId: 과목(Unit) ID (부모 단원) 예) U_COMMON_1
		- unitId: 단원(Unit) ID (자식 단원) 예) U_C1_POLY
		- typeId: 문제 유형(ProblemType) ID 예) T_SENTENCE

		정렬(sort):
		- RECENT: 최신순(기본)
		- OLDEST: 오래된순
		- 커서 페이징은 RECENT/OLDEST만 지원합니다.

		상태(status): '오답 완료 버튼' 기준
		- ALL: 전체(기본)
		- UNSOLVED: 오답 완료 전
		- SOLVED: 오답 완료

		커서 파라미터:
		- 첫 페이지: lastId/lastCreatedAt 없이 호출
		- 다음 페이지: 직전 응답의 nextCursor 값을 그대로 전달
		  - lastId: 마지막으로 받은 항목의 problemId
		  - lastCreatedAt: 마지막으로 받은 항목의 createdAt (ISO-8601)

		페이지 크기:
		- size: 기본 20, 최대 50

		미리보기 URL 포함 여부:
		- includePreviewUrl: true/false (기본 true)
		  - true: previewImage.viewUrl(presigned URL) 포함
		  - false: previewImage.viewUrl은 null, previewImage.storageKey만 포함 (payload/CPU 절감)

		응답:
		- content: 목록
		- hasNext: 다음 페이지 존재 여부
		- nextCursor: 다음 요청에 사용할 커서(lastId, lastCreatedAt)
		- totalElements: 첫 페이지에서만 포함(상단 'N개' 표시용)
		""";

	public static final String GET_MY_PROBLEM_DETAIL = """
		내 오답카드 상세 정보를 조회합니다.

		응답 필드:
		- problemId: 오답카드 ID
		- subject/unit/type: 커리큘럼 정보 (id, name)
		  - subject: 과목(Unit, parent_id = null)
		  - unit: 단원(Unit, parent_id = subject)
		  - type: 문제 유형(ProblemType)

		- originalImage:
		  - assetId: 원본 이미지 Asset ID
		  - viewUrl: 원본 이미지 Presigned GET URL (만료 시간 존재)

		- answerFormat: 정답 형식
		  - CHOICE: 객관식 (answerChoiceNo 사용)
		  - TEXT/NUMBER/EXPRESSION: 단답/숫자/수식 텍스트 (answerValue 사용)

		- answerChoiceNo/answerValue: 저장된 정답 값(없으면 null)
		- solutionText: 저장된 풀이 텍스트(없으면 null)

		- completed: 오답 완료 여부 (completedAt != null)
		- completedAt: 오답 완료 처리 시각(완료 전이면 null)
		- createdAt: 오답카드 생성 시각

		상태 정책:
		- SOLVED/UNSOLVED는 풀이/정답 입력 여부가 아니라, 오답 완료 처리 API 호출 여부(completedAt) 기준입니다.
		""";

	public static final String COMPLETE_WRONG_ANSWER_CARD = """
		오답카드를 '오답 완료(SOLVED)' 상태로 전환합니다.

		상태 정책:
		- status(SOLVED/UNSOLVED)는 풀이/정답 입력 여부가 아니라, 이 API 호출 여부(완료 상태값) 기준입니다.
		- 이미 완료된 오답카드에 재호출해도 멱등으로 동작하도록(변화 없음) 처리하는 것을 권장합니다.
		""";

	public static final String UPDATE_WRONG_ANSWER_CARD = """
		오답카드의 정답/풀이를 수정합니다.

		수정 가능 필드:
		- answerChoiceNo: 객관식 정답 번호 (answerFormat=CHOICE 인 문제에 사용)
		- answerValue: 단답/서술/숫자/수식 정답 값 (answerFormat!=CHOICE 인 문제에 사용)
		- solutionText: 풀이 텍스트

		주의:
		- 문제의 answerFormat(정답 형식)에 따라 유효한 필드가 다릅니다.
		  - CHOICE: answerChoiceNo만 의미 있음 (answerValue는 무시/초기화)
		  - TEXT/NUMBER/EXPRESSION: answerValue만 의미 있음 (answerChoiceNo는 무시/초기화)
		- 완료 여부(SOLVED/UNSOLVED)는 이 API가 아니라 완료 처리 API(completedAt) 기준입니다.
		""";

	public static final String DELETE_WRONG_ANSWER_CARD = """
		내 오답카드를 삭제합니다.

		동작:
		- 본인 소유 오답카드만 삭제할 수 있습니다. (본인 소유가 아니면 404 처리)
		- 오답카드에 연결된 태그/선택지(ProblemChoice, ProblemUnitTag, ProblemTypeTag)는 함께 삭제됩니다.
		- 스캔(ProblemScan) 및 이미지(Asset)는 즉시 삭제하지 않습니다. (별도 purge 정책으로 정리)
		""";

	public static final String STATS_BY_UNIT = """
		내 오답카드를 단원(Unit) 기준으로 집계합니다.

		집계 기준:
		- 단원(unitId = 자식 Unit) 단위로 그룹핑하여
		  solvedCount / unsolvedCount / totalCount 를 제공합니다.

		필터(선택):
		- subjectId: 과목(Unit) ID (부모 단원) 예) U_COMMON_1
		- unitId: 단원(Unit) ID (자식 단원) 예) U_C1_POLY
		  (unitId를 주면 해당 단원만 집계됩니다.)

		정렬(sort):
		- DEFAULT: 단원순(기본)  ※ 현재는 이름 정렬 기반(추후 sortOrder 컬럼 도입 시 커리큘럼 순서로 변경 가능)
		- MAX: 최다 등록순(totalCount desc)
		- MIN: 최소 등록순(totalCount asc)

		주의:
		- 필터 미적용은 파라미터를 생략하거나 빈값으로 보내면 됩니다.
		- Swagger 기본 예시값 "string"은 실제 필터로 동작하므로, 필터를 끄려면 입력값을 지우거나 빈값으로 보내세요.
		""";

	public static final String STATS_BY_TYPE = """
		내 오답카드를 유형(ProblemType) 기준으로 집계합니다.

		집계 기준:
		- 문제 유형(typeId) 단위로 그룹핑하여
		  solvedCount / unsolvedCount / totalCount 를 제공합니다.

		필터(선택):
		- subjectId: 과목(Unit) ID (부모 단원) 예) U_COMMON_1
		- unitId: 단원(Unit) ID (자식 단원) 예) U_C1_POLY
		- typeId: 문제 유형(ProblemType) ID 예) T_SENTENCE
		  (typeId를 주면 해당 유형만 집계됩니다.)

		정렬(sort):
		- DEFAULT: 유형순(기본)  ※ 현재는 이름 정렬 기반(추후 sortOrder 컬럼 도입 시 커리큘럼 순서로 변경 가능)
		- MAX: 최다 등록순(totalCount desc)
		- MIN: 최소 등록순(totalCount asc)

		주의:
		- 필터 미적용은 파라미터를 생략하거나 빈값으로 보내면 됩니다.
		- Swagger 기본 예시값 "string"은 실제 필터로 동작하므로, 필터를 끄려면 입력값을 지우거나 빈값으로 보내세요.
		""";

	public static final String LIST_MY_PROBLEM_TYPES = """
		내 문제 유형(ProblemType) 목록을 조회합니다.

		구성:
		- 기본 유형: 모든 사용자에게 공통으로 제공되는 고정 유형(custom=false)
		- 커스텀 유형: 사용자가 직접 추가한 유형(custom=true)

		요청:
		- GET /api/v1/problem-types
		- Query
		  - includeInactive: boolean (기본 false)
		    - false: active=true만 반환
		    - true: active=false(비활성)도 함께 반환

		응답:
		- types: 유형 리스트 (sortOrder 오름차순)
		  - id: 유형 ID
		    - 기본 유형 예: T_CASE_SPLIT
		    - 커스텀 유형 예: T_C_7f2c0e3d9c5a4b2f9a6d1d3d8b1a2c3d
		  - name: 화면 표시명
		  - sortOrder: 정렬 순서 (오름차순)
		  - active: 선택 가능 여부
		  - custom: 커스텀 여부

		예시:
		- 요청: GET /api/v1/problem-types?includeInactive=false
		- 응답 바디 예시:
		  {
		    "success": true,
		    "code": "OK",
		    "data": {
		      "types": [
		        {"id":"T_CASE_SPLIT","name":"조건별 상황나누기","custom":false,"active":true,"sortOrder":1},
		        {"id":"T_C_7f2c...","name":"서술형","custom":true,"active":true,"sortOrder":7}
		      ]
		    }
		  }
		""";

	public static final String CREATE_CUSTOM_PROBLEM_TYPE = """
		커스텀 유형을 추가합니다.

		입력:
		- Body
		  - name: 유형명 (필수)

		동작:
		- id는 서버에서 자동 생성됩니다.
		- 생성된 커스텀 유형은 active=true로 시작합니다.
		- 같은 사용자가 같은 name의 커스텀 유형을 중복 생성할 수 없습니다.

		예시:
		- 요청: POST /api/v1/problem-types
		- 요청 바디:
		  {"name":"서술형"}
		- 응답 바디 예시:
		  성공:
		  {
		    "success": true,
		    "code": "OK",
		    "data": {"id":"T_C_7f2c...","name":"서술형","custom":true,"active":true,"sortOrder":7}
		  }

		  실패(중복 - 활성):
		  HTTP 409
		  {
		    "status": 409,
		    "code": "REQ_002",
		    "data": null,
		    "message": "이미 추가된 유형입니다."
		  }

		  실패(중복 - 비활성, 복구 가능):
		  HTTP 409
		  {
		    "status": 409,
		    "code": "REQ_002",
		    "data": { "existingTypeId": "T_C_ebc069..." },
		    "message": "이미 추가된 유형입니다."
		  }
		""";

	public static final String UPDATE_CUSTOM_PROBLEM_TYPE = """
		커스텀 유형을 수정합니다. (이름/정렬 순서)

		입력:
		- PATCH /api/v1/problem-types/{typeId}
		- Path
		  - typeId: 수정할 커스텀 유형 ID
		- Body (둘 중 하나 이상 필요)
		  - name: 새 유형명 (선택)
		  - sortOrder: 정렬 순서 (선택, 1 이상)

		주의:
		- 기본 유형(custom=false)은 수정할 수 없습니다.
		- 본인이 만든 커스텀 유형(custom=true)만 수정할 수 있습니다.

		예시1) 이름만 변경
		- 요청: PATCH /api/v1/problem-types/T_C_7f2c...
		- 요청 바디:
		  {"name":"서술형(개념)"}

		예시2) 순서만 변경
		- 요청 바디:
		  {"sortOrder":3}

		예시3) 둘 다 변경
		- 요청 바디:
		  {"name":"서술형(개념)","sortOrder":3}
		""";

	public static final String SET_CUSTOM_PROBLEM_TYPE_ACTIVE = """
		커스텀 유형을 활성/비활성 처리합니다.

		입력:
		- PATCH /api/v1/problem-types/{typeId}/activation
		- Path
		  - typeId: 대상 커스텀 유형 ID
		- Body
		  - active: true/false

		설명:
		- 비활성(active=false)은 사실상 '삭제'에 해당합니다. (소프트 삭제)
		- 과거에 해당 유형으로 등록된 오답카드/통계는 유지됩니다.

		예시(삭제):
		- 요청 바디:
		  {"active":false}

		예시(복구):
		- 요청 바디:
		  {"active":true}
		""";

	public static final String MONTHLY_PROGRESS = """
		월별 오답 현황(등록/완료)을 조회합니다.

		집계 기준:
		- 해당 월에 '생성된' 오답카드 전체를 대상으로 합니다. (createdAt 기준)
		- totalCount: 해당 월에 등록된 오답카드 총 개수
		- solvedCount: 해당 월 등록 카드 중 '오답 완료(SOLVED)' 상태 개수
		- unsolvedCount: 해당 월 등록 카드 중 '오답 미완료(UNSOLVED)' 상태 개수

		입력(Query):
		- year: 조회할 연도 (4자리, 필수) 예) 2026
		- month: 조회할 월 (1~12, 필수) 예) 1

		응답:
		- yearMonth: 조회한 연월 (YYYY-MM)
		- totalCount, solvedCount, unsolvedCount
		""";
}
