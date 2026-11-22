package com.zhikao.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.promotion.constants.PromotionConstants;
import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.ExchangeCode;
import com.zhikao.promotion.domain.vo.ExchangeCodeVO;
import com.zhikao.promotion.enums.ExchangeCodeStatus;
import com.zhikao.promotion.mapper.ExchangeCodeMapper;
import com.zhikao.promotion.service.IExchangeCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.promotion.utils.CodeUtil;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.zhikao.promotion.constants.PromotionConstants.COUPON_CODE_MAP_KEY;
import static com.zhikao.promotion.constants.PromotionConstants.COUPON_CODE_SERIAL_KEY;

/**
 * <p>
 * 兑换码 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
public class ExchangeCodeServiceImpl extends ServiceImpl<ExchangeCodeMapper, ExchangeCode> implements IExchangeCodeService {

    private final BoundValueOperations<String, String> serialOps;
    private final StringRedisTemplate redisTemple;

    // 提前把兑换码自增序列号的key绑定到一个对象上
    public ExchangeCodeServiceImpl(StringRedisTemplate redisTemplate){
        this.redisTemple = redisTemplate;
        serialOps = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }

    @Override
    @Async("generateExchangeCodeExecutor")
    @Transactional
    public void asyncGenerateCode(Coupon coupon) {
        //发放数量
        Integer totalNum = coupon.getTotalNum();
        // 1.获取Redis自增序列号
        Long result = serialOps.increment(totalNum);
        if (result == null) {
            return;
        }
        int maxSerialNum = result.intValue();
        // 将最大序列号写入redis缓存中
        redisTemple.opsForZSet().add(PromotionConstants.COUPON_RANGE_CACHE_KEY_PREFIX,coupon.getId().toString(), maxSerialNum);
        List<ExchangeCode> list = new ArrayList<>(totalNum);
        for (int serialNum = maxSerialNum - totalNum + 1; serialNum <= maxSerialNum; serialNum++) {
            // 2.生成兑换码
            String code = CodeUtil.generateCode(serialNum, coupon.getId());
            ExchangeCode e = new ExchangeCode();
            e.setId(serialNum);
            e.setCode(code);
            e.setExchangeTargetId(coupon.getId());
            e.setExpiredTime(coupon.getIssueEndTime());
            e.setStatus(ExchangeCodeStatus.UNUSED);
            list.add(e);
        }
        // 3.保存数据库
        saveBatch(list);
    }

    @Override
    public boolean updateExchangeMark(long serialNum, boolean mark) {
        Boolean boo = redisTemple.opsForValue().setBit(COUPON_CODE_MAP_KEY, serialNum, mark);
        return boo != null && boo;
    }

    @Override
    public PageDTO<ExchangeCodeVO> queryCodePage(PageQuery pageQuery, Integer status, Long couponId) {
        // 1.校验参数：兑换码状态和优惠券id为必填
        ExchangeCodeStatus codeStatus = ExchangeCodeStatus.of(status);
        if (codeStatus == null) {
            throw new BadRequestException("兑换码状态不正确");
        }
        // 2.分页查询兑换码：按兑换码状态和对应的优惠券id过滤
        Page<ExchangeCode> page = lambdaQuery()
                .eq(ExchangeCode::getStatus, codeStatus)
                .eq(ExchangeCode::getExchangeTargetId, couponId)
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        List<ExchangeCode> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.实体转VO
        List<ExchangeCodeVO> voList = new ArrayList<>(records.size());
        for (ExchangeCode record : records) {
            ExchangeCodeVO vo = new ExchangeCodeVO();
            vo.setCode(record.getCode());
            vo.setId(record.getId());
            voList.add(vo);
        }
        return PageDTO.of(page, voList);
    }

    @Override
    public void expireCode() {
        // 把已过有效期且仍未兑换的兑换码置为已失效
        lambdaUpdate()
                .eq(ExchangeCode::getStatus, ExchangeCodeStatus.UNUSED)
                .lt(ExchangeCode::getExpiredTime, LocalDateTime.now())
                .set(ExchangeCode::getStatus, ExchangeCodeStatus.EXPIRED)
                .update();
    }
}
