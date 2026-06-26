package com.health.platform.wecom;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

@Service
public class WeComTokenService {
    private final WeComProperties properties;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String cachedToken;
    private OffsetDateTime expiresAt = OffsetDateTime.MIN;

    public WeComTokenService(WeComProperties properties) {
        this.properties = properties;
    }

    public synchronized String accessToken() {
        if (!properties.readyForApi()) return null;
        if (cachedToken != null && expiresAt.isAfter(OffsetDateTime.now().plusMinutes(5))) {
            return cachedToken;
        }
        String url = properties.baseUrl() + "/cgi-bin/gettoken?corpid=" + encode(properties.corpId()) + "&corpsecret=" + encode(properties.secret());
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            String body = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            String token = WeComJson.stringValue(body, "access_token");
            long expiresIn = WeComJson.longValue(body, "expires_in", 7200L);
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("WeCom access_token missing: " + body);
            }
            cachedToken = token;
            expiresAt = OffsetDateTime.now().plusSeconds(Math.max(60, expiresIn));
            return cachedToken;
        } catch (Exception ex) {
            throw new IllegalStateException("Get WeCom access_token failed: " + ex.getMessage(), ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
