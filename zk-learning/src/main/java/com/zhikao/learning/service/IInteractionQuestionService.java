package com.zhikao.learning.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.dto.QuestionFormDTO;
import com.zhikao.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.learning.domain.query.QuestionAdminPageQuery;
import com.zhikao.learning.domain.query.QuestionPageQuery;
import com.zhikao.learning.domain.vo.QuestionAdminVO;
import com.zhikao.learning.domain.vo.QuestionVO;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    void saveQuestion(@Valid QuestionFormDTO questionDTO);

    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    QuestionVO queryQuestionById(Long id);

    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

    QuestionAdminVO queryQuestionAdminById(String id);

    void hiddenOrShowQuestion(Long id, boolean hidden);

    void updateQuestion(Long id,QuestionFormDTO questionDTO);

    void deleteQuestion(Long id);
}
