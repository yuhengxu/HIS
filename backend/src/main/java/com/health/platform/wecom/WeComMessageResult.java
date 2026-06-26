package com.health.platform.wecom;

public record WeComMessageResult(String status, String errorMessage) {
    public static WeComMessageResult sent() { return new WeComMessageResult("sent", null); }
    public static WeComMessageResult skipped(String reason) { return new WeComMessageResult("skipped", reason); }
    public static WeComMessageResult failed(String reason) { return new WeComMessageResult("failed", reason); }
}
