package com.zhikao.trade.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.pay.sdk.dto.PayResultDTO;
import com.zhikao.trade.constants.OrderCancelReason;
import com.zhikao.trade.domain.dto.PlaceOrderDTO;
import com.zhikao.trade.domain.po.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.trade.domain.po.OrderDetail;
import com.zhikao.trade.domain.query.OrderPageQuery;
import com.zhikao.trade.domain.vo.OrderConfirmVO;
import com.zhikao.trade.domain.vo.OrderPageVO;
import com.zhikao.trade.domain.vo.OrderVO;
import com.zhikao.trade.domain.vo.PlaceOrderResultVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 订单 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IOrderService extends IService<Order> {

    PlaceOrderResultVO placeOrder(PlaceOrderDTO placeOrderDTO);

    @Transactional
    void saveOrderAndDetails(Order order, List<OrderDetail> orderDetails);

    void cancelOrder(Long orderId, OrderCancelReason reason);

    void deleteOrder(Long id);

    PageDTO<OrderPageVO> queryMyOrderPage(OrderPageQuery pageQuery);

    OrderVO queryOrderById(Long id);

    PlaceOrderResultVO queryOrderStatus(Long orderId);

    void handlePaySuccess(PayResultDTO payResult);

    PlaceOrderResultVO enrolledFreeCourse(Long courseId);

    OrderConfirmVO prePlaceOrder(List<Long> courseIds);

}
