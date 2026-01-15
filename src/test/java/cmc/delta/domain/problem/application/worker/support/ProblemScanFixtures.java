package cmc.delta.domain.problem.application.worker.support;

import static org.mockito.Mockito.*;

import cmc.delta.domain.curriculum.model.ProblemType;
import cmc.delta.domain.curriculum.model.Unit;
import cmc.delta.domain.problem.application.scan.port.out.ai.dto.AiCurriculumResult;
import cmc.delta.domain.problem.application.scan.port.out.ocr.dto.OcrResult;
import cmc.delta.domain.problem.model.asset.Asset;
import cmc.delta.domain.problem.model.scan.ProblemScan;
import cmc.delta.domain.user.model.User;
import java.time.LocalDateTime;

public final class ProblemScanFixtures {

	private ProblemScanFixtures() {}

	public static User user(long id) {
		User u = mock(User.class);
		when(u.getId()).thenReturn(id);
		return u;
	}

	public static ProblemScan uploaded(User user) {
		return ProblemScan.createUploaded(user);
	}

	public static ProblemScan ocrDone(User user, String text) {
		ProblemScan s = ProblemScan.createUploaded(user);
		s.markOcrSucceeded(text, "{}", LocalDateTime.now());
		return s;
	}

	public static Asset originalAsset(String key) {
		Asset a = mock(Asset.class);
		when(a.getStorageKey()).thenReturn(key);
		return a;
	}

	public static OcrResult ocrResult(String plain, String raw) {
		OcrResult r = mock(OcrResult.class);
		when(r.plainText()).thenReturn(plain);
		when(r.rawJson()).thenReturn(raw);
		return r;
	}

	public static AiCurriculumResult aiResult(String unitId, String typeId, double conf) {
		AiCurriculumResult r = mock(AiCurriculumResult.class);
		when(r.predictedUnitId()).thenReturn(unitId);
		when(r.predictedTypeId()).thenReturn(typeId);
		when(r.confidence()).thenReturn(conf);
		when(r.unitCandidatesJson()).thenReturn("[]");
		when(r.typeCandidatesJson()).thenReturn("[]");
		when(r.aiDraftJson()).thenReturn("{}");
		return r;
	}

	public static Unit unit(String id) {
		Unit u = mock(Unit.class);
		when(u.getId()).thenReturn(id);
		return u;
	}

	public static ProblemType type(String id) {
		ProblemType t = mock(ProblemType.class);
		when(t.getId()).thenReturn(id);
		return t;
	}
}
