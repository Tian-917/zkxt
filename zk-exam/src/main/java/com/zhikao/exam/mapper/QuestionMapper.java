package com.zhikao.exam.mapper;

import com.zhikao.api.dto.IdAndNumDTO;
import com.zhikao.exam.domain.po.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 题目 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface QuestionMapper extends BaseMapper<Question> {

    List<IdAndNumDTO> countQuestionOfCreater(@Param("createrIds") List<Long> createrIds);

}
