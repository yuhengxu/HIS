package com.health.platform.oa;

import com.health.platform.iam.UserService;
import org.springframework.stereotype.Service;

@Service
public class SupervisorResolver {
    private final UserService userService;

    public SupervisorResolver(UserService userService) {
        this.userService = userService;
    }

    public Long resolveDirectSupervisor(long initiatorUserId) {
        return userService.mustUser(initiatorUserId).reportToUserId();
    }
}
