package com.zhikao.learning.utils;

import com.zhikao.common.utils.JsonUtils;
import com.zhikao.common.utils.StringUtils;
import com.zhikao.learning.domain.po.LearningLesson;
import com.zhikao.learning.domain.po.LearningRecord;
import com.zhikao.learning.mapper.LearningRecordMapper;
import com.zhikao.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;


/**
 * 学习记录延迟任务处理器。
 * 使用 Redisson 的 RDelayedQueue 替代本地 DelayQueue：队列数据存放在 Redis 中，
 * 多实例共享、服务重启不丢失；并用线程池多线程消费任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningRecordDelayTaskHandler {

    private static final String DELAY_QUEUE_NAME = "learning:record:delay:queue";
    private static final int CONSUMER_COUNT = 4;
    private final static String RECORD_KEY_TEMPLATE = "learning:record:{}";

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final LearningRecordMapper recordMapper;
    private final ILearningLessonService lessonService;
    private final Executor learningRecordExecutor;

    private RBlockingQueue<DelayTask<RecordTaskData>> blockingQueue;
    private RDelayedQueue<DelayTask<RecordTaskData>> delayedQueue;
    private volatile boolean begin = true;

    /**
     * 初始化 Redisson 延迟队列，并启动多个消费线程
     */
    @PostConstruct
    public void init() {
        // 1.创建阻塞队列和延迟队列，数据存放在 Redis 中，多实例共享
        blockingQueue = redissonClient.getBlockingQueue(DELAY_QUEUE_NAME);
        delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        // 2.启动多个消费线程处理到期任务
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            learningRecordExecutor.execute(this::handleDelayTask);
        }
    }

    /**
     * 关闭延迟任务处理线程
     */
    @PreDestroy
    public void stop(){
        begin = false;
        log.debug("延迟任务停止执行！");
    }

    /**
     * 异步延迟任务处理
     */
    public void handleDelayTask(){
        while (begin){
            try {
                // 1.获取到期的延迟任务（阻塞等待，多实例下同一个任务只会被一个实例消费）
                DelayTask<RecordTaskData> task = blockingQueue.take();
                RecordTaskData data = task.getData();
                // 2.查询Redis缓存
                LearningRecord record = readRecordCache(data.getLessonId(), data.getSectionId());
                if(record == null){
                    continue;
                }
                // 3.比较数据，moment值
                if(!Objects.equals(record.getMoment(), data.getMoment())){
                    // 不一致，说明用户还在持续提交播放进度，放弃旧数据
                    continue;
                }
                // 4.一致，持久化数据到数据库
                // 4.1更新学习记录的moment
                record.setFinished(null);
                recordMapper.updateById(record);
                // 4.2更新课表最近学习信息
                LearningLesson lesson = new LearningLesson();
                lesson.setId(data.getLessonId());
                lesson.setLatestSectionId(data.getSectionId());
                lesson.setLatestLearnTime(LocalDateTime.now());
                lessonService.updateById(lesson);

            } catch (InterruptedException e) {
                // 线程被中断，结束消费
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("处理延迟任务发生异常",e);
            }
        }
    }


    /**
     * 添加学习记录至redis缓存并提交延迟任务到队列
     * @param record 学习记录
     */
    public void addLearningRecordTask(LearningRecord record){
        // 1.添加数据到Redis缓存
        writeRecordCache(record);
        // 2.提交延迟任务到Redisson延迟队列，20秒后到期
        delayedQueue.offer(
                new DelayTask<>(new RecordTaskData(record), Duration.ofSeconds(20)),
                20, TimeUnit.SECONDS);
    }

    public void writeRecordCache(LearningRecord record) {
        log.debug("更新学习记录的缓存数据");
        // 1.数据转换
        try {
            String json = JsonUtils.toJsonStr(new RecordCacheData(record));
            // 2.写入Redis
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, record.getLessonId());
            redisTemplate.opsForHash().put(key,record.getSectionId().toString(),json);
            // 3.添加缓存过期时间
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("更新学习记录缓存异常", e);
        }
    }

    /**
     * 从redis缓存中读取学习记录
     * @param lessonId
     * @param sectionId
     * @return
     */
    public LearningRecord readRecordCache(Long lessonId, Long sectionId){
        try {
            // 1.读取Redis数据
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
            Object cacheData = redisTemplate.opsForHash().get(key,sectionId.toString());
            if (cacheData == null){
                return null;
            }
            // 2.数据检查和转换
            return JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
        } catch (Exception e) {
            log.error("读取学习记录缓存异常", e);
            return null;
        }
    }

    /**
     * 从redis缓存中删除学习记录
     * @param lessonId
     * @param sectionId
     */
    public void cleanRecordCache(Long lessonId, Long sectionId){
        // 删除数据
        String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
        redisTemplate.opsForHash().delete(key,sectionId.toString());
    }

    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }

    @Data
    @NoArgsConstructor
    public static class RecordTaskData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }
    }
}
