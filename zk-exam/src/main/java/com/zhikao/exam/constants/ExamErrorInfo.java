package com.zhikao.exam.constants;

public interface ExamErrorInfo {
    String QUESTION_NOT_EXISTS = "题目不存在";
    String EXAM_NOT_EXISTS = "考试不存在";
    String EXAM_NOT_BELONG_TO_USER = "无权访问该考试";
    String EXAM_ALREADY_TAKEN = "该考试只能参加一次";
    String EXAM_ALREADY_SUBMITTED = "考试已提交，请勿重复提交";
    String EXAM_NOT_SUBMITTED = "考试尚未提交，无法查看结果";
    String EXAM_QUESTIONS_EMPTY = "本小节暂未配置题目，无法创建考试";
    String EXAM_PARAM_INVALID = "考试参数不合法";
    String COURSE_NOT_EXISTS = "课程不存在";
    String SECTION_NOT_EXISTS = "小节不存在";
    String LESSON_NOT_VALID = "未报名该课程，无法参加考试";
    String USER_NOT_LOGIN = "请先登录";
}
