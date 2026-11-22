package com.zhikao.learning.controller;


import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.dto.QuestionFormDTO;
import com.zhikao.learning.domain.query.QuestionPageQuery;
import com.zhikao.learning.domain.vo.QuestionVO;
import com.zhikao.learning.service.IInteractionQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    /**
     * 新增互动问题
     * @param questionDTO
     */
    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO questionDTO) {
        questionService.saveQuestion(questionDTO);
    }

    /**
     * 分页查询互动问题
     * @param query
     * @return
     */
    @GetMapping("/page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        return questionService.queryQuestionPage(query);
    }

    /**
     * 根据id查询互动问题
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@PathVariable("id") Long id) {
        return questionService.queryQuestionById(id);
    }

    /**
     * 修改提问
     * @param id
     * @param questionDTO
     */
    @PutMapping("/{id}")
    public void updateQuestion(@PathVariable("id") Long id, @Valid @RequestBody QuestionFormDTO questionDTO) {
        questionService.updateQuestion(id,questionDTO);
    }

    /**
     * 根据id删除当前用户问题
     * @param id
     */
    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable("id") Long id) {
        questionService.deleteQuestion(id);
    }
}
