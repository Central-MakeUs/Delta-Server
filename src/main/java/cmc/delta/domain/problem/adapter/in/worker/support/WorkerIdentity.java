package cmc.delta.domain.problem.adapter.in.worker.support;

public record WorkerIdentity(
	String name,
	String label,
	String backlogKey) {
}
