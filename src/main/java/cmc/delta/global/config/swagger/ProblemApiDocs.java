package cmc.delta.global.config.swagger;

public final class ProblemApiDocs {

	private ProblemApiDocs() {
	}

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
		
		상태(status): '오답 완료 버튼' 기준
		- ALL: 전체(기본)
		- UNSOLVED: 오답 완료 전
		- SOLVED: 오답 완료
		
		주의:
		- 필터 미적용은 파라미터를 생략하거나 빈값으로 보내면 됩니다.
		- Swagger 기본 예시값 "string"은 실제 필터로 동작하므로, 필터를 끄려면 입력값을 지우거나 빈값으로 보내세요.
		""";

	public static final String COMPLETE_WRONG_ANSWER_CARD = """
		오답카드를 '오답 완료(SOLVED)' 상태로 전환합니다.
		
		상태 정책:
		- status(SOLVED/UNSOLVED)는 풀이/정답 입력 여부가 아니라, 이 API 호출 여부(완료 상태값) 기준입니다.
		- 이미 완료된 오답카드에 재호출해도 멱등으로 동작하도록(변화 없음) 처리하는 것을 권장합니다.
		""";
}
