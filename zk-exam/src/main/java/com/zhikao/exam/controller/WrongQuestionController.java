package com.zhikao.exam.controller;

import com.zhikao.exam.domain.vo.PointStatVO;
import com.zhikao.exam.domain.vo.WrongQuestionVO;
import com.zhikao.exam.service.IWrongQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 错题本接口
 */
@Api(tags = "错题本接口")
@RestController
@RequestMapping("/wrong")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final IWrongQuestionService wrongQuestionService;

    @ApiOperation("我的错题（mastered 不传=全部，0=未掌握，1=已掌握）")
    @GetMapping("/list")
    public List<WrongQuestionVO> list(@RequestParam(value = "mastered", required = false) Integer mastered) {
        return wrongQuestionService.queryMyWrong(mastered);
    }

    @ApiOperation("按考点聚合的错题统计")
    @GetMapping("/points")
    public List<PointStatVO> points() {
        return wrongQuestionService.queryPointStat();
    }

    @ApiOperation("标记已掌握")
    @PostMapping("/{questionId}/mastered")
    public void mastered(@PathVariable("questionId") Long questionId) {
        wrongQuestionService.markMastered(questionId);
    }

    @ApiOperation("移出错题本")
    @DeleteMapping("/{questionId}")
    public void remove(@PathVariable("questionId") Long questionId) {
        wrongQuestionService.remove(questionId);
    }
}
