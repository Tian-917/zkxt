package com.zhikao.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.user.domain.po.UserDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 教师详情表 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface UserDetailMapper extends BaseMapper<UserDetail> {

    UserDetail queryById(Long userId);

    List<UserDetail> queryByIds(List<Long> ids);

    Page<UserDetail> queryByPage(Page<UserDetail> p, @Param("ew") QueryWrapper<UserDetail> wrapper);
}
