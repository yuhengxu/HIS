package com.health.platform.system;

import java.util.List;

import com.health.platform.common.ApiResponse;
import com.health.platform.common.SecurityContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final MenuService menuService;

    public MeController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuStore.MenuTreeNode>> menus(@RequestHeader("X-User-Id") Long actorUserId) {
        return ApiResponse.ok(menuService.currentUserMenus(SecurityContextUtil.requireUserId(actorUserId)));
    }
}
