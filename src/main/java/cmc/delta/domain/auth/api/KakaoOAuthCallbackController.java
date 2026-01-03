package cmc.delta.domain.auth.api;

import cmc.delta.global.api.annotation.NoWrap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NoWrap
@RestController
public class KakaoOAuthCallbackController {

    private static final String CALLBACK_PATH = "/callback";

    @GetMapping(CALLBACK_PATH)
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        return ResponseEntity.noContent().build();
    }
}
