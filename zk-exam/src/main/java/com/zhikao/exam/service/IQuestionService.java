package com.zhikao.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.api.dto.exam.QuestionDTO;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.exam.domain.dto.QuestionFormDTO;
import com.zhikao.exam.domain.po.Question;
import com.zhikao.exam.domain.query.QuestionPageQuery;
import com.zhikao.exam.domain.vo.QuestionDetailVO;
import com.zhikao.exam.domain.vo.QuestionPageVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 题目 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IQuestionService extends IService<Question> {

    void addQuestion(QuestionFormDTO questionFormDTO);

    void updateQuestion(QuestionFormDTO questionDTO);

    void deleteQuestionById(Long id);

    PageDTO<QuestionPageVO> queryQuestionByPage(QuestionPageQuery query);

    QuestionDetailVO queryQuestionDetailById(Long id);

    List<QuestionDTO> queryQuestionByIds(List<Long> ids);

    Map<Long, Integer> countQuestionNumOfCreater(List<Long> createrIds);

    List<QuestionDTO> queryQuestionByBizId(Long bizId);

    Boolean checkNameValid(String name);

    Map<Long, Integer> queryQuestionScores(List<Long> ids);

    /**
     * 更新题目答题统计：答题次数+1，答对时正确次数+1
     * @param questionCorrectMap 题目id与是否答对的映射
     */
    void updateQuestionStatistics(Map<Long, Boolean> questionCorrectMap);
}
