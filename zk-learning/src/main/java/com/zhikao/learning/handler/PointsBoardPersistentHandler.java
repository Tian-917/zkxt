package com.zhikao.learning.handler;


import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.DateUtils;
import com.zhikao.learning.constants.RedisConstants;
import com.zhikao.learning.domain.po.PointsBoard;
import com.zhikao.learning.service.IPointsBoardSeasonService;
import com.zhikao.learning.service.IPointsBoardService;
import com.zhikao.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zhikao.learning.constants.LearningConstants.POINTS_BOARD_TABLE_PREFIX;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {

    private final IPointsBoardService pointsBoardService;
    private final IPointsBoardSeasonService pointsBoardSeasonService;
    private final StringRedisTemplate stringRedisTemplate;
//    @Scheduled(cron = "0 0 3 1 * ?")
    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.查询赛季id
        Integer season = pointsBoardSeasonService.querySeasonByTime(time);
        if(season == null){
            // 赛季不存在
            return;
        }
        // 3.创建表
        pointsBoardService.createPointsBoardTableBySeason(season);
    }

    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.计算动态表名
        // 2.1查询赛季信息
        Integer season = pointsBoardSeasonService.querySeasonByTime(time);
        if (season == null) {
            // 赛季不存在，无需持久化
            return;
        }
        // 2.2存入ThreadLocal
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + season);
        try {
            // 3.查询榜单数据
            // 3.1拼接KEY
            String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
            // 3.2查询数据
            int index = XxlJobHelper.getShardIndex(); // 获取第几片
            int total = XxlJobHelper.getShardTotal();// 总共有几片
            int pageNo = index + 1;
            int pageSize = 1000;
            while(true){
                List<PointsBoard> boardList = pointsBoardService.queryCurrentBoardList(key, pageNo, pageSize);
                if(CollUtils.isEmpty(boardList)){
                    break;
                }
                // 4.持久化到数据库
                // 4.1把排名信息写入id
                boardList.forEach(board -> {
                    board.setId(board.getRank().longValue());
                    board.setRank(null);
                });
                // 4.2持久化
                pointsBoardService.saveBatch(boardList);
                // 5.翻页
                pageNo+=total;
            }
            // 6.分片持久化完成计数，由最后一个完成的分片清理Redis，避免先完成的分片提前清理导致其他分片读不到数据
            String doneKey = key + ":done";
            Long finished = stringRedisTemplate.opsForValue().increment(doneKey);
            stringRedisTemplate.expire(doneKey, 1, TimeUnit.HOURS);
            if (finished != null && finished >= total) {
                stringRedisTemplate.unlink(key);
                stringRedisTemplate.delete(doneKey);
            }
        } finally {
            TableInfoContext.removeInfo();
        }
    }

    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.计算key
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 3.删除
        stringRedisTemplate.unlink(key);
    }
}
