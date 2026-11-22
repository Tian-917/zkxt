package com.zhikao.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.auth.domain.po.Role;

import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IRoleService extends IService<Role> {

    boolean exists(Long roleId);
    boolean exists(List<Long> roleIds);

    void deleteRole(Long id);
}
