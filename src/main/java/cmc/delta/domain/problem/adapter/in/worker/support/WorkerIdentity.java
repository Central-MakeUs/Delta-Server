package cmc.delta.domain.problem.adapter.in.worker.support;

public record WorkerIdentity(
	String name, // "ai" / "ocr"  (MDC worker 값)
	String label, // "AI" / "OCR"  (로그용)
	String backlogKey
) {
}
