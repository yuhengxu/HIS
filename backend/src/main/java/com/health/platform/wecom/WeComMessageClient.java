package com.health.platform.wecom;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class WeComMessageClient {
    private final WeComProperties properties;
    private final WeComTokenService tokenService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WeComMessageClient(WeComProperties properties, WeComTokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    public WeComMessageResult sendTextCard(List<String> wecomUserIds, String title, String description, String url, String buttonText) {
        List<String> recipients = wecomUserIds == null ? List.of() : wecomUserIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (recipients.isEmpty()) return WeComMessageResult.skipped("no bound wecom user");
        if (!properties.readyForApi()) return WeComMessageResult.skipped("wecom disabled or not configured");
        String token;
        try {
            token = tokenService.accessToken();
        } catch (RuntimeException ex) {
            return WeComMessageResult.failed(ex.getMessage());
        }
        String body = "{"
            + "\"touser\":\"" + WeComJson.escape(String.join("|", recipients)) + "\","
            + "\"msgtype\":\"textcard\","
            + "\"agentid\":" + properties.agentId() + ","
            + "\"textcard\":{"
            + "\"title\":\"" + WeComJson.escape(title) + "\","
            + "\"description\":\"" + WeComJson.escape(description) + "\","
            + "\"url\":\"" + WeComJson.escape(url) + "\","
            + "\"btntxt\":\"" + WeComJson.escape(buttonText == null ? "查看" : buttonText) + "\"}"
            + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/cgi-bin/message/send?access_token=" + token))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
            String response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            long errcode = WeComJson.longValue(response, "errcode", -1L);
            if (errcode == 0) return WeComMessageResult.sent();
            return WeComMessageResult.failed(response);
        } catch (Exception ex) {
            return WeComMessageResult.failed(ex.getMessage());
        }
    }

    public String mobileTaskUrl(long taskId) {
        String base = properties.mobileBaseUrl();
        return (base.isBlank() ? "" : base) + "/m/oa/tasks/" + taskId;
    }
}
