package com.zhikao.message.service;

import com.zhikao.message.domain.dto.NoticeTaskDTO;
import com.zhikao.message.domain.dto.NoticeTaskFormDTO;
import com.zhikao.message.domain.query.NoticeTaskPageQuery;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.message.domain.po.NoticeTask;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 系统通告的任务表，可以延期或定期发送通告 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface INoticeTaskService extends IService<NoticeTask> {

    Long saveNoticeTask(NoticeTaskFormDTO noticeTaskFormDTO);

    void handleTask(NoticeTask noticeTask);

    void updateNoticeTask(NoticeTaskFormDTO noticeTaskFormDTO);

    PageDTO<NoticeTaskDTO> queryNoticeTasks(NoticeTaskPageQuery pageQuery);

    NoticeTaskDTO queryNoticeTask(Long id);

    PageDTO<NoticeTask> queryTodoNoticeTaskByPage(int pageNo, int size);
}
