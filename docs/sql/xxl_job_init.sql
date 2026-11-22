-- 定时任务初始化脚本，数据库：xxl_job
-- 1. 注册优惠券过期处理任务
-- 2. 启用项目依赖的定时任务
-- 脚本可重复执行

USE xxl_job;

INSERT INTO xxl_job_info (job_group, job_desc, add_time, update_time, author, alarm_email,
                          schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
                          executor_handler, executor_param, executor_block_strategy, executor_timeout,
                          executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
                          child_jobid, trigger_status, trigger_last_time, trigger_next_time)
SELECT 6, '优惠券过期处理', NOW(), NOW(), 'system', '',
       'CRON', '0 0 2 * * ?', 'DO_NOTHING', 'ROUND',
       'expireCouponJobHandler', '', 'SERIAL_EXECUTION', 0,
       0, 'BEAN', '', 'GLUE代码初始化', NOW(),
       '', 1, 0, 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM xxl_job_info WHERE executor_handler = 'expireCouponJobHandler'
);

-- 启用优惠券发放、优惠券过期、积分榜持久化、交易退款、支付检查等任务
UPDATE xxl_job_info
SET trigger_status = 1
WHERE executor_handler IN (
    'couponIssueJobHandler',
    'expireCouponJobHandler',
    'createTableJob',
    'savePointsBoard2DB',
    'clearPointsBoardFromRedis',
    'refundRequestJobHandler',
    'payOrderCheckHandler',
    'refundOrderCheckHandler'
);

-- 榜单持久化完成后触发 Redis 清理任务（如果尚未配置子任务）
UPDATE xxl_job_info p
JOIN xxl_job_info c ON c.executor_handler = 'clearPointsBoardFromRedis'
SET p.child_jobid = c.id
WHERE p.executor_handler = 'savePointsBoard2DB'
  AND (p.child_jobid IS NULL OR p.child_jobid = '');
