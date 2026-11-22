package com.zhikao.learning.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.client.course.CatalogueClient;
import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.dto.IdAndNumDTO;
import com.zhikao.api.dto.course.CataSimpleInfoDTO;
import com.zhikao.api.dto.course.CourseFullInfoDTO;
import com.zhikao.api.dto.course.CourseSearchDTO;
import com.zhikao.api.dto.course.CourseSimpleInfoDTO;
import com.zhikao.common.constants.Constant;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.utils.*;
import com.zhikao.learning.domain.dto.LearningPlanDTO;
import com.zhikao.learning.domain.po.LearningLesson;
import com.zhikao.learning.domain.po.LearningRecord;
import com.zhikao.learning.domain.vo.LearningLessonVO;
import com.zhikao.learning.domain.vo.LearningPlanPageVO;
import com.zhikao.learning.domain.vo.LearningPlanVO;
import com.zhikao.learning.enums.LessonStatus;
import com.zhikao.learning.enums.PlanStatus;
import com.zhikao.learning.mapper.LearningLessonMapper;
import com.zhikao.learning.mapper.LearningRecordMapper;
import com.zhikao.learning.service.ILearningLessonService;
import com.zhikao.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final LearningRecordMapper recordMapper;

    private final IPointsRecordService pointsRecordService;

    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        // 1.查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if(CollUtil.isEmpty(cInfoList)){
            //课程不存在，无法添加
            log.error("课程信息不存在，无法添加到课表");
            return;
        }
        // 2.循环遍历，处理LearningLesson数据
        List<LearningLesson> list = new ArrayList<>(cInfoList.size());
        LocalDateTime now = LocalDateTime.now();
        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            LearningLesson lesson = new LearningLesson();
            // 2.1统一设置有效期为2030年过期
            lesson.setCreateTime(now);
            lesson.setExpireTime(Constant.UNIFIED_EXPIRE_TIME);
           // 2.2填充userId和courseId
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);
        }
        // 3.批量新增
        saveBatch(list);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.分页查询
        // select * from learning_lesson where user_id = #{userId} order by latest_learn_time limit 0, 5
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId) // where user_id = #{userId}
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if(CollUtil.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.查询课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        // 4.封装VO返回
        List<LearningLessonVO> voList = new ArrayList<>(records.size());
        // 4.1循环遍历，把LearningLesson转为VO
        for (LearningLesson record : records) {
            // 4.2拷贝基础属性到vo
            LearningLessonVO vo = BeanUtils.copyBean(record, LearningLessonVO.class);
            // 4.3获取课程信息，填充到vo
            CourseSimpleInfoDTO cInfo = cMap.get(record.getCourseId());
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            voList.add(vo);
        }
//        return new PageDTO<>(page.getTotal(),page.getPages(),voList);
        return PageDTO.of(page, voList);
    }

    private @NonNull Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        // 3.1获取课程id
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        // 3.2查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if(CollUtil.isEmpty(cInfoList)){
            //课程不存在，无法添加
            throw new BadRequestException("课程信息不存在！");
        }
        // 3.3把课程集合处理成Map,key是courseId,值是course本身
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cMap;
    }

    @Override
    public LearningLessonVO queryNowLesson() {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        // 2.查询课程信息
        // 2.1查询最近一次课程
        // select * from learning_lesson where user_id = #{userId} and status = 1 order by latest_learn_time desc limit 1
        LearningLesson one = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus,  LessonStatus.LEARNING)
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        //若无最近学习课程，直接返回空
        if(one == null){
            log.debug("无最近学习课程");
            return null;
        }
        // 2.2统计课表中的课程数量
        Integer count = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        // 3.封装VO返回
        // 3.1根据课程id获取课程详细信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(one.getCourseId(), false, false);
        LearningLessonVO vo = BeanUtils.copyBean(one, LearningLessonVO.class);
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum()); // 补上
        vo.setCourseAmount(count);
        // 3.2获取最近一次学习的小节名称和序号
        if(one.getLatestSectionId() != null) {
            CataSimpleInfoDTO section = CollUtil.getFirst(
                    catalogueClient.batchQueryCatalogue(List.of(one.getLatestSectionId())));
            if (section != null) {
                vo.setLatestSectionName(section.getName());
                vo.setLatestSectionIndex(section.getCIndex());
            }
        }
        return vo;
    }

    @Override
    public LearningLessonVO queryLessonById(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询课表记录
        // select * from learning_lesson where user_id = #{userId} and course_id = #{courseId}
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .last("limit 1")
                .one();
        // 2.1课表中没有记录，说明用户未购买该课程，返回null由前端展示购买入口
        if (lesson == null) {
            return null;
        }
        // 3.把课表信息拷贝到VO
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        // 4.查询课程基本信息，补全课程名称、封面、章节数
        CourseSearchDTO cInfo = courseClient.getSearchInfo(courseId);
        if (cInfo == null) {
            log.error("课程信息不存在，courseId=" + courseId);
            throw new BadRequestException("课程信息不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSections());
        return vo;
    }


    @Override
    @Transactional
    public void deleteLessonById(Long courseId) {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.删除指定课程
        // delect from learning_lesson where user_id = #{userId} and course_id = #{courseId}
        boolean removed = lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .remove();

    }

    @Override
    public void deleteLessonByUserIdAndCourseId(Long userId, Long courseId) {
        // delect from learning_lesson where user_id = #{userId} and course_id = #{courseId}
        lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .remove();
    }

    @Override
    public Long isLessonValid(Long courseId) {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.查询课程
        // select * from learning_lesson where user_id = #{userId} ,course_id = #{courseId}
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .last("limit 1")
                .one();
        if(lesson == null){
            return null;
        }
        // 3.无效课程：1.课程状态为过期课程status = 3 2.课程已过过期时间 expireTime != null && expireTime.isBefore(LocalDateTime.now())
        boolean expired = lesson.getStatus() == LessonStatus.EXPIRED || (lesson.getExpireTime() != null && lesson.getExpireTime().isBefore(LocalDateTime.now()));
        return expired ? null : lesson.getId();
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        // select count(*) from learning_lesson where course_id = #{courseId}
        Integer count = lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
        return count;
    }

    @Override
    public LearningLesson queryByUserIdAndCourseId(Long userId, Long courseId) {
        // select * from learning_lesson where user_id = #{userId} and course_id = #{courseId}
       return lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .last("limit 1")
                .one();
    }

    @Override
    public void createLearningPlan(LearningPlanDTO planDTO) {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        // 2.查询课表中的指定课程有关的数据
        LearningLesson lesson = queryByUserIdAndCourseId(userId, planDTO.getCourseId());
//        if (lesson == null) {
//            throw new BadRequestException("课程信息不存在！");
//        }
        AssertUtils.isNotNull(lesson,"课程信息不存在！");
        // 3.修改数据
        LearningLesson l = new LearningLesson();
        l.setId(lesson.getId());
        l.setWeekFreq(lesson.getWeekFreq());
        if(lesson.getPlanStatus() == PlanStatus.NO_PLAN){
            l.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(l);
    }

    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        LearningPlanPageVO result = new LearningPlanPageVO();
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.获取本周起始时间
        LocalDate now = LocalDate.now();
        LocalDateTime begin = DateUtils.getWeekBeginTime(now);
        LocalDateTime end = DateUtils.getWeekEndTime(now);
        // 3.查询总的统计数据
        // 3.1本周总的已学习小节数量
        Integer weekFinished = recordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
        );
        result.setWeekFinished(weekFinished);
        // 3.2本周总的计划学习小节数量
        Integer weekTotalPlan = getBaseMapper().queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);
        // 3.3本周学习积分（统计本周所有类型的积分）
        Integer weekPoints = pointsRecordService.queryUserPointsByTypeAndDate(userId, null, begin, end);
        result.setWeekPoints(weekPoints);

        // 4.查询分页数据
        // 4.1分页查询课程表信息以及学习计划信息
        Page<LearningLesson> p = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = p.getRecords();
        if(CollUtils.isEmpty(records)){
            return result.pageInfo(PageDTO.empty(p));
        }
        // 4.2查询课表对应的课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        // 4.3统计每一个课程本周已学习小节数量
        List<IdAndNumDTO> list = recordMapper.countLearnedSections(userId,begin,end);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(list);
        // 4.4组装数据VO
        List<LearningPlanVO> voList = new ArrayList<>(records.size());
        for (LearningLesson r : records) {
            // 4.4.1拷贝基础属性到vo
            LearningPlanVO vo = BeanUtils.copyBean(r, LearningPlanVO.class);
            // 4.4.2填充课程详细信息
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            if(cInfo != null){
                vo.setCourseName(cInfo.getName());
                vo.setSections(cInfo.getSectionNum());
            }
            // 4.4.3每个课程的本周已学习小节数量
            vo.setWeekLearnedSections(countMap.getOrDefault(r.getId(), 0));
            voList.add(vo);
        }
        return result.pageInfo(p.getTotal(),p.getTotal(),voList);
    }
}
