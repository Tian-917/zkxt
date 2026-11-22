package com.zhikao.exam.service;

import com.zhikao.exam.domain.po.ExamRecord;
import com.zhikao.exam.domain.po.ExamRecordDetail;
import com.zhikao.exam.domain.vo.PointStatVO;
import com.zhikao.exam.domain.vo.WrongQuestionVO;

import java.util.List;

/**
 * 错题本服务
 */
public interface IWrongQuestionService {

    /**
     * 交卷后归档：答错入错题本（累计次数），答对则把历史错题置为已掌握
     *
     * @param record  考试记录
     * @param details 阅卷明细
     */
    void recordFromExam(ExamRecord record, List<ExamRecordDetail> details);

    /**
     * 我的错题
     *
     * @param mastered 是否只看已掌握；null 表示全部
     */
    List<WrongQuestionVO> queryMyWrong(Integer mastered);

    /**
     * 按考点（小节）聚合未掌握错题
     */
    List<PointStatVO> queryPointStat();

    /**
     * 手动标记已掌握
     */
    void markMastered(Long questionId);

    /**
     * 移出错题本
     */
    void remove(Long questionId);
}
