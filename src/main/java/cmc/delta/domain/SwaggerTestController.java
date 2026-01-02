/**
 * 이 Controller는 단순한 테스트용
 */
package cmc.delta.domain;

import cmc.delta.global.api.response.ApiResponse;
import cmc.delta.global.api.response.ApiResponses;
import cmc.delta.global.config.swagger.ApiErrorCodeExamples;
import cmc.delta.global.error.ErrorCode;
import cmc.delta.global.error.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Swagger Test", description = "SpringDoc/Swagger 동작 확인용 API")
@RestController
@RequestMapping("/api/v1/swagger-test")
public class SwaggerTestController {

    @Operation(summary = "Ping", description = "Swagger 기본 동작 + ApiResponse 스키마 노출 확인")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> data = Map.of(
                "pong", true,
                "now", Instant.now().toString()
        );
        return ApiResponses.success(200, data);
    }

    @Operation(summary = "BusinessException 테스트", description = "GlobalExceptionHandler + 에러 응답 예시 확인")
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_REQUEST,
            ErrorCode.AUTHENTICATION_FAILED,
            ErrorCode.INTERNAL_ERROR
    })
    @GetMapping("/force-error")
    public ApiResponse<Object> forceError(@RequestParam(defaultValue = "INVALID") String type) {
        if ("AUTH".equalsIgnoreCase(type)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED);
        }
        if ("500".equalsIgnoreCase(type)) {
            throw new RuntimeException("swagger test 500");
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "swagger test 400");
    }

    @Operation(summary = "타입 미스매치 테스트", description = "PathVariable 타입 변환 실패(400) 케이스 확인")
    @ApiErrorCodeExamples({
            ErrorCode.INVALID_REQUEST
    })
    @GetMapping("/users/{userId}")
    public ApiResponse<Map<String, Object>> getUser(@PathVariable Long userId) {
        Map<String, Object> data = Map.of("userId", userId);
        return ApiResponses.success(200, data);
    }
}

