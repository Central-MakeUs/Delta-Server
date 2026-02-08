package cmc.delta.domain.problem.adapter.in.worker.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cmc.delta.domain.problem.application.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import java.time.LocalDateTime;

public final class WorkerFixtures {

	private WorkerFixtures() {}

	public static User user(long id) {
		User u = mock(User.class);
		when(u.getId()).thenReturn(id);
		return u;
	}

	public static ProblemScan uploaded(User user) {
		return ProblemScan.createUploaded(user);
	}

	public static ProblemScan ocrDone(User user, String text) {
		ProblemScan scan = ProblemScan.createUploaded(user);
		scan.markOcrSucceeded(text, "{}", LocalDateTime.now());
		return scan;
	}

	public static Asset asset(String storageKey) {
		Asset a = mock(Asset.class);
		when(a.getStorageKey()).thenReturn(storageKey);
		return a;
	}

	public static OcrResult ocrResult(String plain, String raw) {
		OcrResult r = mock(OcrResult.class);
		when(r.plainText()).thenReturn(plain);
		when(r.latexStyled()).thenReturn(null);
		when(r.rawJson()).thenReturn(raw);
		return r;
	}

	public static AiCurriculumResult aiResult(String unitId, String typeId, double conf) {
		AiCurriculumResult r = mock(AiCurriculumResult.class);
		when(r.isMathProblem()).thenReturn(true);
		when(r.predictedUnitId()).thenReturn(unitId);
		when(r.predictedTypeId()).thenReturn(typeId);
		when(r.confidence()).thenReturn(conf);
		when(r.unitCandidatesJson()).thenReturn("[]");
		when(r.typeCandidatesJson()).thenReturn("[]");
		when(r.aiDraftJson()).thenReturn("{}");
		return r;
	}
}
