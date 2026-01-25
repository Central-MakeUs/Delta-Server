package cmc.delta.domain.problem.application.port.in.scan.command;

import cmc.delta.domain.problem.application.port.in.support.UploadFile;

public record CreateScanCommand(UploadFile file) {
}
