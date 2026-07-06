package cmc.delta.domain.report.application.port.out;

import cmc.delta.domain.report.application.dto.ReportSourceSignature;
import cmc.delta.domain.report.application.dto.WrongAnswerSample;
import java.util.List;

/**
 * 리포트가 읽어야 하는 오답 원천 데이터(읽기 전용 뷰). problem 애그리거트를 리포트 관점에서 조회한다.
 */
public interface ReportSourcePort {

	ReportSourceSignature loadSignature(Long userId);

	List<WrongAnswerSample> loadSamples(Long userId, int limit);
}
