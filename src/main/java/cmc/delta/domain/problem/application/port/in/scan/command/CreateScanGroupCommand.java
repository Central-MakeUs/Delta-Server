package cmc.delta.domain.problem.application.port.in.scan.command;

import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import java.util.List;

public record CreateScanGroupCommand(List<UploadFile> files) {
}
