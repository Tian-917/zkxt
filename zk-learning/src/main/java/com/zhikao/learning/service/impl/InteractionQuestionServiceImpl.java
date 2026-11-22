package com.zhikao.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.cache.CategoryCache;
import com.zhikao.api.client.course.CatalogueClient;
import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.client.search.SearchClient;
import com.zhikao.api.client.user.UserClient;
import com.zhikao.api.dto.course.CataSimpleInfoDTO;
import com.zhikao.api.dto.course.CourseFullInfoDTO;
import com.zhikao.api.dto.course.CourseSimpleInfoDTO;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.utils.BeanUtils;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.StringUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.learning.domain.dto.QuestionFormDTO;
import com.zhikao.learning.domain.po.InteractionQuestion;
import com.zhikao.learning.domain.po.InteractionReply;
import com.zhikao.learning.domain.query.QuestionAdminPageQuery;
import com.zhikao.learning.domain.query.QuestionPageQuery;
import com.zhikao.learning.domain.vo.QuestionAdminVO;
import com.zhikao.learning.domain.vo.QuestionVO;
import com.zhikao.learning.enums.QuestionStatus;
import com.zhikao.learning.mapper.InteractionQuestionMapper;
import com.zhikao.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final IInteractionReplyService replyService;
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;

    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取当前登录的用户id
        Long userId = UserContext.getUser();
        // 2.数据封装
        InteractionQuestion question = BeanUtils.copyBean(questionDTO, InteractionQuestion.class);
        question.setUserId(userId);
        // 3.写入数据库
        save(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id和小节id不能同时为空
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new  BadRequestException("课程id和小节id不能同时为空!");
        }
        // 2.分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class,info -> !info.getProperty().equals("description"))// sql查询排除描述字段
                .eq(Boolean.TRUE.equals(query.getOnlyMine()), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        // 3.1获取提问者id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()){
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 3.2根据id查询最近一次回答
        answerIds.remove(null);
        Map<Long,InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if (CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyService.listByIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if (!reply.getAnonymity()) {
                    userIds.add(reply.getUserId());
                }
            }
        }
        // 3.3查询用户信息（提问者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap =  new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
        }
        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            // 4.1将PO转为VO
            QuestionVO vo = BeanUtils.copyBean(q, QuestionVO.class);
            voList.add(vo);
            // 4.2封装提问者信息
            if(!q.getAnonymity()){
                UserDTO userDTO = userMap.get(q.getUserId());
                if (userDTO != null) {
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            // 4.3封装最近一次回答的消息
            InteractionReply reply = replyMap.get(q.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if (!reply.getAnonymity()) {
                    UserDTO user = userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(user.getName());
                }
            }
        }
        return PageDTO.of(page,voList);

    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1.根据id查询信息
        InteractionQuestion question = getById(id);
        // 2.数据校验
        if (question == null || question.getHidden()) {
            // 没有数据或者被隐藏了
            return null;
        }
        // 3.获取提问者信息
        UserDTO user = null;
        if (!question.getAnonymity()) {
            user = userClient.queryUserById(question.getUserId());
        }
        // 4.封装VO
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if(user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        // 1.处理课程名称，得到课程id
        List<Long> courseIds = null;
        if(StringUtils.isNotBlank(query.getCourseName())){
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if(CollUtils.isEmpty(courseIds)){
                return PageDTO.empty(0L,0L);
            }
        }
        // 2.分页查询
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.准备VO所需要的数据：用户数据、课程数据、章节数据
        Set<Long> userIds = new HashSet<>();
        Set<Long> coursesIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();
        // 3.1获取各种数据的id集合
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            coursesIds.add(q.getCourseId());
            cataIds.add(q.getSectionId());
            cataIds.add(q.getChapterId());
        }
        // 3.2根据id查询用户
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long,UserDTO> userMap = new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
        }
        // 3.3根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(coursesIds);
        Map<Long,CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId,course -> course));
        }
        // 3.4根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        // 4.封装VO
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            // 4.1.将PO转VO，属性拷贝
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            //4.2.用户信息
            UserDTO user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
            }
            //4.3.课程信息以及分类信息
            CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
                //4.4.章节信息
                vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
                vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
            }
        }
        return PageDTO.of(page, voList);
    }

    @Override
    public QuestionAdminVO queryQuestionAdminById(String id) {
        // 1.根据id查询互动问题信息
        InteractionQuestion question = getById(id);
        if (question == null) {
            return null;
        }
        // 2.准备VO所需要的数据：用户数据、课程数据、章节数据、教师数据
        // 2.1根据id查询用户
        UserDTO user = userClient.queryUserById(question.getUserId());
        // 2.2根据id查询课程（包含教师信息）
        CourseFullInfoDTO courseInfo = courseClient.getCourseInfoById(question.getCourseId(), false, true);
        // 2.3根据id查询章节（章和节都要查，才能分别封装章名称、节名称）
        List<Long> cataIds = new ArrayList<>(2);
        cataIds.add(question.getChapterId());
        cataIds.add(question.getSectionId());
        cataIds.remove(null);
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(cataIds)) {
            if (CollUtils.isNotEmpty(catas)) {
                cataMap = catas.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
            }
        }
        // 2.4查询教师数据
        List<UserDTO> teachers = new ArrayList<>();
        if (courseInfo != null && CollUtils.isNotEmpty(courseInfo.getTeacherIds())) {
            teachers = userClient.queryUserByIds(courseInfo.getTeacherIds());
        }
        // 3.封装vo
        QuestionAdminVO vo = BeanUtils.copyBean(question, QuestionAdminVO.class);
        // 3.1封装提问者信息
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        // 3.2封装课程、分类、章节、教师信息
        if (courseInfo != null) {
            vo.setCourseName(courseInfo.getName());
            vo.setCategoryName(categoryCache.getCategoryNames(courseInfo.getCategoryIds()));
            // 直接从map中按id取章、节名称，取不到时给空
            vo.setChapterName(cataMap.getOrDefault(question.getChapterId(), ""));
            vo.setSectionName(cataMap.getOrDefault(question.getSectionId(), ""));
            if (CollUtils.isNotEmpty(teachers)) {
                vo.setTeacherName(teachers.stream()
                        .map(UserDTO::getName)
                        .collect(Collectors.joining("、")));
            }
        }
        // 3.3封装问题状态
        if (question.getStatus() != null) {
            vo.setStatus(question.getStatus().getValue());
        }
        // 4.管理端查看问题后，如果之前是未查看状态，需要标记为已查看
        // 学生新增回答/评论时重置为 UN_CHECK 的逻辑在 InteractionReplyServiceImpl#saveReply 中处理
        if (question.getStatus() == QuestionStatus.UN_CHECK) {
            question.setStatus(QuestionStatus.CHECKED);
            updateById(question);
        }
        return vo;
    }

    @Override
    public void hiddenOrShowQuestion(Long id, boolean hidden) {
        lambdaUpdate()
                .set(InteractionQuestion::getHidden, hidden)
                .eq(InteractionQuestion::getId, id)
                .update();
    }

    @Override
    public void updateQuestion(Long id,QuestionFormDTO questionDTO) {
        InteractionQuestion question = getById(id);
        if (question.getUserId().equals(UserContext.getUser())) {
            lambdaUpdate()
                    .eq(InteractionQuestion::getId, id)
                    .set(InteractionQuestion::getAnonymity, questionDTO.getAnonymity())
                    .set(InteractionQuestion::getChapterId, questionDTO.getChapterId())
                    .set(InteractionQuestion::getSectionId, questionDTO.getSectionId())
                    .set(InteractionQuestion::getDescription, questionDTO.getDescription())
                    .set(InteractionQuestion::getTitle, questionDTO.getTitle())
                    .update();
        }

    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        // 1.查询问题
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }
        // 2.校验问题归属，只能删除自己提出的问题
        if (question.getUserId().equals(UserContext.getUser())) {
            // 删除问题下的回答和评论
            replyService.lambdaUpdate()
                    .eq(InteractionReply::getQuestionId, id)
                    .remove();
        }
        // 3.删除问题
        removeById(id);
    }
}
