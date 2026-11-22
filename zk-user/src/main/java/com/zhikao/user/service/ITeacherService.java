package com.zhikao.user.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.user.domain.query.UserPageQuery;
import com.zhikao.user.domain.vo.TeacherPageVO;

/**
 * <p>
 * 教师详情表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ITeacherService{
    PageDTO<TeacherPageVO> queryTeacherPage(UserPageQuery pageQuery);

}
