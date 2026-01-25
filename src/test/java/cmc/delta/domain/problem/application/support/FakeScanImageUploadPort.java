package cmc.delta.domain.problem.application.support;

import cmc.delta.domain.problem.application.port.in.support.UploadFile;
import cmc.delta.domain.problem.application.port.out.storage.ScanImageUploadPort;

public final class FakeScanImageUploadPort implements ScanImageUploadPort {

	public String lastUploadDirectory;
	public UploadFile lastUploadedFile;

	public UploadResult nextUploadResult = new UploadResult("s3/key.png", 100, 200);

	@Override
	public UploadResult uploadImage(UploadFile file, String directory) {
		this.lastUploadedFile = file;
		this.lastUploadDirectory = directory;
		return nextUploadResult;
	}
}
