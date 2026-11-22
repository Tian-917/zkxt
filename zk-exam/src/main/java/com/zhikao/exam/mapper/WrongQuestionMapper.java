package com.zhikao.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhikao.exam.domain.po.WrongQuestion;
import com.zhikao.exam.domain.vo.PointStatVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 错题本 Mapper
 */
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    /**
     * 记一次错：一题一条，重复答错累加次数并把「已掌握」打回未掌握
     */
    @Insert("INSERT INTO wrong_question " +
            "(user_id, question_id, course_id, section_id, section_name, last_answer, wrong_count, last_wrong_time, mastered) " +
            "VALUES (#{userId}, #{questionId}, #{courseId}, #{sectionId}, #{sectionName}, #{lastAnswer}, 1, NOW(), 0) " +
            "ON DUPLICATE KEY UPDATE wrong_count = wrong_count + 1, last_wrong_time = NOW(), " +
            "last_answer = VALUES(last_answer), section_name = VALUES(section_name), mastered = 0")
    int upsertWrong(WrongQuestion wrong);

    /**
     * 重练答对：置为已掌握（没有记录时影响 0 行，无副作用）
     */
    @Update("UPDATE wrong_question SET mastered = 1 WHERE user_id = #{userId} AND question_id = #{questionId}")
    int markMastered(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 按考点（小节）聚合未掌握错题
     */
    @Select("SELECT section_id AS sectionId, section_name AS sectionName, " +
            "COUNT(*) AS questionCount, COALESCE(SUM(wrong_count), 0) AS wrongTimes " +
            "FROM wrong_question WHERE user_id = #{userId} AND mastered = 0 " +
            "GROUP BY section_id, section_name ORDER BY questionCount DESC")
    List<PointStatVO> statByPoint(@Param("userId") Long userId);
}
