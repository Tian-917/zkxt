package com.zhikao.api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.zhikao.api.client.auth.AuthClient;
import com.zhikao.api.dto.auth.RoleDTO;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.common.enums.UserType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleCache {

    private final Cache<Long, RoleDTO> roleCaches;
    private final AuthClient authClient;

    public String getRoleName(Long roleId) {
        RoleDTO roleDTO = roleCaches.get(roleId, authClient::queryRoleById);
        if (roleDTO == null) {
            return null;
        }
        return roleDTO.getName();
    }

    public String exchangeRoleName(UserDTO u) {
        if (u == null) {
            return "--";
        }
        if (UserType.STUDENT.equalsValue(u.getType())) {
            // 学生，直接返回角色名称
            return u.getName();
        } else {
            // 管理员需要拼接角色名称
            return getRoleName(u.getRoleId()) + "-" + u.getName();
        }
    }
}
