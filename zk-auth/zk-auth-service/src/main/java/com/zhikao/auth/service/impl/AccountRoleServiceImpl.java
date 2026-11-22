package com.zhikao.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.auth.domain.po.AccountRole;
import com.zhikao.auth.mapper.AccountRoleMapper;
import com.zhikao.auth.service.IAccountRoleService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账户、角色关联表 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
public class AccountRoleServiceImpl extends ServiceImpl<AccountRoleMapper, AccountRole> implements IAccountRoleService {

}
