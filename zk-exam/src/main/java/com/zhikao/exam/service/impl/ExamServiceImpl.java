package com.zhikao.exam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.client.learning.LearningClient;
import com.zhikao.api.client.user.UserClient;
import com.zhikao.api.dto.cert.CertIssueDTO;
import com.zhikao.api.dto.course.CatalogueDTO;
import com.zhikao.api.dto.course.CourseFullInfoDTO;
import com.zhikao.api.dto.exam.QuestionDTO;
import com.zhikao.api.dto.leanring.LearningLessonDTO;
import com.zhikao.api.dto.leanring.LearningRecordDTO;
import com.zhikao.api.dto.leanring.LearningRecordFormDTO;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.common.autoconfigure.mq.RabbitMqHelper;
import com.zhikao.common.constants.MqConstants;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.exceptions.UnauthorizedException;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.StringUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.exam.constants.QuestionType;
import com.zhikao.exam.domain.dto.ExamAnswerDTO;
import com.zhikao.exam.domain.dto.ExamCreateDTO;
import com.zhikao.exam.domain.dto.ExamSubmitDTO;
import com.zhikao.exam.domain.po.ExamRecord;
import com.zhikao.exam.domain.po.ExamRecordDetail;
import com.zhikao.exam.domain.vo.ExamPageVO;
import com.zhikao.exam.domain.vo.ExamQuestionVO;
import com.zhikao.exam.domain.vo.ExamRecordDetailVO;
import com.zhikao.exam.domain.vo.ExamVO;
import com.zhikao.exam.repository.ExamRecordRepository;
import com.zhikao.exam.service.IExamService;
import com.zhikao.exam.service.IWrongQuestionService;
import com.zhikao.exam.service.IQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.zhikao.exam.constants.ExamConstants.PASS_SCORE;
import static com.zhikao.exam.constants.ExamConstants.STATUS_FINISHED;
import static com.zhikao.exam.constants.ExamConstants.STATUS_UNFINISHED;
import static com.zhikao.exam.constants.ExamConstants.TYPE_EXAM;
import static com.zhikao.exam.constants.ExamConstants.TYPE_PRACTICE;
import static com.zhikao.exam.constants.ExamErrorInfo.COURSE_NOT_EXISTS;
import static com.zhikao.exam.constants.ExamErrorInfo.EXAM_ALREADY_SUBMITTED;
import static com.zhikao.exam.constants.ExamErrorInfo.EXAM_ALREADY_TAKEN;
import static com.zhikao.exam.constants.ExamErrorInfo.EXAM_NOT_EXISTS;
import static com.zhikao.exam.constants.ExamErrorInfo.EXAM_NOT_SUBMITTED;
import static com.zhikao.exam.constants.ExamErrorInfo.EXAM_PARAM_INVALID;
import static com.zhikao.exam.constants.ExamErrorInfo.EXAM_QUESTIONS_EMPTY;
import static com.zhikao.exam.constants.ExamErrorInfo.LESSON_NOT_VALID;
import static com.zhikao.exam.constants.ExamErrorInfo.SECTION_NOT_EXISTS;
import static com.zhikao.exam.constants.ExamErrorInfo.USER_NOT_LOGIN;

/**
 * <p>
 * 考试评测 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements IExamService {

    private final ExamRecordRepository recordRepository;

    private final IQuestionService questionService;

    private final CourseClient courseClient;

    private final LearningClient learningClient;
    private final UserClient userClient;
    private final RabbitMqHelper rabbitMqHelper;
    private final IWrongQuestionService wrongQuestionService;

    @Override
    public ExamVO createExam(ExamCreateDTO dto) {
        // 1.校验登录用户与参数
        Long userId = getUserId();
        if (dto == null || dto.getCourseId() == null || dto.getSectionId() == null) {
            throw new BadRequestException(EXAM_PARAM_INVALID);
        }
        Integer type = dto.getType();
        if (type == null || (type != TYPE_PRACTICE && type != TYPE_EXAM)) {
            throw new BadRequestException(EXAM_PARAM_INVALID);
        }
        Long courseId = dto.getCourseId();
        Long sectionId = dto.getSectionId();
        // 2.考试只能参加一次，练习可以重复
        if (type == TYPE_EXAM
                && recordRepository.countByUserIdAndSectionIdAndType(userId, sectionId, TYPE_EXAM) > 0) {
            throw new BadRequestException(EXAM_ALREADY_TAKEN);
        }
        // 3.校验课程报名状态
        Long lessonId = learningClient.isLessonValid(courseId);
        if (lessonId == null) {
            throw new BadRequestException(LESSON_NOT_VALID);
        }
        // 4.查询课程与小节名称
        CourseFullInfoDTO course = courseClient.getCourseInfoById(courseId, true, false);
        if (course == null) {
            throw new BadRequestException(COURSE_NOT_EXISTS);
        }
        String sectionName = findSectionName(course, sectionId);
        if (sectionName == null) {
            throw new BadRequestException(SECTION_NOT_EXISTS);
        }
        // 5.查询试卷题目
        List<QuestionDTO> questions = questionService.queryQuestionByBizId(sectionId);
        if (CollUtils.isEmpty(questions)) {
            throw new BadRequestException(EXAM_QUESTIONS_EMPTY);
        }
        // 6.保存考试记录
        ExamRecord record = new ExamRecord()
                .setId(IdWorker.getIdStr())
                .setUserId(userId)
                .setCourseId(courseId)
                .setSectionId(sectionId)
                .setLessonId(lessonId)
                .setCourseName(course.getName())
                .setSectionName(sectionName)
                .setType(type)
                .setStatus(STATUS_UNFINISHED)
                .setStartTime(LocalDateTime.now())
                .setQuestionIds(questions.stream().map(QuestionDTO::getId).collect(Collectors.toList()));
        recordRepository.save(record);
        // 7.返回试卷，不包含答案与解析
        List<ExamQuestionVO> questionVOs = questions.stream()
                .map(question -> toQuestionVO(question, false))
                .collect(Collectors.toList());
        return new ExamVO(record.getId(), questionVOs);
    }

    @Override
    public List<ExamRecordDetailVO> submitExam(ExamSubmitDTO dto) {
        // 1.校验登录用户与参数
        Long userId = getUserId();
        if (dto == null || StringUtils.isBlank(dto.getId())) {
            throw new BadRequestException(EXAM_PARAM_INVALID);
        }
        // 2.查询本人考试记录
        ExamRecord record = recordRepository.findByIdAndUserId(dto.getId(), userId)
                .orElseThrow(() -> new BadRequestException(EXAM_NOT_EXISTS));
        if (record.getStatus() != null && record.getStatus() == STATUS_FINISHED) {
            throw new BadRequestException(EXAM_ALREADY_SUBMITTED);
        }
        // 3.按试卷顺序加载题目
        List<QuestionDTO> questions = queryQuestionsInOrder(record.getQuestionIds());
        if (CollUtils.isEmpty(questions)) {
            throw new BadRequestException(EXAM_QUESTIONS_EMPTY);
        }
        // 4.构建提交答案映射
        Map<Long, Object> answerMap = new HashMap<>();
        if (CollUtils.isNotEmpty(dto.getExamDetails())) {
            for (ExamAnswerDTO answerDTO : dto.getExamDetails()) {
                if (answerDTO != null && answerDTO.getQuestionId() != null) {
                    answerMap.put(answerDTO.getQuestionId(), answerDTO.getAnswer());
                }
            }
        }
        // 5.逐题阅卷
        List<ExamRecordDetail> details = new ArrayList<>(questions.size());
        Map<Long, Boolean> correctMap = new HashMap<>();
        int totalScore = 0;
        for (QuestionDTO question : questions) {
            Integer questionType = getQuestionType(question);
            String studentAnswer = normalizeAnswer(answerMap.get(question.getId()), questionType);
            boolean correct = checkCorrect(questionType, question.getAnswer(), studentAnswer);
            int questionScore = question.getScore() == null ? 0 : question.getScore();
            int score = correct ? questionScore : 0;
            details.add(new ExamRecordDetail()
                    .setQuestionId(question.getId())
                    .setQuestionType(questionType)
                    .setAnswer(studentAnswer)
                    .setCorrect(correct)
                    .setScore(score));
            if (!studentAnswer.isEmpty()) {
                correctMap.put(question.getId(), correct);
            }
            totalScore += score;
        }
        // 6.更新考试记录
        LocalDateTime commitTime = LocalDateTime.now();
        record.setStatus(STATUS_FINISHED)
                .setCommitTime(commitTime)
                .setDuration(calcDuration(record.getStartTime(), commitTime))
                .setScore(totalScore)
                .setExamDetails(details);
        recordRepository.save(record);
        // 7.更新题目答题统计
        questionService.updateQuestionStatistics(correctMap);
        // 7.1 归档错题：答错入错题本，答对把历史错题置为已掌握
        wrongQuestionService.recordFromExam(record, details);
        // 8.首次完成时同步学习记录
        syncLearningRecord(record);
        // 9.正式考通过 -> 发证事件（证书域异步签发，最终一致）
        if (isCertifiable(record)) {
            publishCertEvent(record);
        }
        // 10.返回阅卷明细
        return toDetailVOs(record, questions);
    }

    /** 正式考且成绩达标才具备发证资格 */
    private boolean isCertifiable(ExamRecord record) {
        return record.getType() != null && record.getType() == TYPE_EXAM
                && record.getScore() != null && record.getScore() >= PASS_SCORE;
    }

    /** 发送发证事件：学时由学习服务按已完成小节汇总 */
    private void publishCertEvent(ExamRecord record) {
        try {
            Integer minutes = learningClient.queryStudyMinutes(record.getCourseId());
            Long examId;
            try {
                examId = Long.valueOf(record.getId());
            } catch (NumberFormatException e) {
                examId = record.getSectionId();
            }
            CertIssueDTO dto = new CertIssueDTO()
                    .setUserId(record.getUserId())
                    .setUserName(queryUserName(record.getUserId()))
                    .setCertType(record.getCourseName())
                    .setExamId(examId)
                    .setScore(record.getScore())
                    .setStudyMinutes(minutes == null ? 0 : minutes);
            rabbitMqHelper.send(MqConstants.Exchange.CERT_EXCHANGE, MqConstants.Key.CERT_ISSUE_KEY, dto);
            log.info("考试通过，已投递发证事件：examId={}, userId={}, score={}, studyMinutes={}",
                    examId, record.getUserId(), record.getScore(), minutes);
        } catch (Exception e) {
            // 发证失败不影响交卷结果，证书域有补偿任务兜底
            log.error("发证事件投递失败：examId={}", record.getId(), e);
        }
    }

    private String queryUserName(Long userId) {
        try {
            UserDTO user = userClient.queryUserById(userId);
            return user == null ? null : user.getName();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public PageDTO<ExamPageVO> queryMyExams(PageQuery query) {
        // 1.获取登录用户
        Long userId = getUserId();
        // 2.分页查询已提交的考试记录
        int pageNo = query.getPageNo() == null ? 1 : Math.max(1, query.getPageNo());
        int pageSize = query.getPageSize() == null ? 10 : Math.max(1, query.getPageSize());
        PageRequest pageRequest = PageRequest.of(
                pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "commitTime"));
        Page<ExamRecord> page = recordRepository.findByUserIdAndStatus(userId, STATUS_FINISHED, pageRequest);
        // 3.转换VO
        List<ExamPageVO> list = page.getContent().stream()
                .map(record -> new ExamPageVO()
                        .setId(record.getId())
                        .setCourseName(record.getCourseName())
                        .setSectionName(record.getSectionName())
                        .setType(record.getType() == null ? 0 : record.getType() - 1)
                        .setScore(record.getScore() == null ? 0 : record.getScore())
                        .setDuration(record.getDuration())
                        .setCommitTime(record.getCommitTime()))
                .collect(Collectors.toList());
        return new PageDTO<>(page.getTotalElements(), (long) page.getTotalPages(), list);
    }

    @Override
    public List<ExamRecordDetailVO> queryExamDetail(String id) {
        // 1.获取登录用户
        Long userId = getUserId();
        if (StringUtils.isBlank(id)) {
            throw new BadRequestException(EXAM_NOT_EXISTS);
        }
        // 2.查询本人已提交的考试记录
        ExamRecord record = recordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BadRequestException(EXAM_NOT_EXISTS));
        if (record.getStatus() == null || record.getStatus() != STATUS_FINISHED) {
            throw new BadRequestException(EXAM_NOT_SUBMITTED);
        }
        // 3.加载题目并组装明细
        List<QuestionDTO> questions = queryQuestionsInOrder(record.getQuestionIds());
        return toDetailVOs(record, questions);
    }

    /**
     * 获取当前登录用户，未登录则抛出异常
     */
    private Long getUserId() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException(USER_NOT_LOGIN);
        }
        return userId;
    }

    /**
     * 在课程目录中查找小节名称
     */
    private String findSectionName(CourseFullInfoDTO course, Long sectionId) {
        if (CollUtils.isEmpty(course.getChapters())) {
            return null;
        }
        for (CatalogueDTO chapter : course.getChapters()) {
            if (CollUtils.isEmpty(chapter.getSections())) {
                continue;
            }
            for (CatalogueDTO section : chapter.getSections()) {
                if (sectionId.equals(section.getId())) {
                    return section.getName();
                }
            }
        }
        return null;
    }

    /**
     * 按给定id顺序查询题目
     */
    private List<QuestionDTO> queryQuestionsInOrder(List<Long> questionIds) {
        if (CollUtils.isEmpty(questionIds)) {
            return CollUtils.emptyList();
        }
        List<QuestionDTO> questions = questionService.queryQuestionByIds(questionIds);
        if (CollUtils.isEmpty(questions)) {
            return CollUtils.emptyList();
        }
        Map<Long, QuestionDTO> questionMap = questions.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, question -> question));
        return questionIds.stream()
                .map(questionMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取题目类型，题目类型在DTO中是字符串
     */
    private Integer getQuestionType(QuestionDTO question) {
        if (question.getType() == null) {
            return null;
        }
        try {
            return Integer.valueOf(question.getType());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 归一化学生答案：选择题为按数值升序的选项序号，判断题为1/0，主观题为原文
     */
    private String normalizeAnswer(Object rawAnswer, Integer questionType) {
        if (rawAnswer == null) {
            return "";
        }
        if (questionType == null) {
            return rawAnswer.toString().trim();
        }
        if (questionType == QuestionType.JUDGE.getValue()) {
            return normalizeJudgeAnswer(rawAnswer);
        }
        if (questionType == QuestionType.RADIO.getValue()) {
            return normalizeSingleAnswer(rawAnswer);
        }
        if (questionType == QuestionType.MULTI.getValue()
                || questionType == QuestionType.UNCERTAINTY.getValue()) {
            return normalizeMultiAnswer(rawAnswer);
        }
        return rawAnswer.toString();
    }

    /**
     * 单选题答案归一化
     */
    private String normalizeSingleAnswer(Object rawAnswer) {
        if (rawAnswer instanceof Collection) {
            for (Object item : (Collection<?>) rawAnswer) {
                if (item != null) {
                    return item.toString().trim();
                }
            }
            return "";
        }
        return rawAnswer.toString().trim();
    }

    /**
     * 多选题答案归一化：拆分、按数值升序、去重后逗号连接
     */
    private String normalizeMultiAnswer(Object rawAnswer) {
        if (rawAnswer == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (rawAnswer instanceof Collection) {
            for (Object item : (Collection<?>) rawAnswer) {
                if (item != null && StringUtils.isNotBlank(item.toString())) {
                    parts.add(item.toString().trim());
                }
            }
        } else {
            String text = rawAnswer.toString().trim();
            if (text.isEmpty()) {
                return "";
            }
            for (String part : text.split(",")) {
                if (StringUtils.isNotBlank(part)) {
                    parts.add(part.trim());
                }
            }
        }
        return sortAndJoin(parts);
    }

    /**
     * 对选项序号去重并按数值升序拼接
     */
    private String sortAndJoin(List<String> parts) {
        if (CollUtils.isEmpty(parts)) {
            return "";
        }
        TreeSet<Integer> numbers = new TreeSet<>();
        List<String> others = new ArrayList<>();
        for (String part : parts) {
            try {
                numbers.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                if (!others.contains(part)) {
                    others.add(part);
                }
            }
        }
        List<String> all = numbers.stream().map(String::valueOf).collect(Collectors.toList());
        all.addAll(others);
        return String.join(",", all);
    }

    /**
     * 判断题答案归一化：true->1，false/其他->0
     */
    private String normalizeJudgeAnswer(Object rawAnswer) {
        if (rawAnswer instanceof Boolean) {
            return (Boolean) rawAnswer ? "1" : "0";
        }
        String text = rawAnswer.toString().trim().toLowerCase();
        if (text.isEmpty()) {
            return "";
        }
        return "true".equals(text) || "1".equals(text) ? "1" : "0";
    }

    /**
     * 判断答案是否正确，主观题不自动阅卷
     */
    private boolean checkCorrect(Integer questionType, String correctAnswer, String studentAnswer) {
        if (questionType == null || StringUtils.isEmpty(studentAnswer)) {
            return false;
        }
        if (questionType == QuestionType.SUBJECTIVE.getValue()) {
            return false;
        }
        if (questionType == QuestionType.JUDGE.getValue()) {
            String correct = "1".equals(StringUtils.trim(correctAnswer)) ? "1" : "0";
            return correct.equals(studentAnswer);
        }
        if (questionType == QuestionType.RADIO.getValue()) {
            return studentAnswer.equals(StringUtils.trim(correctAnswer));
        }
        if (questionType == QuestionType.MULTI.getValue()
                || questionType == QuestionType.UNCERTAINTY.getValue()) {
            return normalizeMultiAnswer(studentAnswer).equals(normalizeMultiAnswer(correctAnswer));
        }
        return false;
    }

    /**
     * 计算答题时长，单位秒
     */
    private Integer calcDuration(LocalDateTime startTime, LocalDateTime commitTime) {
        if (startTime == null || commitTime == null) {
            return 0;
        }
        long seconds = Duration.between(startTime, commitTime).getSeconds();
        return seconds < 0 ? 0 : (int) seconds;
    }

    /**
     * 首次完成该小节时同步学习记录
     */
    private void syncLearningRecord(ExamRecord record) {
        try {
            LearningLessonDTO lesson = learningClient.queryLearningRecordByCourse(record.getCourseId());
            if (lesson == null) {
                // 查询失败时无法判断是否已同步，跳过以避免产生重复学习记录
                log.warn("查询学习记录失败，跳过学习同步，examId={}", record.getId());
                return;
            }
            List<LearningRecordDTO> records = lesson.getRecords();
            boolean exists = CollUtils.isNotEmpty(records) && records.stream()
                    .anyMatch(item -> Objects.equals(item.getSectionId(), record.getSectionId()));
            if (exists) {
                return;
            }
            LearningRecordFormDTO formDTO = new LearningRecordFormDTO();
            // 2表示考试类型
            formDTO.setSectionType(2);
            formDTO.setLessonId(record.getLessonId());
            formDTO.setSectionId(record.getSectionId());
            formDTO.setDuration(record.getDuration());
            formDTO.setMoment(0);
            formDTO.setCommitTime(record.getCommitTime());
            learningClient.addLearningRecord(formDTO);
        } catch (Exception e) {
            // 学习同步失败不影响交卷结果
            log.error("同步学习记录失败，examId={}", record.getId(), e);
        }
    }

    /**
     * 组装答题明细
     */
    private List<ExamRecordDetailVO> toDetailVOs(ExamRecord record, List<QuestionDTO> questions) {
        Map<Long, ExamRecordDetail> detailMap = new HashMap<>();
        if (CollUtils.isNotEmpty(record.getExamDetails())) {
            for (ExamRecordDetail detail : record.getExamDetails()) {
                detailMap.put(detail.getQuestionId(), detail);
            }
        }
        List<ExamRecordDetailVO> list = new ArrayList<>(questions.size());
        for (QuestionDTO question : questions) {
            ExamRecordDetail detail = detailMap.get(question.getId());
            Integer questionType = getQuestionType(question);
            String answer = detail == null || detail.getAnswer() == null ? "" : detail.getAnswer();
            list.add(new ExamRecordDetailVO()
                    .setQuestion(toQuestionVO(question, true))
                    .setAnswer(toDisplayAnswer(questionType, answer))
                    .setCorrect(detail != null && Boolean.TRUE.equals(detail.getCorrect()))
                    .setScore(detail == null || detail.getScore() == null ? 0 : detail.getScore()));
        }
        return list;
    }

    /**
     * 题目DTO转VO，创建考试时不携带答案与解析
     */
    private ExamQuestionVO toQuestionVO(QuestionDTO question, boolean withAnswer) {
        Integer questionType = getQuestionType(question);
        ExamQuestionVO vo = new ExamQuestionVO()
                .setId(question.getId())
                .setName(question.getName())
                .setType(questionType)
                .setDifficulty(question.getDifficulty())
                .setScore(question.getScore())
                .setOptions(question.getOptions());
        if (withAnswer) {
            vo.setAnswer(toDisplayCorrectAnswer(questionType, question.getAnswer()));
            vo.setAnalysis(question.getAnalysis());
        }
        return vo;
    }

    /**
     * 学生答案展示转换：判断题转为布尔值
     */
    private Object toDisplayAnswer(Integer questionType, String answer) {
        if (StringUtils.isBlank(answer)) {
            return "";
        }
        if (questionType != null && questionType == QuestionType.JUDGE.getValue()) {
            return "1".equals(answer);
        }
        return answer;
    }

    /**
     * 正确答案展示转换：判断题转为布尔值
     */
    private Object toDisplayCorrectAnswer(Integer questionType, String answer) {
        if (questionType != null && questionType == QuestionType.JUDGE.getValue()) {
            return "1".equals(StringUtils.trim(answer));
        }
        return answer;
    }
}
