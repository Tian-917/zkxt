-- 补齐调度中心任务：证书到期提醒 / 发证补偿 / 定时通知 / 课程完结
-- 先补执行器分组（cert/message/course 之前都没有分组）

INSERT INTO `xxl_job`.`xxl_job_group` (app_name, title, address_type, address_list, update_time) VALUES
  ('cert-service',    '证书服务',   0, NULL, NOW()),
  ('message-service', '消息中心',   0, NULL, NOW()),
  ('course-service',  '课程服务',   0, NULL, NOW());

-- 4 个任务（FIX_RATE 单位：秒；trigger_status=1 启用）
INSERT INTO `xxl_job`.`xxl_job_info`
  (job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf,
   misfire_strategy, executor_route_strategy, executor_handler, executor_param,
   executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source,
   glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time)
SELECT g.id, '证书到期复证提醒', NOW(), NOW(), 'dev', '', 'FIX_RATE', '3600',
       'DO_NOTHING', 'SHARDING_BROADCAST', 'certExpireNoticeJob', '',
       'SERIAL_EXECUTION', 0, 0, 'BEAN', '', NULL, NOW(), '', 1, 0, 0
FROM `xxl_job`.`xxl_job_group` g WHERE g.app_name = 'cert-service';

INSERT INTO `xxl_job`.`xxl_job_info`
  (job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf,
   misfire_strategy, executor_route_strategy, executor_handler, executor_param,
   executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source,
   glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time)
SELECT g.id, '证书发证补偿', NOW(), NOW(), 'dev', '', 'FIX_RATE', '600',
       'DO_NOTHING', 'SHARDING_BROADCAST', 'certIssueCompensateJob', '',
       'SERIAL_EXECUTION', 0, 0, 'BEAN', '', NULL, NOW(), '', 1, 0, 0
FROM `xxl_job`.`xxl_job_group` g WHERE g.app_name = 'cert-service';

INSERT INTO `xxl_job`.`xxl_job_info`
  (job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf,
   misfire_strategy, executor_route_strategy, executor_handler, executor_param,
   executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source,
   glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time)
SELECT g.id, '定时通知发布', NOW(), NOW(), 'dev', '', 'FIX_RATE', '60',
       'DO_NOTHING', 'SHARDING_BROADCAST', 'publishNoticeJob', '',
       'SERIAL_EXECUTION', 0, 0, 'BEAN', '', NULL, NOW(), '', 1, 0, 0
FROM `xxl_job`.`xxl_job_group` g WHERE g.app_name = 'message-service';

INSERT INTO `xxl_job`.`xxl_job_info`
  (job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf,
   misfire_strategy, executor_route_strategy, executor_handler, executor_param,
   executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source,
   glue_remark, glue_updatetime, child_jobid, trigger_status, trigger_last_time, trigger_next_time)
SELECT g.id, '课程完结处理', NOW(), NOW(), 'dev', '', 'FIX_RATE', '3600',
       'DO_NOTHING', 'SHARDING_BROADCAST', 'courseFinished', '',
       'SERIAL_EXECUTION', 0, 0, 'BEAN', '', NULL, NOW(), '', 1, 0, 0
FROM `xxl_job`.`xxl_job_group` g WHERE g.app_name = 'course-service';
