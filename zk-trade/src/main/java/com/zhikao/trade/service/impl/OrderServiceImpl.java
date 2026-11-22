package com.zhikao.trade.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.client.promotion.PromotionClient;
import com.zhikao.api.constants.CourseStatus;
import com.zhikao.api.dto.course.CourseSimpleInfoDTO;
import com.zhikao.api.dto.promotion.CouponDiscountDTO;
import com.zhikao.api.dto.promotion.OrderCouponDTO;
import com.zhikao.api.dto.promotion.OrderCourseDTO;
import com.zhikao.api.dto.trade.OrderBasicDTO;
import com.zhikao.common.autoconfigure.mq.RabbitMqHelper;
import com.zhikao.common.constants.Constant;
import com.zhikao.common.constants.MqConstants;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.exceptions.BadRequestException;
import com.zhikao.common.exceptions.BizIllegalException;
import com.zhikao.common.exceptions.DbException;
import com.zhikao.common.utils.BeanUtils;
import com.zhikao.common.utils.CollUtils;
import com.zhikao.common.utils.UserContext;
import com.zhikao.pay.sdk.dto.PayResultDTO;
import com.zhikao.trade.config.TradeProperties;
import com.zhikao.trade.constants.OrderCancelReason;
import com.zhikao.trade.constants.OrderStatus;
import com.zhikao.trade.constants.RefundStatus;
import com.zhikao.trade.constants.TradeErrorInfo;
import com.zhikao.trade.domain.dto.PlaceOrderDTO;
import com.zhikao.trade.domain.po.Order;
import com.zhikao.trade.domain.po.OrderDetail;
import com.zhikao.trade.domain.query.OrderPageQuery;
import com.zhikao.trade.domain.vo.*;
import com.zhikao.trade.mapper.OrderMapper;
import com.zhikao.trade.service.ICartService;
import com.zhikao.trade.service.IOrderDetailService;
import com.zhikao.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.zhikao.common.constants.ErrorInfo.Msg.OPERATE_FAILED;
import static com.zhikao.trade.constants.TradeErrorInfo.ORDER_ALREADY_FINISH;
import static com.zhikao.trade.constants.TradeErrorInfo.ORDER_NOT_EXISTS;

/**
 * <p>
 * 订单 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final CourseClient courseClient;
    private final IOrderDetailService detailService;
    private final ICartService cartService;
    private final TradeProperties tradeProperties;
    private final RabbitMqHelper rabbitMqHelper;
    private final PromotionClient promotionClient;

    @Override
    @Transactional
    public PlaceOrderResultVO placeOrder(PlaceOrderDTO placeOrderDTO) {
        Long userId = UserContext.getUser();
        // 1.查询课程费用信息，如果不可购买，这里直接报错
        List<CourseSimpleInfoDTO> courseInfos = getOnShelfCourse(placeOrderDTO.getCourseIds());
        // 2.封装订单信息
        Order order = new Order();
        // 2.1.计算订单金额
        Integer totalAmount = courseInfos.stream()
                .map(CourseSimpleInfoDTO::getPrice).reduce(Integer::sum).orElse(0);
        // 2.2.计算优惠金额
        int discountAmount = 0;
        List<Long> couponIds = placeOrderDTO.getCouponIds();
        Map<Long, Integer> discountDetails = Collections.emptyMap();
        if (CollUtils.isNotEmpty(couponIds)) {
            // 组装订单课程信息
            List<OrderCourseDTO> orderCourses = courseInfos.stream()
                    .map(ci -> new OrderCourseDTO()
                            .setId(ci.getId())
                            .setCateId(ci.getThirdCateId())
                            .setPrice(ci.getPrice()))
                    .collect(Collectors.toList());
            // 计算优惠明细
            OrderCouponDTO orderCouponDTO = new OrderCouponDTO(couponIds, orderCourses);
            CouponDiscountDTO discount = promotionClient.queryDiscountSolutionDetail(orderCouponDTO);
            if (discount != null) {
                discountAmount = discount.getDiscountAmount();
                if (discount.getDiscountDetail() != null) {
                    discountDetails = discount.getDiscountDetail();
                }
            }
        }
        order.setDiscountAmount(discountAmount);
        order.setCouponIds(couponIds);
        Integer realAmount = totalAmount - discountAmount;
        // 2.3.封装其它信息
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setRealAmount(realAmount);
        order.setStatus(OrderStatus.NO_PAY.getValue());
        order.setMessage(OrderStatus.NO_PAY.getProgressName());
        // 2.4.订单id
        Long orderId = placeOrderDTO.getOrderId();
        order.setId(orderId);

        // 3.封装订单详情
        List<OrderDetail> orderDetails = new ArrayList<>(courseInfos.size());
        for (CourseSimpleInfoDTO courseInfo : courseInfos) {
            orderDetails.add(packageOrderDetail(courseInfo, order, discountDetails));
        }

        // 4.写入数据库
        saveOrderAndDetails(order, orderDetails);

        // 5.删除购物车数据
        cartService.deleteCartByUserAndCourseIds(userId, placeOrderDTO.getCourseIds());

        // 6.核销优惠券
        if (CollUtils.isNotEmpty(couponIds)) {
            promotionClient.writeOffCoupon(couponIds);
        }

        // 7.构建下单结果
        return PlaceOrderResultVO.builder()
                .orderId(orderId)
                .payAmount(realAmount)
                .status(order.getStatus())
                .payOutTime(LocalDateTime.now().plusMinutes(tradeProperties.getPayOrderTTLMinutes()))
                .build();
    }

    private List<CourseSimpleInfoDTO> getOnShelfCourse(List<Long> courseIds) {
        // 1.查询课程
        List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
        LocalDateTime now = LocalDateTime.now();
        // 2.判断状态
        for (CourseSimpleInfoDTO courseInfo : courseInfos) {
            // 2.1.检查课程是否上架
            if(!CourseStatus.SHELF.equalsValue(courseInfo.getStatus())){
                throw new BizIllegalException(TradeErrorInfo.COURSE_NOT_FOR_SALE);
            }
            // 2.2.检查课程是否过期
            if(courseInfo.getPurchaseEndTime().isBefore(now)){
                throw new BizIllegalException(TradeErrorInfo.COURSE_EXPIRED);
            }
        }
        return courseInfos;
    }


    @Override
    @Transactional
    public PlaceOrderResultVO enrolledFreeCourse(Long courseId) {
        Long userId = UserContext.getUser();
        // 1.查询课程信息
        List<Long> cIds = CollUtils.singletonList(courseId);
        List<CourseSimpleInfoDTO> courseInfos = getOnShelfCourse(cIds);
        if (CollUtils.isEmpty(courseInfos)) {
            // 课程不存在
            throw new BizIllegalException(TradeErrorInfo.COURSE_NOT_EXISTS);
        }
        CourseSimpleInfoDTO courseInfo = courseInfos.get(0);
        if(!courseInfo.getFree()){
            // 非免费课程，直接报错
            throw new BizIllegalException(TradeErrorInfo.COURSE_NOT_FREE);
        }
        // 2.创建订单
        Order order = new Order();
        // 2.1.基本信息
        order.setUserId(userId);
        order.setTotalAmount(0);
        order.setDiscountAmount(0);
        order.setRealAmount(0);
        order.setStatus(OrderStatus.ENROLLED.getValue());
        order.setFinishTime(LocalDateTime.now());
        order.setMessage(OrderStatus.ENROLLED.getProgressName());
        // 2.2.订单id
        Long orderId = IdWorker.getId(order);
        order.setId(orderId);

        // 3.订单详情
        OrderDetail detail = packageOrderDetail(courseInfo, order, Collections.emptyMap());

        // 4.写入数据库
        saveOrderAndDetails(order, CollUtils.singletonList(detail));

        // 5.发送MQ消息，通知报名成功
        rabbitMqHelper.send(
                MqConstants.Exchange.ORDER_EXCHANGE,
                MqConstants.Key.ORDER_PAY_KEY,
                OrderBasicDTO.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .courseIds(cIds)
                        .finishTime(order.getFinishTime())
                        .build()
        );
        // 6.返回vo
        return PlaceOrderResultVO.builder()
                .orderId(orderId)
                .payAmount(0)
                .status(order.getStatus())
                .build();
    }

    @Override
    public OrderConfirmVO prePlaceOrder(List<Long> courseIds) {
        // 1.查询课程信息
        List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(courseInfos)) {
            throw new BizIllegalException(TradeErrorInfo.COURSE_NOT_EXISTS);
        }
        List<OrderCourseVO> courses = BeanUtils.copyList(courseInfos, OrderCourseVO.class);
        // 2.计算总价
        int total = courseInfos.stream().mapToInt(CourseSimpleInfoDTO::getPrice).sum();
        // 3.计算折扣
        List<OrderCourseDTO> orderCourses = courseInfos.stream().map(ci ->
                        new OrderCourseDTO()
                                .setId(ci.getId())
                                .setCateId(ci.getThirdCateId())
                                .setPrice(ci.getPrice()))
                .collect(Collectors.toList());
        List<CouponDiscountDTO> discountSolution = promotionClient.findDiscountSolution(orderCourses);
        // 4.生成订单id
        long orderId = IdWorker.getId();
        // 5.组织返回
        OrderConfirmVO vo = new OrderConfirmVO();
        vo.setOrderId(orderId);
        vo.setTotalAmount(total);
        vo.setDiscounts(discountSolution);
        vo.setCourses(courses);
        return vo;
    }

    private OrderDetail packageOrderDetail(CourseSimpleInfoDTO courseInfo, Order order, Map<Long, Integer> discountDetails) {
        OrderDetail detail = new OrderDetail();
        detail.setUserId(order.getUserId());
        detail.setOrderId(order.getId());
        detail.setStatus(order.getStatus());
        detail.setCourseId(courseInfo.getId());
        detail.setPrice(courseInfo.getPrice());
        detail.setCoverUrl(courseInfo.getCoverUrl());
        detail.setName(courseInfo.getName());
        detail.setValidDuration(courseInfo.getValidDuration());
        // 课程有效期统一为2030年过期
        detail.setCourseExpireTime(Constant.UNIFIED_EXPIRE_TIME);
        // 该课程分摊到的优惠金额
        int discountAmount = discountDetails.getOrDefault(courseInfo.getId(), 0);
        detail.setDiscountAmount(discountAmount);
        detail.setRealPayAmount(courseInfo.getPrice() - discountAmount);
        return detail;
    }

    @Override
    @Transactional
    public void saveOrderAndDetails(Order order, List<OrderDetail> orderDetails) {
        // 4.1.写订单
        boolean success = save(order);
        if (!success) {
            throw new DbException(TradeErrorInfo.PLACE_ORDER_FAILED);
        }
        // 4.2.写订单详情
        if(orderDetails.size() == 1){
            success = detailService.save(orderDetails.get(0));
        }else {
            success = detailService.saveBatch(orderDetails);
        }
        if (!success) {
            throw new DbException(TradeErrorInfo.PLACE_ORDER_FAILED);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, OrderCancelReason reason) {
        Long userId = UserContext.getUser();
        // 1.查询订单
        Order order = getById(orderId);
        // 用户手动取消时校验归属；系统超时取消没有用户上下文，跳过归属校验
        if (order == null
                || (reason == OrderCancelReason.USER_CANCEL && !Objects.equals(userId, order.getUserId()))) {
            throw new BadRequestException(ORDER_NOT_EXISTS);
        }
        // 2.判断订单状态是否已经取消，幂等判断
        if(OrderStatus.CLOSED.equalsValue(order.getStatus())){
           // 订单已经取消，无需重复操作
           return;
        }
        // 3.判断订单是否未支付，只有未支付订单才可以取消
        if(!OrderStatus.NO_PAY.equalsValue(order.getStatus())){
            // 超时取消时订单可能已经被支付，直接结束，避免消息重试
            if (reason == OrderCancelReason.TIME_OUT) {
                return;
            }
            throw new BizIllegalException(ORDER_ALREADY_FINISH);
        }
        // 4.可以更新订单状态为取消了
        boolean success = lambdaUpdate()
                .set(Order::getStatus, OrderStatus.CLOSED.getValue())
                .set(Order::getMessage, reason.getMessage())
                .set(Order::getCloseTime, LocalDateTime.now())
                .eq(Order::getStatus, OrderStatus.NO_PAY.getValue())
                .eq(Order::getId, orderId)
                .update();
        if (!success) {
            return;
        }
        // 5.更新订单条目的状态
        detailService.updateStatusByOrderId(orderId, OrderStatus.CLOSED.getValue());
        // 6.退还优惠券
        if (CollUtils.isNotEmpty(order.getCouponIds())) {
            // 1.超时取消没有用户上下文，补上订单所属用户
            boolean needRestore = UserContext.getUser() == null;
            if (needRestore) {
                UserContext.setUser(order.getUserId());
            }
            try {
                promotionClient.refundCoupon(order.getCouponIds());
            } finally {
                if (needRestore) {
                    UserContext.removeUser();
                }
            }
        }
    }

    @Override
    public void deleteOrder(Long id) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.查询订单
        Order order = getById(id);
        if (order == null) {
            return;
        }
        // 3.判断订单所属用户与当前登录用户是否一致
        if(!Objects.equals(userId, order.getUserId())){
            // 不一致，说明不是当前用户的订单，直接结束
            throw new BadRequestException("不能删除他人订单");
        }
        // 4.删除订单
        boolean success = removeById(id);
        if (!success) {
            throw new DbException(OPERATE_FAILED);
        }
    }

    @Override
    public PageDTO<OrderPageVO> queryMyOrderPage(OrderPageQuery pageQuery) {
        Long userId = UserContext.getUser();
        // 1.分页排序条件
        Page<Order> p = pageQuery.toMpPageDefaultSortByCreateTimeDesc();
        // 2.分页查询订单
        Integer status = pageQuery.getStatus();
        Page<Order> page = lambdaQuery()
                .eq(status != null, Order::getStatus, status)
                .eq(Order::getUserId, userId)
                .page(p);
        // 3.数据判断
        List<Order> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(p);
        }
        // 4.查询订单明细信息
        List<Long> orderIds = records.stream().map(Order::getId).collect(Collectors.toList());
        // 4.1.根据订单id查询订单明细
        List<OrderDetail> details = detailService.queryByOrderIds(orderIds);
        // 4.2.将订单明细分组，key是订单id，值是订单下的所有detail
        Map<Long, List<OrderDetailVO>> detailMap = details.stream()
                .map(od -> {
                    OrderDetailVO v = BeanUtils.copyBean(od, OrderDetailVO.class);
                    // 1.已支付且没有进行中的退款申请才可退
                    v.setCanRefund(OrderStatus.canRefund(od.getStatus())
                            && !RefundStatus.inProgress(v.getRefundStatus()));
                    return v;
                })
                .collect(Collectors.groupingBy(OrderDetailVO::getOrderId));
        // 5.转换VO
        List<OrderPageVO> list = new ArrayList<>(orderIds.size());
        for (Order record : records) {
            // 5.1.转换订单
            OrderPageVO v = BeanUtils.toBean(record, OrderPageVO.class);
            list.add(v);
            // 5.2.写入vo
            v.setDetails(detailMap.get(record.getId()));
            v.setStatusDesc(OrderStatus.desc(v.getStatus()));
        }
        return PageDTO.of(page, list);
    }

    @Override
    public OrderVO queryOrderById(Long id) {
        // 1.查询订单
        Order order = getById(id);
        if (order == null || !Objects.equals(UserContext.getUser(), order.getUserId())) {
            throw new BadRequestException(ORDER_NOT_EXISTS);
        }
        // 2.查询订单详情
        List<OrderDetail> details = detailService.queryByOrderId(id);
        // 3.转换VO
        // 3.1.订单
        OrderVO vo = BeanUtils.toBean(order, OrderVO.class);
        vo.setStatusDesc(OrderStatus.desc(order.getStatus()));
        // 3.2.订单详情
        List<OrderDetailVO> dvs = BeanUtils.copyList(details, OrderDetailVO.class, (d, v) -> v.setCanRefund(
                // 订单已经支付，且 退款没有在进行中，标记为可退款状态
                OrderStatus.canRefund(d.getStatus()) && !RefundStatus.inProgress(v.getRefundStatus())
        ));
        // 3.3.优惠券规则
        String couponDesc = queryCouponDesc(order.getCouponIds());
        if (couponDesc != null) {
            vo.setCouponDesc(couponDesc);
            // 订单明细也展示同一批优惠券规则
            dvs.forEach(d -> d.setCouponDesc(couponDesc));
        }
        vo.setDetails(dvs);
        // 3.4.订单进度
        vo.setProgressNodes(detailService.packageProgressNodes(order, null));
        return vo;
    }

    @Override
    public PlaceOrderResultVO queryOrderStatus(Long orderId) {
        // 1.查询订单
        Order order = getById(orderId);
        if (order == null || !Objects.equals(UserContext.getUser(), order.getUserId())) {
            throw new BadRequestException(ORDER_NOT_EXISTS);
        }
        // 2.计算超时时间
        LocalDateTime outTime = null;
        if(OrderStatus.NO_PAY.equalsValue(order.getStatus())){
            outTime = order.getCreateTime().plusMinutes(tradeProperties.getPayOrderTTLMinutes());
        }
        // 3.封装结果
        return PlaceOrderResultVO.builder()
                .orderId(orderId)
                .payAmount(order.getRealAmount())
                .status(order.getStatus())
                .payOutTime(outTime)
                .build();
    }

    /**
     * 查询订单优惠券规则，多个规则使用 / 拼接
     */
    private String queryCouponDesc(List<Long> couponIds) {
        if (CollUtils.isEmpty(couponIds)) {
            return null;
        }
        List<String> rules = promotionClient.queryDiscountRules(couponIds);
        return CollUtils.isEmpty(rules) ? null : String.join("/", rules);
    }

    @Override
    @Transactional
    public void handlePaySuccess(PayResultDTO payResult) {
        // 1.查询订单
        Order order = getById(payResult.getBizOrderId());
        if (order == null) {
            return;
        }
        // 2.更新订单状态
        Order o = new Order();
        o.setId(order.getId());
        o.setStatus(OrderStatus.PAYED.getValue());
        o.setPayTime(payResult.getSuccessTime());
        o.setPayChannel(payResult.getPayChannel());
        o.setPayOrderNo(payResult.getPayOrderNo());
        o.setMessage("用户支付成功");
        updateById(o);
        // 3.更新订单条目
        detailService.markDetailSuccessByOrderId(o.getId(), payResult.getPayChannel(), payResult.getSuccessTime());
        // 4.查询订单包含的课程信息
        List<Long> cIds = detailService.queryCourseIdsByOrderId(o.getId());
        // 5.发送MQ消息，通知报名成功
        rabbitMqHelper.send(
                MqConstants.Exchange.ORDER_EXCHANGE,
                MqConstants.Key.ORDER_PAY_KEY,
                OrderBasicDTO.builder()
                        .orderId(o.getId()).userId(order.getUserId()).courseIds(cIds)
                        .finishTime(o.getPayTime())
                        .build()
        );
    }

}
