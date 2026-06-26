package com.health.platform.wecom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WeComProperties {
    @Value("${wecom.enabled:false}")
    private boolean enabled;
    @Value("${wecom.corp-id:}")
    private String corpId;
    @Value("${wecom.agent-id:0}")
    private long agentId;
    @Value("${wecom.secret:}")
    private String secret;
    @Value("${wecom.base-url:https://qyapi.weixin.qq.com}")
    private String baseUrl;
    @Value("${wecom.his-public-base-url:}")
    private String hisPublicBaseUrl;

    public boolean enabled() { return enabled; }
    public String corpId() { return blankToNull(corpId); }
    public long agentId() { return agentId; }
    public String secret() { return blankToNull(secret); }
    public String baseUrl() { return baseUrl == null || baseUrl.isBlank() ? "https://qyapi.weixin.qq.com" : baseUrl; }
    public String hisPublicBaseUrl() { return blankToNull(hisPublicBaseUrl); }

    public boolean readyForApi() {
        return enabled && corpId() != null && secret() != null && agentId > 0;
    }

    public String mobileBaseUrl() {
        String base = hisPublicBaseUrl();
        return base == null ? "" : base.replaceAll("/+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
