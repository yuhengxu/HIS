package com.health.platform.wecom;

import java.util.Map;

import com.health.platform.common.ApiResponse;
import com.health.platform.iam.UserRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wecom/auth")
public class WeComAuthController {
    private final WeComOAuthService oauthService;
    private final WeComSessionService sessionService;

    public WeComAuthController(WeComOAuthService oauthService, WeComSessionService sessionService) {
        this.oauthService = oauthService;
        this.sessionService = sessionService;
    }

    @GetMapping("/url")
    public ApiResponse<Map<String, String>> authUrl(@RequestParam(required = false) String redirect) {
        return ApiResponse.ok(Map.of("url", oauthService.authUrl(redirect)));
    }

    @PostMapping("/login")
    public ApiResponse<WeComOAuthService.MobileLoginResult> login(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(oauthService.login(body == null ? null : body.get("code")));
    }

    @GetMapping("/me")
    public ApiResponse<UserRecord> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(sessionService.currentUser(authorization).orElse(null));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sessionService.logout(authorization);
        return ApiResponse.ok(null);
    }
}
