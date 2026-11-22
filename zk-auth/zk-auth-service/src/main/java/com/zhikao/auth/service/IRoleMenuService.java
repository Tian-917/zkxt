package com.zhikao.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.auth.domain.po.RoleMenu;

import java.util.List;

/**
 * <p>
 * 账户、角色关联表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IRoleMenuService extends IService<RoleMenu> {

    void removeByRoleId(Long id);

    void deleteRoleMenus(Long roleId, List<Long> menuIds);
}
