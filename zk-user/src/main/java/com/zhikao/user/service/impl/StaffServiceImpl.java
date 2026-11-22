package com.zhikao.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.cache.RoleCache;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.enums.UserType;
import com.zhikao.common.utils.BeanUtils;
import com.zhikao.user.domain.po.UserDetail;
import com.zhikao.user.domain.query.UserPageQuery;
import com.zhikao.user.domain.vo.StaffVO;
import com.zhikao.user.service.IStaffService;
import com.zhikao.user.service.IUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 员工详情表 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements IStaffService {

    private final IUserDetailService detailService;
    private final RoleCache roleCache;
    @Override
    public PageDTO<StaffVO> queryStaffPage(UserPageQuery query) {
        // 1.搜索
        Page<UserDetail> p = detailService.queryUserDetailByPage(query, UserType.STAFF);
        // 2.处理vo
        return PageDTO.of(p, u -> {
            StaffVO v = BeanUtils.toBean(u, StaffVO.class);
            v.setRoleName(roleCache.getRoleName(u.getRoleId()));
            return v;
        });
    }
}
