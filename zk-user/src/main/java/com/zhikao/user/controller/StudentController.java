package com.zhikao.user.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.user.domain.dto.StudentFormDTO;
import com.zhikao.user.domain.query.UserPageQuery;
import com.zhikao.user.domain.vo.StudentPageVo;
import com.zhikao.user.service.IStudentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学员详情表 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/students")
@Api(tags = "用户管理接口")
public class StudentController {

    @Autowired
    private IStudentService studentService;

    @ApiOperation("分页查询学生信息")
    @GetMapping("/page")
    public PageDTO<StudentPageVo> queryStudentPage(UserPageQuery pageQuery){
        return studentService.queryStudentPage(pageQuery);
    }

    @ApiOperation("学员注册")
    @PostMapping("/register")
    public void registerStudent(@RequestBody StudentFormDTO studentFormDTO) {
        studentService.saveStudent(studentFormDTO);
    }

    @ApiOperation("修改学员密码")
    @PutMapping("/password")
    public void updateMyPassword(@RequestBody StudentFormDTO studentFormDTO) {
        studentService.updateMyPassword(studentFormDTO);
    }
}
