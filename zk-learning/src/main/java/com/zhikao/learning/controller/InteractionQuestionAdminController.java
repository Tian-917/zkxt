package com.zhikao.learning.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.query.QuestionAdminPageQuery;
import com.zhikao.learning.domain.vo.QuestionAdminVO;
import com.zhikao.learning.service.IInteractionQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService questionService;

    /**
     * 分页查询互动问题
     * @param query
     * @return
     */
    @GetMapping("/page")
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        return questionService.queryQuestionPageAdmin(query);
    }

    /**
     * 根据id查询互动问题
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public QuestionAdminVO queryQuestionAdminById(@PathVariable("id") String id) {
        return questionService.queryQuestionAdminById(id);
    }

    /**
     * 隐藏或显示问题
     * @param id
     * @param hidden
     */
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenOrShowQuestion(@PathVariable("id") Long id, @PathVariable("hidden") boolean hidden) {
        questionService.hiddenOrShowQuestion(id,hidden);
    }
}
