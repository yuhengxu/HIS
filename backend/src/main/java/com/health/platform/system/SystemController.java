package com.health.platform.system;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "health-platform-backend",
            "time", OffsetDateTime.now().toString()
        );
    }

    @GetMapping("/modules")
    public List<ModuleInfo> modules() {
        return List.of(
            new ModuleInfo("iam", "用户与权限", "统一账户、角色、权限点、用户单独授权"),
            new ModuleInfo("oa", "OA 基座", "基础 OA 访问权限与后续办公能力入口"),
            new ModuleInfo("inventory", "物资管理", "物资档案、库存、入库、出库、盘点"),
            new ModuleInfo("audit", "审计", "操作日志、权限变更日志、AI 调用审计"),
            new ModuleInfo("ai-access", "AI 接入", "AI 接口白名单与数据库只读边界")
        );
    }

    public record ModuleInfo(String code, String name, String description) {
    }
}
