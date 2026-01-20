package cmc.delta.domain.problem.application.port.in.scan.command;

import org.springframework.web.multipart.MultipartFile;

public record CreateScanCommand(MultipartFile file) {}
