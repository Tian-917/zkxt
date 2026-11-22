package com.zhikao.promotion.service.impl;

import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDto;
import com.zhikao.common.autoconfigure.mq.RabbitMqHelper;
import com.zhikao.common.autoconfigure.redisson.annotations.Lock;
import com.zhikao.common.constants.MqConstants;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.domain.query.PageQuery;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.exceptions.BizIllegalException;
import com.zhikao.common.exceptions.DbException;
import com.zhikao.common.utils.*;
import com.zhikao.promotion.constants.PromotionConstants;
import com.zhikao.promotion.domain.dto.UserCouponDTO;
import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.UserCoupon;
import com.zhikao.promotion.domain.vo.CouponVO;
import com.zhikao.promotion.enums.UserCouponStatus;
import com.zhikao.promotion.mapper.CouponMapper;
import com.zhikao.promotion.mapper.UserCouponMapper;
import com.zhikao.promotion.service.IUserCouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.promotion.strategy.discount.DiscountStrategy;
import com.zhikao.promotion.utils.CodeUtil;
import com.zhikao.promotion.utils.PromotionRedisScripts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    private final CouponMapper couponMapper;
    //private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitMqHelper rabbitMqHelper;

    @Override
    @Lock(name = "lock:coupon:#{couponId}")
    public void receiveCoupon(Long couponId) {
        /*// 1.查询优惠券
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 2.校验发放时间（注意：这里校验的是发放时间，不是优惠券有效期）
        LocalDateTime now = LocalDateTime.now();
        if(coupon.getIssueBeginTime() == null || now.isBefore(coupon.getIssueBeginTime())
                || coupon.getIssueEndTime() == null || now.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("优惠券发放尚未开始或已经结束");
        }
        // 3.校验库存
        if(coupon.getIssueNum() >= coupon.getTotalNum()){
            throw new BadRequestException("优惠券库存不足");
        }
        Long userId = UserContext.getUser();
        // 4.校验并生成优惠券
        synchronized (userId.toString().intern()) {
            IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
            userCouponService.checkAndCreateUserCoupon(userId, coupon);
        }
        String key = "lock:coupon:uid" + userId;
        // 4.1创建锁对象
        RLock lock = redissonClient.getLock(key);
        // 4.2尝试获取锁
        boolean isLock = lock.tryLock();
        // 4.3判断是否获取锁成功
        if(!isLock){
            throw new BizIllegalException("请求太频繁");
        }
        try {
            // 4.4获取成功，执行业务
            IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
            userCouponService.checkAndCreateUserCoupon(userId, coupon);
        } finally {
            // 4.5释放锁
            lock.unlock();
        }
        IUserCouponService userCouponService = (IUserCouponService) AopContext.currentProxy();
        userCouponService.checkAndCreateUserCoupon(userId, coupon);*/
        // 1.查询优惠券
        Coupon coupon = queryCouponByCache(couponId);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 2.校验发放时间（注意：这里校验的是发放时间，不是优惠券有效期）
        LocalDateTime now = LocalDateTime.now();
        if(coupon.getIssueBeginTime() == null || now.isBefore(coupon.getIssueBeginTime())
                || coupon.getIssueEndTime() == null || now.isAfter(coupon.getIssueEndTime())){
            throw new BadRequestException("优惠券发放尚未开始或已经结束");
        }
        Long userId = UserContext.getUser();
        // 3.通过Lua脚本原子校验库存、限领数量并扣减库存
        String couponCacheKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;
        String userCountKey = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
        int userLimit = coupon.getUserLimit() == null ? 1 : coupon.getUserLimit();
        Long result = stringRedisTemplate.execute(
                PromotionRedisScripts.RECEIVE_COUPON,
                Arrays.asList(couponCacheKey, userCountKey),
                userId.toString(), String.valueOf(userLimit)
        );
        if (result == null) {
            throw new BizIllegalException("领取优惠券失败");
        }
        if (result == -1L) {
            throw new BadRequestException("优惠券库存不足");
        }
        if (result == -2L) {
            throw new BadRequestException("超出领取数量");
        }
        // 4.发送mq消息，发送失败时回滚缓存中的库存和领取数量
        try {
            UserCouponDTO uc = new UserCouponDTO();
            uc.setUserId(userId);
            uc.setCouponId(couponId);
            rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,MqConstants.Key.COUPON_RECEIVE,uc);
        } catch (Exception e) {
            log.error("发送领券消息失败，couponId={}, userId={}", couponId, userId, e);
            stringRedisTemplate.execute(
                    PromotionRedisScripts.ROLLBACK_RECEIVE_COUPON,
                    Arrays.asList(couponCacheKey, userCountKey),
                    userId.toString()
            );
            throw new BizIllegalException(500, "领取优惠券失败", e);
        }
    }

    private Coupon queryCouponByCache(Long couponId) {
        // 1.准备key
        String key = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;
        // 2.查询map
        Map<Object, Object> objMap = stringRedisTemplate.opsForHash().entries(key);
        if(BeanUtils.isEmpty(objMap)){
            return null;
        }
        // 3.数据反序列化
        return BeanUtils.mapToBean(objMap,Coupon.class,false, CopyOptions.create());
    }

    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO userCouponDTO) {
        // 1.查询优惠券
        Coupon coupon = couponMapper.selectById(userCouponDTO.getCouponId());
        // 2.更新优惠券已发放数量 + 1
        int r = couponMapper.incrIssueNum(coupon.getId());
        if(r == 0){
            throw new BizIllegalException("优惠券库存不足");
        }
        // 3.新增一个优惠券
        saveUserCoupon(userCouponDTO.getUserId(), coupon);
    }

    @Override
    public void exchangeCoupon(String code) {
        // 1.解析兑换码，得到序列号
        long serialNum = CodeUtil.parseCode(code);
        Long userId = UserContext.getUser();
        // 2.查询兑换码对应的优惠券id
        // ZSet的score是每张优惠券兑换码的最大序列号，第一个score >= serialNum的成员就是该兑换码所属的券
        Set<String> couponIds = stringRedisTemplate.opsForZSet()
                .rangeByScore(PromotionConstants.COUPON_RANGE_CACHE_KEY_PREFIX,
                        serialNum, Long.MAX_VALUE, 0, 1);
        if (couponIds == null || couponIds.isEmpty()) {
            throw new BizIllegalException("兑换码不存在！");
        }
        Long couponId = Long.valueOf(couponIds.iterator().next());
        String couponCacheKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponId;
        // 3.校验是否过期
        String issueEndTime = (String) stringRedisTemplate.opsForHash()
                .get(couponCacheKey, "issueEndTime");
        if (StringUtils.isBlank(issueEndTime)
                || LocalDateTime.now().isAfter(LocalDateTime.parse(issueEndTime,
                DateTimeFormatter.ofPattern(DateUtils.DEFAULT_DATE_TIME_FORMAT)))) {
            throw new BizIllegalException("兑换码已经过期");
        }
        // 4.读取每人限领数量
        String userLimitStr = (String) stringRedisTemplate.opsForHash()
                .get(couponCacheKey, "userLimit");
        if (StringUtils.isBlank(userLimitStr)) {
            throw new BizIllegalException("优惠券不存在");
        }
        int userLimit = Integer.parseInt(userLimitStr);
        // 5.通过Lua脚本原子校验兑换码是否已兑换、限领数量，并写入兑换标记
        String userCountKey = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponId;
        Long result = stringRedisTemplate.execute(
                PromotionRedisScripts.EXCHANGE_COUPON,
                Arrays.asList(PromotionConstants.COUPON_CODE_MAP_KEY, userCountKey),
                String.valueOf(serialNum), userId.toString(), String.valueOf(userLimit)
        );
        if (result == null) {
            throw new BizIllegalException("兑换优惠券失败");
        }
        if (result == -1L) {
            throw new BizIllegalException("兑换码已经被兑换过了");
        }
        if (result == -2L) {
            throw new BadRequestException("超出领取数量");
        }
        // 6.发送MQ消息，发送失败时回滚兑换标记和领取数量
        try {
            UserCouponDTO uc = new UserCouponDTO();
            uc.setUserId(userId);
            uc.setCouponId(couponId);
            uc.setCodeId(serialNum);
            rabbitMqHelper.send(MqConstants.Exchange.PROMOTION_EXCHANGE,
                    MqConstants.Key.COUPON_EXCHANGE, uc);
        } catch (Exception e) {
            log.error("发送兑换消息失败，code={}, userId={}", code, userId, e);
            stringRedisTemplate.execute(
                    PromotionRedisScripts.ROLLBACK_EXCHANGE_COUPON,
                    Arrays.asList(PromotionConstants.COUPON_CODE_MAP_KEY, userCountKey),
                    String.valueOf(serialNum), userId.toString()
            );
            throw new BizIllegalException(500, "兑换优惠券失败", e);
        }
    }

    @Override
    public PageDTO<CouponVO> queryMyCouponPage(PageQuery pageQuery, UserCouponStatus status) {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        // 2.分页查询该用户
        Page<UserCoupon> page = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        List<UserCoupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.批量查询优惠券模板，避免循环查库（N+1）
        List<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        Map<Long, Coupon> couponMap = coupons.stream().collect(Collectors.toMap(Coupon::getId, c -> c));
        // 4.封装VO并返回
        List<CouponVO> voList = new ArrayList<>(records.size());
        for (UserCoupon record : records) {
            Coupon coupon = couponMap.get(record.getCouponId());
            if (coupon == null) {
                // 优惠券模板已被删除，跳过
                continue;
            }
            CouponVO vo = BeanUtils.copyBean(coupon, CouponVO.class);
            vo.setTermEndTime(record.getTermEndTime());
            voList.add(vo);
        }
        return PageDTO.of(page,voList);
    }

    @Override
    @Transactional
    public void refundCoupon(List<Long> couponIds) {
        // 1.获取当前用户；内部系统调用（如订单超时关闭）没有用户上下文，此时不限制用户
        Long userId = UserContext.getUser();
        if(CollUtils.isEmpty(couponIds)){
            return;
        }
        // 2.根据ids查询需退还的优惠券
        List<UserCoupon> userCoupons = lambdaQuery()
                .eq(userId != null, UserCoupon::getUserId, userId)
                .in(UserCoupon::getId, couponIds)
                .list();
        if(CollUtils.isEmpty(userCoupons)){
            return;
        }
        // 3.只处理已使用的券：已过期置为EXPIRED，未过期恢复UNUSED
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> refundable = new ArrayList<>(userCoupons.size());
        Set<Long> templateIds = new HashSet<>();
        for (UserCoupon uc : userCoupons) {
            if(uc.getStatus() != UserCouponStatus.USED){
                continue;
            }
            templateIds.add(uc.getCouponId());
            UserCoupon update = new UserCoupon();
            update.setId(uc.getId());
            update.setStatus(now.isAfter(uc.getTermEndTime())
                    ? UserCouponStatus.EXPIRED
                    : UserCouponStatus.UNUSED);
            refundable.add(update);
        }
        if(CollUtils.isEmpty(refundable)){
            return;
        }
        // 4.批量更新状态
        boolean success = updateBatchById(refundable);
        if(!success){
            return;
        }
        // 5.更新优惠券已使用数量
        int rows = couponMapper.incrUsedNum(new ArrayList<>(templateIds), -1);
        if(rows < 1){
            throw new DbException("更新优惠券使用数量失败");
        }
    }

    @Override
    public void expireCoupon() {
        // 把已过有效期且仍未使用的优惠券置为已失效
        lambdaUpdate()
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED)
                .lt(UserCoupon::getTermEndTime, LocalDateTime.now())
                .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED)
                .update();
    }

    @Override
    public List<String> queryDiscountRules(List<Long> userCouponIds) {
        if(CollUtils.isEmpty(userCouponIds)){
            return CollUtils.emptyList();
        }
        // 1.根据用户优惠券id查询优惠券，不做用户过滤。
        // 订单id全局唯一，管理端查询他人订单详情时也需要还原优惠券规则
        List<UserCoupon> userCoupons = lambdaQuery()
                .in(UserCoupon::getId, userCouponIds)
                .list();
        if(CollUtils.isEmpty(userCoupons)){
            return CollUtils.emptyList();
        }
        // 3.查询优惠券模板
        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        Map<Long, Coupon> couponMap = coupons.stream().collect(Collectors.toMap(Coupon::getId, c -> c));
        // 4.生成规则描述
        List<String> rules = new ArrayList<>(userCoupons.size());
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMap.get(uc.getCouponId());
            if (coupon == null) {
                continue;
            }
            rules.add(DiscountStrategy.getDiscount(coupon.getDiscountType()).getRule(coupon));
        }
        return rules;
    }

    @Override
    @Transactional
    public void useCoupon(List<Long> userCouponIds) {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        if (CollUtils.isEmpty(userCouponIds)) {
            return;
        }
        // 2.获取当前用户指定的优惠券
        List<UserCoupon> userCoupons = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .in(UserCoupon::getId, userCouponIds)
                .list();
        if(CollUtils.isEmpty(userCoupons)){
            return;
        }
        // 3.核销指定优惠券
        // 3.1校验优惠券是否满足条件：未使用 + 未过期
        LocalDateTime now = LocalDateTime.now();
        for (UserCoupon uc : userCoupons) {
            if (uc.getStatus() != UserCouponStatus.UNUSED
                    || now.isBefore(uc.getTermBeginTime())
                    || now.isAfter(uc.getTermEndTime())) {
                throw new BadRequestException("优惠券状态异常或已过期");
            }

        }
        // 3.2统一更新
        boolean success = lambdaUpdate()
                .in(UserCoupon::getId, userCouponIds)
                .eq(UserCoupon::getUserId, userId)
                .set(UserCoupon::getStatus, UserCouponStatus.USED)
                .set(UserCoupon::getUsedTime, now)
                .update();
        if (!success) {
            return;
        }
        // 3.3更新优惠券已使用数量
        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .distinct()
                .collect(Collectors.toList());
        int rows = couponMapper.incrUsedNum(couponIds, 1);
        if (rows < 1) {
            throw new DbException("更新优惠券使用数量失败");
        }
    }

    private void saveUserCoupon(Long userId, Coupon coupon) {
        // 1.基础信息
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        // 2.有效期信息
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if(termBeginTime == null){
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        userCoupon.setTermBeginTime(termBeginTime);
        userCoupon.setTermEndTime(termEndTime);
        // 3.保存
        save(userCoupon);
    }
}
