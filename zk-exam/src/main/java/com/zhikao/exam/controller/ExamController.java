package com.zhikao.exam.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.exam.domain.dto.ExamCreateDTO;
import com.zhikao.exam.domain.dto.ExamSubmitDTO;
import com.zhikao.exam.domain.vo.ExamPageVO;
import com.zhikao.exam.domain.vo.ExamRecordDetailVO;
import com.zhikao.exam.domain.vo.ExamVO;
import com.zhikao.exam.service.IExamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 考试评测 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Api(tags = "考试评测相关接口")
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final IExamService examService;

    @ApiOperation("创建练习/考试")
    @PostMapping
    public ExamVO createExam(@RequestBody ExamCreateDTO dto) {
        return examService.createExam(dto);
    }

    @ApiOperation("提交试卷并自动阅卷")
    @PostMapping("/details")
    public List<ExamRecordDetailVO> submitExam(@RequestBody ExamSubmitDTO dto) {
        return examService.submitExam(dto);
    }

    @ApiOperation("分页查询我的考试")
    @GetMapping("/page")
    public PageDTO<ExamPageVO> queryMyExams(PageQuery query) {
        return examService.queryMyExams(query);
    }

    @ApiOperation("查询考试详情")
    @GetMapping("/{id}")
    public List<ExamRecordDetailVO> queryExamDetail(
            @ApiParam("考试记录id") @PathVariable("id") String id) {
        return examService.queryExamDetail(id);
    }
}
