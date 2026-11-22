package com.zhikao.exam.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.exam.domain.dto.ExamCreateDTO;
import com.zhikao.exam.domain.dto.ExamSubmitDTO;
import com.zhikao.exam.domain.vo.ExamPageVO;
import com.zhikao.exam.domain.vo.ExamRecordDetailVO;
import com.zhikao.exam.domain.vo.ExamVO;

import java.util.List;

/**
 * 考试评测服务类
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IExamService {

    /**
     * 创建练习/考试
     * @param dto 创建表单
     * @return 考试记录id与试卷题目
     */
    ExamVO createExam(ExamCreateDTO dto);

    /**
     * 提交试卷并自动阅卷
     * @param dto 提交表单
     * @return 阅卷后的答题明细
     */
    List<ExamRecordDetailVO> submitExam(ExamSubmitDTO dto);

    /**
     * 分页查询当前用户已提交的考试记录
     * @param query 分页参数
     * @return 考试记录分页数据
     */
    PageDTO<ExamPageVO> queryMyExams(PageQuery query);

    /**
     * 查询考试详情
     * @param id 考试记录id
     * @return 答题明细
     */
    List<ExamRecordDetailVO> queryExamDetail(String id);
}
