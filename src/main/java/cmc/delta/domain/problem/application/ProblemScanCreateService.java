// // src/main/java/cmc/delta/domain/problem/application/ProblemScanCreateService.java
// package cmc.delta.domain.problem.application;
//
// import cmc.delta.domain.problem.api.dto.response.ProblemScanCreateResponse;
// import cmc.delta.domain.problem.model.Asset;
// import cmc.delta.domain.problem.model.ProblemScan;
// import cmc.delta.domain.user.model.User;
// import cmc.delta.domain.user.persistence.UserJpaRepository;
// import cmc.delta.global.api.storage.dto.StorageUploadData;
// import cmc.delta.global.storage.StorageService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.multipart.MultipartFile;
//
// @Service
// @RequiredArgsConstructor
// public class ProblemScanCreateService {
//
// 	private static final String ORIGINAL_DIR = "problem-scan/original";
//
// 	private final StorageService storageService;
// 	private final UserJpaRepository userRepository;
//
// 	private final ProblemScanRepository problemScanRepository;
// 	private final AssetRepository assetRepository;
//
// 	@Transactional
// 	public ProblemScanCreateResponse createScan(Long userId, MultipartFile file) {
// 		StorageUploadData uploaded = storageService.uploadImage(file, ORIGINAL_DIR);
//
// 		User userRef = userRepository.getReferenceById(userId);
//
// 		ProblemScan scan = problemScanRepository.save(ProblemScan.createUploaded(userRef));
//
// 		Asset asset = assetRepository.save(Asset.createOriginal(
// 			scan,
// 			uploaded.storageKey(),
// 			uploaded.width(),
// 			uploaded.height()
// 		));
//
// 		return new ProblemScanCreateResponse(scan.getId(), asset.getId(), scan.getStatus().name());
// 	}
// }
