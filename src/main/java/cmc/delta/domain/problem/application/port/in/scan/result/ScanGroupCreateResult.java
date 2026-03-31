package cmc.delta.domain.problem.application.port.in.scan.result;

import java.util.List;

public record ScanGroupCreateResult(Long scanGroupId, List<ScanCreateResult> scans) {
}
