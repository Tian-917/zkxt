package com.zhikao.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.client.trade.TradeClient;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.enums.UserType;
import com.zhikao.common.utils.BeanUtils;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.RandomUtils;
import com.zhikao.user.constants.UserConstants;
import com.zhikao.user.domain.dto.StudentFormDTO;
import com.zhikao.user.domain.po.User;
import com.zhikao.user.domain.po.UserDetail;
import com.zhikao.user.domain.query.UserPageQuery;
import com.zhikao.user.domain.vo.StudentPageVo;
import com.zhikao.user.service.IStudentService;
import com.zhikao.user.service.IUserDetailService;
import com.zhikao.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 学员详情表 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements IStudentService {

    private final IUserService userService;
    private final IUserDetailService detailService;
    private final TradeClient tradeClient;

    @Override
    @Transactional
    public void saveStudent(StudentFormDTO studentFormDTO) {
        // 1.新增用户账号
        User user = new User();
        user.setCellPhone(studentFormDTO.getCellPhone());
        user.setPassword(studentFormDTO.getPassword());
        user.setType(UserType.STUDENT);
        userService.addUserByPhone(user, studentFormDTO.getCode());

        // 2.新增学员详情
        UserDetail student = new UserDetail();
        student.setId(user.getId());
        student.setName(RandomUtils.randomString(8));
        student.setRoleId(UserConstants.STUDENT_ROLE_ID);
        detailService.save(student);
    }

    @Override
    public void updateMyPassword(StudentFormDTO studentFormDTO) {
        userService.updatePasswordByPhone(
                studentFormDTO.getCellPhone(), studentFormDTO.getCode(), studentFormDTO.getPassword()
        );
    }

    @Override
    public PageDTO<StudentPageVo> queryStudentPage(UserPageQuery query) {
        // 1.分页条件
        Page<UserDetail> page  =  detailService.queryUserDetailByPage(query, UserType.STUDENT);
        List<UserDetail> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        // 2.查询购买的课程数量
        List<Long> stuIds = records.stream().map(UserDetail::getId).collect(Collectors.toList());
        Map<Long, Integer> numMap = tradeClient.countEnrollCourseOfStudent(stuIds);

        // 3.处理vo
        List<StudentPageVo> list = new ArrayList<>(records.size());
        for (UserDetail r : records) {
            StudentPageVo v = BeanUtils.toBean(r, StudentPageVo.class);
            list.add(v);
            v.setCourseAmount(numMap.get(r.getId()));
        }
        return new PageDTO<>(page.getTotal(), page.getPages(), list);
    }
}
