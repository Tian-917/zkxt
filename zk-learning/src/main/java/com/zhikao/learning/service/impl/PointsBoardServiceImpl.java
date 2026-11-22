package com.zhikao.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.client.user.UserClient;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.DateUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.learning.constants.RedisConstants;
import com.zhikao.learning.utils.TableInfoContext;
import com.zhikao.learning.domain.po.PointsBoard;
import com.zhikao.learning.domain.query.PointsBoardQuery;
import com.zhikao.learning.domain.vo.PointsBoardItemVO;
import com.zhikao.learning.domain.vo.PointsBoardVO;
import com.zhikao.learning.mapper.PointsBoardMapper;
import com.zhikao.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.zhikao.learning.constants.LearningConstants.POINTS_BOARD_TABLE_PREFIX;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;

    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        // 1.判断是否是查询当前赛季
        Long season = query.getSeason();
        boolean isCurrent = season == null || season == 0;
        // 2.获取Redis的Key
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 3.查询我的积分和排名
        PointsBoard myBoard = isCurrent ?
                queryMyCurrentBoard(key) : // 查询当前榜单(Redis)
                queryMyHistoryBoard(query.getSeason()); // 查询历史榜单(MySQL)
        // 4.查询榜单列表
        List<PointsBoard> list = isCurrent ?
                queryCurrentBoardList(key,query.getPageNo(),query.getPageSize()) :
                queryHistoryBoardList(query);
        // 5.封装VO
        PointsBoardVO vo = new PointsBoardVO();
        // 5.1处理我的信息
        if (myBoard != null) {
            vo.setPoints(myBoard.getPoints());
            vo.setRank(myBoard.getRank());
        }
        if(CollUtils.isEmpty(list)){
            vo.setBoardList(Collections.emptyList());
            return vo;
        }
        // 5.2处理用户信息
        Set<Long> uIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = new HashMap<>(uIds.size());
        if(CollUtils.isNotEmpty(users)){
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        // 5.3处理榜单列表
        List<PointsBoardItemVO> items = new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO v = new PointsBoardItemVO();
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            v.setName(userMap.get(p.getUserId()));
            items.add(v);
        }
        vo.setBoardList(items);
        return vo;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable(POINTS_BOARD_TABLE_PREFIX + season);
    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        // 1.计算历史榜单表名，并保存外层上下文，查询完成后恢复
        String previous = TableInfoContext.getInfo();
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + query.getSeason());
        try {
            // 2.分页查询历史榜单
            Page<PointsBoard> page = page(query.toMpPage());
            List<PointsBoard> records = page.getRecords();
            if (CollUtils.isEmpty(records)) {
                return CollUtils.emptyList();
            }
            // 3.历史榜单表中id存储的是排名
            records.forEach(b -> b.setRank(b.getId().intValue()));
            return records;
        } finally {
            if (previous == null) {
                TableInfoContext.removeInfo();
            } else {
                TableInfoContext.setInfo(previous);
            }
        }
    }

    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        // 1.计算分页
        int from = (pageNo - 1) * pageSize;
        // 2.查询
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, from, from + pageSize - 1);
        if (CollUtils.isEmpty(tuples)) {
            return Collections.emptyList();
        }
        // 3.封装
        int rank = from + 1;
        List<PointsBoard> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if(userId == null || points == null){
                continue;
            }
            PointsBoard p = new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setPoints(points.intValue());
            p.setRank(rank++);
            list.add(p);
        }
        return list;
    }

    private PointsBoard queryMyHistoryBoard(Long season) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.计算历史榜单表名，并保存外层上下文，查询完成后恢复
        String previous = TableInfoContext.getInfo();
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + season);
        try {
            // 3.查询我的历史榜单记录
            Optional<PointsBoard> opt = lambdaQuery()
                    .eq(PointsBoard::getUserId, userId)
                    .oneOpt();
            if (opt.isEmpty()) {
                return null;
            }
            // 4.历史榜单表中id存储的是排名
            PointsBoard pointsBoard = opt.get();
            pointsBoard.setRank(pointsBoard.getId().intValue());
            return pointsBoard;
        } finally {
            if (previous == null) {
                TableInfoContext.removeInfo();
            } else {
                TableInfoContext.setInfo(previous);
            }
        }
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        // 1.绑定key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        // 2.获取当前用户信息
        String userId = UserContext.getUser().toString();
        // 查询积分
        Double points = ops.score(userId);
        // 查询排名
        Long rank = ops.reverseRank(userId);
        // 封装返回
        PointsBoard myBoard = new PointsBoard();
        myBoard.setPoints(points != null ? points.intValue() : 0);
        myBoard.setRank(rank != null ? rank.intValue() + 1 : 0);
        return myBoard;
    }
}
