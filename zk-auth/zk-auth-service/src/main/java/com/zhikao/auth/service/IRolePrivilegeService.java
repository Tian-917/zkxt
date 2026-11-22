package com.zhikao.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.auth.domain.po.RolePrivilege;

import java.util.List;

/**
 * <p>
 * 账户、角色关联表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IRolePrivilegeService extends IService<RolePrivilege> {

    void removeByPrivilegeId(Long id);

    void removeByRoleId(Long id);

    void deleteRolePrivileges(Long roleId, List<Long> privilegeIds);
}
