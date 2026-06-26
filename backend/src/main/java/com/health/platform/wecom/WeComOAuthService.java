package com.health.platform.wecom;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.health.platform.common.BusinessException;
import com.health.platform.common.ErrorCode;
import com.health.platform.iam.IamStore;
import com.health.platform.iam.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class WeComOAuthService {
    private final WeComProperties properties;
    private final WeComTokenService tokenService;
    private final WeComSessionService sessionService;
    private final IamStore iamStore;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WeComOAuthService(WeComProperties properties, WeComTokenService tokenService, WeComSessionService sessionService, IamStore iamStore) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.iamStore = iamStore;
    }

    public String authUrl(String redirect) {
        String target = redirect == null || redirect.isBlank() ? "/m/oa" : redirect;
        String callback = properties.mobileBaseUrl() + "/m/oa/login?redirect=" + encode(target);
        if (properties.corpId() == null) return callback;
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + encode(properties.corpId())
            + "&redirect_uri=" + encode(callback)
            + "&response_type=code&scope=snsapi_base&state=his-oa#wechat_redirect";
    }

    public MobileLoginResult login(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Missing WeCom OAuth code");
        }
        String wecomUserId = resolveWeComUserId(code);
        UserRecord user = iamStore.findUserByWecomUserId(wecomUserId)
            .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "当前企业微信账号未绑定 HIS 用户"));
        WeComSessionRecord session = sessionService.createSession(user, wecomUserId);
        return new MobileLoginResult(session.token(), user.id(), user.username(), user.displayName(), user.roleCodes().stream().toList(), wecomUserId);
    }

    private String resolveWeComUserId(String code) {
        if (code.startsWith("mock:")) {
            return code.substring("mock:".length());
        }
        String accessToken = tokenService.accessToken();
        if (accessToken == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "企业微信未配置，无法使用真实 OAuth code 登录");
        }
        String url = properties.baseUrl() + "/cgi-bin/user/getuserinfo?access_token=" + encode(accessToken) + "&code=" + encode(code);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            String userId = WeComJson.stringValue(body, "UserId");
            if (userId == null || userId.isBlank()) {
                throw new IllegalStateException("WeCom UserId missing: " + body);
            }
            return userId;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "企业微信 OAuth 登录失败: " + ex.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public record MobileLoginResult(String token, long userId, String username, String displayName, java.util.List<String> roleCodes, String wecomUserId) {}
}
