package com.zhikao.user.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.user.domain.dto.StudentFormDTO;
import com.zhikao.user.domain.query.UserPageQuery;
import com.zhikao.user.domain.vo.StudentPageVo;

/**
 * <p>
 * 学员详情表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IStudentService {

    void saveStudent(StudentFormDTO studentFormDTO);

    void updateMyPassword(StudentFormDTO studentFormDTO);

    PageDTO<StudentPageVo> queryStudentPage(UserPageQuery pageQuery);
}
