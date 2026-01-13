// package cmc.delta.domain.problem.api;
//
// import cmc.delta.domain.problem.api.dto.response.ProblemScanCreateResponse;
// import cmc.delta.global.config.security.principal.CurrentUser;
// import cmc.delta.global.config.security.principal.UserPrincipal;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.MediaType;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;
//
// @RestController
// @RequiredArgsConstructor
// @RequestMapping("/api/v1/problem-scans")
// public class ProblemScanController {
//
// 	private final ProblemScanCreateService createService;
//
// 	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
// 	public ProblemScanCreateResponse create(
// 		@CurrentUser UserPrincipal me,
// 		@RequestPart("file") MultipartFile file
// 	) {
// 		return createService.createScan(me.userId(), file);
// 	}
// }
