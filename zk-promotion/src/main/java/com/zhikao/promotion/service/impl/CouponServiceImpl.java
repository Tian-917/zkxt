package com.zhikao.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.api.cache.CategoryCache;
import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.dto.course.CourseSimpleInfoDTO;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.exceptions.BizIllegalException;
import com.zhikao.common.utils.*;
import com.zhikao.promotion.constants.PromotionConstants;
import com.zhikao.promotion.domain.dto.CouponFormDTO;
import com.zhikao.promotion.domain.dto.CouponIssueFormDTO;
import com.zhikao.promotion.domain.po.Coupon;
import com.zhikao.promotion.domain.po.CouponScope;
import com.zhikao.promotion.domain.po.ExchangeCode;
import com.zhikao.promotion.domain.po.UserCoupon;
import com.zhikao.promotion.domain.query.CouponQuery;
import com.zhikao.promotion.domain.vo.CouponDetailVO;
import com.zhikao.promotion.domain.vo.CouponPageVO;
import com.zhikao.promotion.domain.vo.CouponScopeVO;
import com.zhikao.promotion.domain.vo.CouponVO;
import com.zhikao.promotion.enums.CouponStatus;
import com.zhikao.promotion.enums.ObtainType;
import com.zhikao.promotion.enums.UserCouponStatus;
import com.zhikao.promotion.mapper.CouponMapper;
import com.zhikao.promotion.service.ICouponScopeService;
import com.zhikao.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.promotion.service.IExchangeCodeService;
import com.zhikao.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final ICouponScopeService scopeService;

    private final IExchangeCodeService exchangeCodeService;

    private final IUserCouponService userCouponService;

    private final StringRedisTemplate stringRedisTemplate;

    private final CategoryCache categoryCache;

    private final CourseClient courseClient;

    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // 1.保存优惠券
        // 1.1转PO
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        // 1.2保存
        save(coupon);
        if(!Boolean.TRUE.equals(dto.getSpecific())){
            // 没有限定范围
            return;
        }
        Long couponId = coupon.getId();
        // 2.保存限定范围
        List<Long> scopes = dto.getScopes();
        if(CollUtils.isEmpty(scopes)){
            throw new BadRequestException("限定范围不能为空");
        }
        // 2.1转PO，目前只支持限定课程分类范围，type固定为1
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId).setType(1))
                .collect(Collectors.toList());
        scopeService.saveBatch(list);
    }

    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Integer type = query.getType();
        Integer status = query.getStatus();
        String name = query.getName();
        // 1.分页查询
        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        // 2.处理VO
        List<Coupon> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        // 3.返回
        List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
        return PageDTO.of(page, list);
    }

    @Override
    public CouponDetailVO queryCouponById(Long id) {
        // 1.根据Id查询优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            return null;
        }
        // 2.封装VO
        CouponDetailVO vo = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        // 3.查询并封装使用范围
        if (!Boolean.TRUE.equals(coupon.getSpecific())) {
            return vo;
        }
        List<CouponScope> scopes = scopeService.lambdaQuery()
                .eq(CouponScope::getCouponId, id)
                .list();
        if (CollUtils.isEmpty(scopes)) {
            return vo;
        }
        List<CouponScopeVO> scopeVOS = new ArrayList<>(scopes.size());
        Map<Long, String> courseNames = queryCourseNames(scopes);
        for (CouponScope scope : scopes) {
            Long bizId = scope.getBizId();
            String name = String.valueOf(bizId);
            // type=1：分类范围；type=2：课程范围；为空时兼容历史数据，按分类处理
            int type = scope.getType() == null ? 1 : scope.getType();
            if (type == 1) {
                try {
                    name = categoryCache.getNameByLv3Id(bizId);
                } catch (Exception e) {
                    log.warn("查询分类名称失败，scopeId={}", bizId, e);
                }
            } else if (type == 2) {
                name = courseNames.getOrDefault(bizId, name);
            }
            scopeVOS.add(new CouponScopeVO(bizId, name));
        }
        vo.setScopes(scopeVOS);
        return vo;
    }

    private Map<Long, String> queryCourseNames(List<CouponScope> scopes) {
        List<Long> courseIds = scopes.stream()
                .filter(scope -> scope.getType() != null && scope.getType() == 2)
                .map(CouponScope::getBizId)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(courseIds)) {
            return Collections.emptyMap();
        }
        try {
            List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
            if (CollUtils.isEmpty(courseInfos)) {
                return Collections.emptyMap();
            }
            return courseInfos.stream().collect(Collectors.toMap(
                    CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
        } catch (Exception e) {
            log.warn("查询课程名称失败，courseIds={}", courseIds, e);
            return Collections.emptyMap();
        }
    }

    @Override
    @Transactional
    public void deleteCouponById(Long id) {
        // 1.查询优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            return;
        }
        // 2.发放中的优惠券不允许删除
        if (coupon.getStatus() == CouponStatus.ISSUING) {
            throw new BizIllegalException("发放中的优惠券不能删除");
        }
        // 3.已经发放过用户券的优惠券不允许删除，避免用户券失去优惠券模板
        if (coupon.getIssueNum() != null && coupon.getIssueNum() > 0) {
            throw new BizIllegalException("优惠券已发放，不能删除");
        }
        // 4.条件删除，防止与定时发放任务并发时删除正在发放的优惠券
        boolean success = remove(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getId, id)
                .ne(Coupon::getStatus, CouponStatus.ISSUING.getValue())
                .and(w -> w.isNull(Coupon::getIssueNum).or().le(Coupon::getIssueNum, 0)));
        if (!success) {
            throw new BizIllegalException("优惠券状态不允许删除");
        }
        // 5.级联清理使用范围、未兑换的兑换码和缓存
        scopeService.remove(new LambdaQueryWrapper<CouponScope>()
                .eq(CouponScope::getCouponId, id));
        exchangeCodeService.remove(new LambdaQueryWrapper<ExchangeCode>()
                .eq(ExchangeCode::getExchangeTargetId, id));
        stringRedisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
        stringRedisTemplate.delete(PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + id);
    }

    @Override
    @Transactional
    public void updateCouponById(CouponFormDTO dto) {
        // 1.获取优惠券id
        Long couponId = dto.getId();
        // 2.更新优惠券本身
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        updateById(coupon);
        // 3.更新限定范围
        Boolean specific = dto.getSpecific();
        if (specific == null) {
            // 未提交是否限定范围时，保持原有限定范围不变
            return;
        }
        // 3.1.先删除旧的使用范围，再按需保存新的范围
        scopeService.remove(new LambdaQueryWrapper<CouponScope>()
                .eq(CouponScope::getCouponId, couponId));
        if (!specific) {
            return;
        }
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BadRequestException("限定范围不能为空");
        }
        // 3.2.保存新的限定范围，目前只支持限定课程分类范围，type固定为1
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId).setType(1))
                .collect(Collectors.toList());
        scopeService.saveBatch(list);
    }

    @Override
    @Transactional
    public void beginIssue(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2.判断优惠券状态，是否时暂停还是待发放
        if(coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE){
            throw new BizIllegalException("优惠券状态错误！");
        }
        // 3.判断是否为立即发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        // 开始发放的时间为null或者开始发放时间在now之前，都代表立刻发放
        boolean isBegin = issueBeginTime == null || issueBeginTime.isBefore(now);
        // 4.更新优惠券
        // 4.1拷贝属性到po
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        // 4.2更新状态
        if(isBegin){
            c.setStatus(CouponStatus.ISSUING);
            c.setIssueBeginTime(now);
        }else {
            c.setStatus(CouponStatus.UN_ISSUE);
        }
        // 4.3更新到数据库中
        updateById(c);
        // 5.添加缓存
        if(isBegin){
            coupon.setIssueBeginTime(c.getIssueBeginTime());
            coupon.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupon);
        }
        // 6.判断是否需要生成兑换码，优惠券类型必须是兑换码，优惠券状态必须是待发放
        if(coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT){
            coupon.setIssueEndTime(c.getIssueEndTime());
            exchangeCodeService.asyncGenerateCode(coupon);
        }
    }

    private void cacheCouponInfo(Coupon coupon) {
        // 1.组织数据
        // 缓存中的 totalNum 表示剩余库存：总量 - 已发放数量，避免暂停后重新发放时库存被重置
        int totalNum = coupon.getTotalNum() == null ? 0 : coupon.getTotalNum();
        int issueNum = coupon.getIssueNum() == null ? 0 : coupon.getIssueNum();
        int remainNum = Math.max(totalNum - issueNum, 0);
        Map<String,String> map = new HashMap<>(4);
        map.put("issueBeginTime", DateUtils.formatNormal(coupon.getIssueBeginTime()));
        map.put("issueEndTime", DateUtils.formatNormal(coupon.getIssueEndTime()));
        map.put("totalNum", String.valueOf(remainNum));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));
        // 2.写缓存
        stringRedisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupon.getId(),map);
    }

    @Override
    public List<CouponVO> queryIssuingCoupons() {
        // 1.查询发放中的优惠券列表
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if(CollUtils.isEmpty(coupons)){
            return CollUtils.emptyList();
        }
        // 2.统计当前用户已经领取的优惠券信息
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        // 2.1查询当前用户已经领取的优惠券数据
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        // 2.2统计当前用户已经领取的优惠券数量
        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.3统计当前用户已经领取并且未使用的优惠券数量
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(userCoupon -> userCoupon.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 3.封装VO结果
        List<CouponVO> list = new ArrayList<>(coupons.size());
        for (Coupon coupon : coupons) {
            // 3.1拷贝PO属性到VO
            CouponVO vo = BeanUtils.copyBean(coupon, CouponVO.class);
            list.add(vo);
            // 3.2是否可以领取：已经被领取的数量 < 优惠券的总数量 && 用户已经领取的数量 < 每人限领数量
            vo.setAvailable(
                    coupon.getIssueNum() < coupon.getTotalNum() &&
                            issuedMap.getOrDefault(coupon.getId(),0L) < coupon.getUserLimit()
            );
            // 3.3是否可以使用：用户已经领取并且未使用的优惠券 > 0
            vo.setReceived(unusedMap.getOrDefault(coupon.getId(),0L) > 0);
        }
        return list;
    }

    @Override
    @Transactional
    public void issueCouponByPage(int page, int size) {
        // 查询已到发放时间、但还未开始发放的优惠券
        LocalDateTime now = LocalDateTime.now();
        Page<Coupon> couponPage = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .le(Coupon::getIssueBeginTime, now)
                .gt(Coupon::getIssueEndTime, now)
                .page(new Page<>(page, size));
        if (CollUtils.isEmpty(couponPage.getRecords())) {
            return;
        }
        beginIssueBatch(couponPage.getRecords());
    }

    @Override
    @Transactional
    public void beginIssueBatch(List<Coupon> coupons) {
        if (CollUtils.isEmpty(coupons)) {
            return;
        }
        // 1.批量更新状态，限定原状态为未开始，避免多分片重复处理
        List<Long> ids = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        boolean success = lambdaUpdate()
                .in(Coupon::getId, ids)
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .set(Coupon::getStatus, CouponStatus.ISSUING)
                .update();
        if (!success) {
            return;
        }
        // 2.写入领取缓存
        coupons.forEach(this::cacheCouponInfo);
    }

    @Override
    public void finishIssuingCoupons() {
        // 已过发放结束时间、仍处于未开始或发放中的优惠券，统一置为已结束
        lambdaUpdate()
                .in(Coupon::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING)
                .lt(Coupon::getIssueEndTime, LocalDateTime.now())
                .set(Coupon::getStatus, CouponStatus.FINISHED)
                .update();
    }

    @Override
    @Transactional
    public void pauseIssue(Long id) {
        // 1.查询优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        // 2.未开始或发放中的优惠券才能暂停
        CouponStatus status = coupon.getStatus();
        if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING) {
            throw new BizIllegalException("优惠券状态错误");
        }
        // 3.条件更新，避免并发下重复暂停
        boolean success = lambdaUpdate()
                .eq(Coupon::getId, id)
                .in(Coupon::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING)
                .set(Coupon::getStatus, CouponStatus.PAUSE)
                .update();
        if (!success) {
            log.warn("优惠券暂停失败或已被其他请求暂停，id={}", id);
        }

        // 4.删除缓存
        stringRedisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
    }

}
