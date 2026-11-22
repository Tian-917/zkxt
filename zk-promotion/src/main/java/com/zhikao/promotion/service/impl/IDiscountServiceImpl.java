package com.zhikao.promotion.service.impl;

import com.zhikao.api.dto.promotion.CouponDiscountDTO;
import com.zhikao.api.dto.promotion.OrderCouponDTO;
import com.zhikao.api.dto.promotion.OrderCourseDTO;
import com.zhikao.common.exceptions.BizIllegalException;
import com.zhikao.common.utils.BeanUtils;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.CouponScope;
import com.zhikao.promotion.domain.po.UserCoupon;
import com.zhikao.promotion.enums.UserCouponStatus;
import com.zhikao.promotion.mapper.UserCouponMapper;
import com.zhikao.promotion.service.ICouponScopeService;
import com.zhikao.promotion.service.ICouponService;
import com.zhikao.promotion.service.IDiscountService;
import com.zhikao.promotion.service.IUserCouponService;
import com.zhikao.promotion.strategy.discount.Discount;
import com.zhikao.promotion.strategy.discount.DiscountStrategy;
import com.zhikao.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IDiscountServiceImpl implements IDiscountService {

    private final UserCouponMapper userCouponMapper;

    private final ICouponScopeService couponScopeService;

    private final ICouponService couponService;

    private final IUserCouponService userCouponService;

    private final Executor discountSolutionExecutor;

    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        // 0.课程参数为空直接返回，否则限定范围的券会被当成不限范围
        if (CollUtils.isEmpty(orderCourses)
                || orderCourses.stream().anyMatch(c -> c == null || c.getId() == null)) {
            log.warn("计算优惠方案时课程参数为空，直接返回空方案");
            return Collections.emptyList();
        }
        // 1.查询我的所有可用的优惠券
        List<Coupon> coupons = userCouponMapper.queryMyCoupons(UserContext.getUser());
        if(CollUtils.isEmpty(coupons)){
            return Collections.emptyList();
        }
        // 2.初筛
        // 2.1计算订单总价
        int totalAmount = orderCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        // 2.2筛选可用券
        List<Coupon> availableCoupons = coupons.stream()
                .filter(c-> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalAmount,c))
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(availableCoupons)){
            return Collections.emptyList();
        }
        // 3.排列组合出所有方案
        // 3.1细筛（找出每一个优惠券的可用的课程，判断课程总价是否达到优惠券的使用需求）
        Map<Coupon,List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(availableCoupons,orderCourses);
        if (CollUtils.isEmpty(availableCouponMap)){
            return Collections.emptyList();
        }
        // 3.2排列组合
        availableCoupons = new ArrayList<>(availableCouponMap.keySet());
        List<List<Coupon>> solutions = PermuteUtil.permute(availableCoupons);
        // 3.3.添加单券的方案
        for (Coupon c : availableCoupons) {
            solutions.add(List.of(c));
        }
        // 4.并发计算每个方案的优惠明细
        List<CompletableFuture<CouponDiscountDTO>> futures = new ArrayList<>(solutions.size());
        for (List<Coupon> solution : solutions) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> calculateSolutionDiscount(availableCouponMap, orderCourses, solution),
                    discountSolutionExecutor
            ));
        }
        List<CouponDiscountDTO> list = new ArrayList<>(futures.size());
        for (CompletableFuture<CouponDiscountDTO> future : futures) {
            try {
                CouponDiscountDTO dto = future.get(3, TimeUnit.SECONDS);
                if (dto != null) {
                    list.add(dto);
                }
            } catch (Exception e) {
                // 单个方案算失败不影响其它方案
                log.error("优惠方案计算失败，{}", e.getMessage());
            }
        }
        // 5.筛选最优解
        return findBestSolution(list);
    }

    @Override
    public CouponDiscountDTO queryDiscountSolutionDetail(OrderCouponDTO dto) {
        // 1.检验用户是否用券
        List<OrderCourseDTO> course = dto.getCourseList();
        List<Long> userCouponIds = dto.getUserCouponIds();
        if(CollUtils.isEmpty(course) || CollUtils.isEmpty(userCouponIds)){
            //没用券，优惠为0
            return new CouponDiscountDTO();
        }
        // 2.校验用户券是否存在，是否未使用，是否在有效期内
        Long userId = UserContext.getUser();
        List<UserCoupon> userCoupons = userCouponService.listByIds(userCouponIds);
        // 2.1.校验券是否存在（去重后比较，防止传入不存在的id）
        if (userCoupons.size() != new HashSet<>(userCouponIds).size()) {
            throw new BizIllegalException("优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        for (UserCoupon userCoupon : userCoupons) {
            // 2.2.校验是否属于当前用户
            if (!userId.equals(userCoupon.getUserId())) {
                throw new BizIllegalException("优惠券不属于当前用户");
            }
            // 2.3.校验是否未使用
            if (userCoupon.getStatus() != UserCouponStatus.UNUSED) {
                throw new BizIllegalException("优惠券已使用或已失效");
            }
            // 2.4.校验是否在有效期内
            if (now.isBefore(userCoupon.getTermBeginTime()) || now.isAfter(userCoupon.getTermEndTime())) {
                throw new BizIllegalException("优惠券不在有效期内");
            }
        }
        // 3.查询该券组合是否可用
        // 3.1获取当前用户的所有的优惠券id
        List<Long> couponIds = userCoupons.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        // 3.2优惠券id与优惠券映射
        Map<Long, Coupon> couponMap = couponService.listByIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));
        // 3.3将UserCoupon转为Coupon集合，把用户券id写入creater
        List<Coupon> coupons = new ArrayList<>(userCoupons.size());
        for (UserCoupon userCoupon : userCoupons) {
            Coupon coupon = BeanUtils.copyBean(couponMap.get(userCoupon.getCouponId()), Coupon.class);
            coupon.setCreater(userCoupon.getId());
            coupons.add(coupon);
        }
        // 3.3细筛，校验每张券在当前订单中是否可用
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(coupons, course);
        if (availableCouponMap.size() != coupons.size()) {
            throw new BizIllegalException("优惠券不满足使用条件");
        }
        // 4.计算优惠明细
        return calculateSolutionDiscount(availableCouponMap, course, coupons);
    }

    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备Map记录最优解
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        // 2.遍历，筛选最优解
        for (CouponDiscountDTO solution : list) {
            // 2.1.计算当前方案的id组合
            String ids = solution.getIds().stream()
                    .sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            // 2.2.比较用券相同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
                // 当前方案优惠金额少，跳过
                continue;
            }
            // 2.3.比较金额相同时，用券数量是否最少
            best = lessCouponMap.get(solution.getDiscountAmount());
            int size = solution.getIds().size();
            if (size > 1 &&best != null && best.getIds().size() <= solution.getIds().size()) {
                // 当前方案用券更多，放弃
                continue;
            }
            // 2.4.更新最优解
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 3.求交集
        Collection<CouponDiscountDTO> bestSolutions = CollUtils.intersection(moreDiscountMap.values(), lessCouponMap.values());
        // 4.排序，按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }


    private CouponDiscountDTO calculateSolutionDiscount(
            Map<Coupon, List<OrderCourseDTO>> couponMap,
            List<OrderCourseDTO> courses, List<Coupon> solution) {
        // 1.初始化DTO
        CouponDiscountDTO dto = new CouponDiscountDTO();
        // 2.初始化折扣明细映射
        Map<Long, Integer> detailMap = courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, o -> 0));
        // 3.计算折扣
        for (Coupon coupon : solution) {
            // 3.1获取优惠券限定范围对应的课程
            List<OrderCourseDTO> availableCourses = couponMap.get(coupon);
            // 3.2计算课程总价
            int totalAmount = availableCourses.stream().mapToInt(oc -> oc.getPrice() - detailMap.get(oc.getId())).sum();
            // 3.3判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (!discount.canUse(totalAmount, coupon)) {
                //券不可用
                continue;
            }
            // 3.4计算优惠金额
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);
            // 3.5计算优惠明细
            calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
            // 3.6更新DTO数据
            dto.getIds().add(coupon.getCreater());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
        }
        // 保存每门课程的优惠明细
        dto.setDiscountDetail(detailMap);
        return dto;
    }

    private void calculateDiscountDetails(
            Map<Long, Integer> detailMap,
            List<OrderCourseDTO> courses,
            int totalAmount, int discountAmount) {
        int times = 0;
        int remainDiscount = discountAmount;
        for (OrderCourseDTO course : courses) {
            // 更新课程已计算数量
            times++;
            int discount = 0;
            // 判断是否为最后一个课程
            if(times==courses.size()){
                // 是最后课程，总折扣金额 - 之前所有商品折扣之和
                discount = remainDiscount;
            } else {
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
                discount = discountAmount * course.getPrice() / totalAmount;
                remainDiscount -= discount;
            }
            // 更新折扣明细
            detailMap.put(course.getId(), discount + detailMap.get(course.getId()));
        }
    }

    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(List<Coupon> coupons, List<OrderCourseDTO> courses) {
        Map<Coupon,List<OrderCourseDTO>> map = new HashMap<>(coupons.size());
        for (Coupon coupon : coupons) {
            // 1.找出优惠券的可用课程
            List<OrderCourseDTO> availableCourses = courses;
            if(coupon.getSpecific()){
                // 1.1限定了范围，查询券的可用范围
                List<CouponScope> scopes = couponScopeService.lambdaQuery().eq(CouponScope::getCouponId, coupon.getId()).list();
                // 1.2按范围类型分组：type=1为分类，type=2为课程，为空时兼容历史数据按分类处理
                Set<Long> cateIds = scopes.stream()
                        .filter(scope -> scope.getType() == null || scope.getType() == 1)
                        .map(CouponScope::getBizId)
                        .collect(Collectors.toSet());
                Set<Long> courseIds = scopes.stream()
                        .filter(scope -> scope.getType() != null && scope.getType() == 2)
                        .map(CouponScope::getBizId)
                        .collect(Collectors.toSet());
                // 1.3筛选课程：命中分类范围或课程范围均可
                availableCourses = courses.stream()
                        .filter(c -> cateIds.contains(c.getCateId()) || courseIds.contains(c.getId()))
                        .collect(Collectors.toList());
            }
            if(CollUtils.isEmpty(availableCourses)) {
                //没有任何可用课程，抛弃
                continue;
            }
            // 2.计算课程总价
            int totalAmount = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            // 3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (discount.canUse(totalAmount,coupon)){
                map.put(coupon,availableCourses);
            }
        }
        return map;
    }
}
