package com.example.item.demo35.service;

import com.example.item.demo35.entity.Order;
import com.example.item.demo35.entity.OrderItem;
import com.example.item.demo35.entity.Payment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface OrderService {

    Order getOrderById(Integer orderId);

    Object createOrder(Integer userId, Map<String, Object> orderRequest);

    List<Order> getOrderList(Integer userId, Integer status, Integer page, Integer pageSize);

    Order getOrderDetail(Integer userId, String orderNo);

    boolean cancelOrder(Integer userId, String orderNo, String reason);

    boolean confirmOrder(Integer userId, String orderNo);

    boolean requestRefund(Integer userId, String orderNo, String reason);

    boolean payOrder(Integer userId, String orderNo, String paymentMethod);

    List<OrderItem> getOrderItemsByOrderId(Integer orderId);

    Order getOrderByOrderNo(String orderNo);

    boolean updatePayStatus(String orderNo, String alipayTradeNo, Date paymentTime);

    void updateQrCode(String orderNo, String qrCode);

    String getProductTitleByOrderNo(String orderNo);

    Payment getPaymentByOrderNo(String orderNo);

    void savePendingPayment(String orderNo, Integer userId, BigDecimal amount, String paymentMethod);

    void markPaymentClosed(String orderNo, String transactionId);

    Map<String, Object> queryRefundStatus(Integer userId, String orderNo);

    boolean approveRefund(String orderNo, String reason);

    boolean rejectRefund(String orderNo, String reason);

    Map<String, Object> queryRefundStatusForAdmin(String orderNo);
}
