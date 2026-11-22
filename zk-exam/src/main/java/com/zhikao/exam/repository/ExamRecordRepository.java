package com.zhikao.exam.repository;

import com.zhikao.exam.domain.po.ExamRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 考试记录仓储
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ExamRecordRepository extends MongoRepository<ExamRecord, String> {

    /**
     * 分页查询用户的指定状态考试记录
     */
    Page<ExamRecord> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    /**
     * 根据id和用户id查询考试记录
     */
    Optional<ExamRecord> findByIdAndUserId(String id, Long userId);

    /**
     * 统计用户指定小节、类型的考试记录数量
     */
    long countByUserIdAndSectionIdAndType(Long userId, Long sectionId, Integer type);
}
