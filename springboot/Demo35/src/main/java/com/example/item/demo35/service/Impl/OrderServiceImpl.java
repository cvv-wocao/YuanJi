package com.example.item.demo35.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.example.item.demo35.Dao.AddressDao;
import com.example.item.demo35.Dao.CartDao;
import com.example.item.demo35.Dao.OrderDao;
import com.example.item.demo35.Dao.OrderItemDao;
import com.example.item.demo35.Dao.PaymentDao;
import com.example.item.demo35.Dao.ProductDao;
import com.example.item.demo35.config.AlipayConfig;
import com.example.item.demo35.config.BigDataConfig;
import com.example.item.demo35.entity.*;
import com.example.item.demo35.service.OrderService;
import com.example.item.demo35.util.Result;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_STATUS_PENDING_PAYMENT = 0;
    private static final int ORDER_STATUS_PENDING_SHIPMENT = 1;
    private static final int ORDER_STATUS_PENDING_RECEIPT = 2;
    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final int ORDER_STATUS_CANCELLED = 4;
    private static final int ORDER_STATUS_REFUNDING = 5;
    private static final int ORDER_STATUS_REFUNDED = 6;
    private static final int PAYMENT_STATUS_PENDING = 0;
    private static final int PAYMENT_STATUS_SUCCESS = 1;
    private static final int PAYMENT_STATUS_FAILED = 2;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private CartDao cartDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private AddressDao addressDao;

    @Autowired
    private AlipayConfig alipayConfig;

    //==============================Kafka========================
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Gson gson = new Gson();

    @Autowired
    private BigDataConfig bigDataConfig;


    private void sendOrderEvent(String eventType, Order order) {
        if (!bigDataConfig.isEnabled()) {
            return; // 如果大数据功能未启用，不发送 Kafka 消息
        }
        try {
            // 构建一个简单的 JSON 消息
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());
            event.put("order", order);
            String message = gson.toJson(event);
            kafkaTemplate.send("order-change", message);
            System.out.println("发送 Kafka 消息: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //==============================Kafka========================


    @Override
    public Order getOrderById(Integer orderId) {
        return orderDao.selectById(orderId);
    }

    @Override
    public List<OrderItem> getOrderItemsByOrderId(Integer orderId) {
        return orderItemDao.selectByOrderId(orderId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Order createOrder(Integer userId, Map<String, Object> orderRequest) {
        String orderNo = generateOrderNo();
        List<Map<String, Object>> items = (List<Map<String, Object>>) orderRequest.get("items");
        Integer addressId = (Integer) orderRequest.get("addressId");
        String remark = (String) orderRequest.get("remark");

        Address address = addressDao.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            return null;
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Integer productId = (Integer) item.get("productId");
            Integer quantity = (Integer) item.get("quantity");
            String specification = (String) item.get("specification");

            Product product = productDao.selectById(productId);
            if (product == null || product.getStock() < quantity) {
                return null;
            }

            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(productId);
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getThumbnail());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setTotalPrice(itemTotal);
            orderItem.setSpecification(specification);
            orderItems.add(orderItem);

            productDao.updateStock(productId, quantity);
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPaymentAmount(totalAmount);
        order.setStatus(ORDER_STATUS_PENDING_PAYMENT);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setRemark(remark);

//        if (!orderDao.insert(order)) {
//            return null;
//        }
        boolean result = orderDao.insert(order);
        if(result){
            System.out.println("创建订单成功："+order);
            sendOrderEvent("CreateOrder",order);
            System.out.println("OrderServiceImpl阶段，已发送kafka消息");
        }else {
            return null;
        }
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemDao.insert(orderItem);
        }

        cartDao.deleteByUserId(userId);
        return order;
    }

    @Override
    public List<Order> getOrderList(Integer userId, Integer status, Integer page, Integer pageSize) {
        return orderDao.selectByUserId(userId, status);
    }

    @Override
    public Order getOrderDetail(Integer userId, String orderNo) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order != null && order.getUserId().equals(userId)) {
            return order;
        }
        return null;
    }

    @Override
    public boolean cancelOrder(Integer userId, String orderNo, String reason) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order != null && order.getUserId().equals(userId) && order.getStatus() == ORDER_STATUS_PENDING_PAYMENT) {
            order.setStatus(ORDER_STATUS_CANCELLED);
            order.setCancelReason(reason);
            boolean result = orderDao.update(order);
            if (result) {
                sendOrderEvent("CancelOrder", order);
                System.out.println("OrderServiceImpl阶段，已发送kafka消息");
            }
            return result;
        }
        return false;
    }

    @Override
    public boolean confirmOrder(Integer userId, String orderNo) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order != null && order.getUserId().equals(userId) && order.getStatus() == ORDER_STATUS_PENDING_RECEIPT) {
            order.setStatus(ORDER_STATUS_COMPLETED);
            order.setReceiveTime(new Date());
            boolean result = orderDao.update(order);
            if (result) {
                sendOrderEvent("ConfirmOrder", order);
                System.out.println("OrderServiceImpl阶段，已发送kafka消息（确认收货）");
            }
            return result;
        }
        return false;
    }

    @Override
    public boolean requestRefund(Integer userId, String orderNo, String reason) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId) || !canStartRefund(order)) {
            return false;
        }
        order.setStatus(ORDER_STATUS_REFUNDING);
        order.setCancelReason(reason);
        boolean result = orderDao.update(order);
        if (result) {
            sendOrderEvent("RequestRefund", order);
            System.out.println("OrderServiceImpl阶段，已发送kafka消息（申请退款）");
        }
        return result;
    }

    @Override
    public boolean payOrder(Integer userId, String orderNo, String paymentMethod) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (!canPayOrder(order, userId)) {
            return false;
        }

        try {
            boolean paymentSuccess = callThirdPartyPayment(paymentMethod, order);
            if (!paymentSuccess) {
                return false;
            }

            Date now = new Date();
            order.setStatus(ORDER_STATUS_PENDING_SHIPMENT);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentTime(now);
            if (!orderDao.update(order)) {
                return false;
            }

            upsertPayment(order, PAYMENT_STATUS_SUCCESS, UUID.randomUUID().toString(), now, paymentMethod);

            // 添加 Kafka 消息
            sendOrderEvent("PayOrder", order);
            System.out.println("OrderServiceImpl阶段，已发送kafka消息（支付成功）");

            return true;
        } catch (Exception e) {
            throw new RuntimeException("支付失败: " + e.getMessage(), e);
        }
    }

    private boolean callThirdPartyPayment(String paymentMethod, Order order) {
        try {
            log.info("Call mock {} payment, amount={}", paymentMethod, order.getPaymentAmount());
            Thread.sleep(1000);
            return true;
        } catch (Exception e) {
            log.error("Mock payment failed", e);
            return false;
        }
    }

    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return sdf.format(new Date()) + (int) (Math.random() * 1000);
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        return orderDao.selectByOrderNo(orderNo);
    }

    @Override
    public boolean updatePayStatus(String orderNo, String alipayTradeNo, Date paymentTime) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return false;
        }

        Date paidAt = paymentTime != null ? paymentTime : new Date();
        boolean statusChanged = false;
        if (order.getStatus() == ORDER_STATUS_PENDING_PAYMENT) {
            order.setStatus(ORDER_STATUS_PENDING_SHIPMENT);
            order.setPaymentMethod("alipay");
            order.setPaymentTime(paidAt);
            if (!orderDao.update(order)) {
                return false;
            }
            statusChanged = true;
        }

        upsertPayment(order, PAYMENT_STATUS_SUCCESS, alipayTradeNo, paidAt, "alipay");
        if (statusChanged) {
            sendOrderEvent("PayOrder", order);
            System.out.println("OrderServiceImpl阶段，已发送kafka消息（支付宝支付成功）");
        }
        return true;
    }

    @Override
    public void updateQrCode(String orderNo, String qrCode) {
        log.info("Order {} qrCode generated: {}", orderNo, qrCode);
    }

    @Override
    public String getProductTitleByOrderNo(String orderNo) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return "商品";
        }

        List<OrderItem> items = orderItemDao.selectByOrderId(order.getId());
        if (items != null && !items.isEmpty()) {
            return items.get(0).getProductName();
        }
        return "订单-" + orderNo;
    }

    @Override
    public Payment getPaymentByOrderNo(String orderNo) {
        return paymentDao.selectByOrderNo(orderNo);
    }

    @Override
    public void savePendingPayment(String orderNo, Integer userId, BigDecimal amount, String paymentMethod) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return;
        }

        Payment payment = paymentDao.selectByOrderNo(orderNo);
        if (payment == null) {
            payment = new Payment();
            payment.setOrderNo(orderNo);
            payment.setUserId(userId);
            payment.setAmount(amount);
            payment.setPaymentMethod(paymentMethod);
            payment.setStatus(PAYMENT_STATUS_PENDING);
            paymentDao.insert(payment);
            return;
        }

        if (Integer.valueOf(PAYMENT_STATUS_SUCCESS).equals(payment.getStatus())) {
            return;
        }

        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PAYMENT_STATUS_PENDING);
        paymentDao.update(payment);
    }

    @Override
    public void markPaymentClosed(String orderNo, String transactionId) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return;
        }

        Payment payment = paymentDao.selectByOrderNo(orderNo);
        if (payment == null) {
            payment = new Payment();
            payment.setOrderNo(orderNo);
            payment.setUserId(order.getUserId());
            payment.setAmount(resolvePayAmount(order));
            payment.setPaymentMethod("alipay");
            payment.setStatus(PAYMENT_STATUS_FAILED);
            payment.setTransactionId(transactionId);
            paymentDao.insert(payment);
            return;
        }

        if (Integer.valueOf(PAYMENT_STATUS_SUCCESS).equals(payment.getStatus())) {
            return;
        }

        payment.setStatus(PAYMENT_STATUS_FAILED);
        payment.setTransactionId(transactionId);
        payment.setPaymentMethod("alipay");
        paymentDao.update(payment);
    }

    @Override
    public Map<String, Object> queryRefundStatus(Integer userId, String orderNo) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }
        return doQueryRefundStatus(order);
    }

    @Override
    public boolean approveRefund(String orderNo, String reason) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null || order.getStatus() != ORDER_STATUS_REFUNDING) {
            return false;
        }
        boolean result = submitRefund(order, buildAdminRefundReason(reason));
        if (result) {
            // 注意：submitRefund 内部可能已修改订单状态为 REFUNDED，但需要重新查询获取最新状态
            Order updatedOrder = orderDao.selectByOrderNo(orderNo);
            sendOrderEvent("ApproveRefund", updatedOrder);
            System.out.println("OrderServiceImpl阶段，已发送kafka消息（同意退款）");
        }
        return result;
    }

    @Override
    public boolean rejectRefund(String orderNo, String reason) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null || order.getStatus() != ORDER_STATUS_REFUNDING) {
            return false;
        }

        order.setStatus(resolveRefundRejectedStatus(order));
        order.setCancelReason(buildAdminRejectReason(reason));
        boolean result = orderDao.update(order);
        if (result) {
            sendOrderEvent("RejectRefund", order);
            System.out.println("OrderServiceImpl阶段，已发送kafka消息（拒绝退款）");
        }
        return result;
    }

    @Override
    public Map<String, Object> queryRefundStatusForAdmin(String orderNo) {
        Order order = orderDao.selectByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        return doQueryRefundStatus(order);
    }

    private boolean canPayOrder(Order order, Integer userId) {
        if (order == null) {
            return false;
        }
        if (!order.getUserId().equals(userId)) {
            return false;
        }
        return order.getStatus() == ORDER_STATUS_PENDING_PAYMENT;
    }

    private boolean canStartRefund(Order order) {
        return order.getStatus() == ORDER_STATUS_PENDING_SHIPMENT || order.getStatus() == ORDER_STATUS_PENDING_RECEIPT;
    }

    private boolean submitRefund(Order order, String reason) {
        String orderNo = order.getOrderNo();
        Payment payment = paymentDao.selectByOrderNo(orderNo);
        if (payment == null || payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            order.setStatus(ORDER_STATUS_REFUNDING);
            order.setCancelReason(reason);
            return orderDao.update(order);
        }

        String paymentMethod = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : order.getPaymentMethod();
        if (paymentMethod == null || !"alipay".equalsIgnoreCase(paymentMethod)) {
            order.setStatus(ORDER_STATUS_REFUNDING);
            order.setCancelReason(reason);
            return orderDao.update(order);
        }

        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("trade_no", payment.getTransactionId());
            bizContent.put("refund_amount", resolvePayAmount(order).toPlainString());
            bizContent.put("refund_reason", reason);
            bizContent.put("out_request_no", buildRefundRequestNo(orderNo));
            request.setBizContent(bizContent.toJSONString());

            AlipayTradeRefundResponse response = alipayConfig.getAlipayClient().execute(request);
            if (!response.isSuccess()) {
                log.error("Alipay refund failed, orderNo={}, subCode={}, subMsg={}", orderNo, response.getSubCode(), response.getSubMsg());
                return false;
            }

            if ("Y".equalsIgnoreCase(response.getFundChange())) {
                return markRefundSuccess(order, reason);
            }

            order.setStatus(ORDER_STATUS_REFUNDING);
            order.setCancelReason(reason);
            return orderDao.update(order);
        } catch (AlipayApiException e) {
            log.error("Alipay refund exception, orderNo={}", orderNo, e);
            return false;
        }
    }

    private Map<String, Object> doQueryRefundStatus(Order order) {
        String orderNo = order.getOrderNo();
        if (isRefundRejected(order)) {
            return buildRefundStatus(order, "REFUND_REJECTED", false, true, order.getCancelReason());
        }

        if (isRefundWaitingApproval(order)) {
            return buildRefundStatus(order, "REFUND_PENDING_APPROVAL", false, false, "退款申请已提交，等待管理员审核");
        }

        Payment payment = paymentDao.selectByOrderNo(orderNo);
        if (payment == null || payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            return buildRefundStatus(order, "REFUND_NOT_AVAILABLE", false, false, "未找到可退款的支付流水");
        }

        String paymentMethod = payment.getPaymentMethod() != null ? payment.getPaymentMethod() : order.getPaymentMethod();
        if (paymentMethod == null || !"alipay".equalsIgnoreCase(paymentMethod)) {
            return buildRefundStatus(order, "REFUND_NOT_AVAILABLE", false, false, "当前订单不是支付宝支付");
        }

        if (order.getStatus() == ORDER_STATUS_REFUNDED) {
            return buildRefundStatus(order, "REFUND_SUCCESS", true, true, "退款成功");
        }

        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("trade_no", payment.getTransactionId());
            bizContent.put("out_request_no", buildRefundRequestNo(orderNo));
            request.setBizContent(bizContent.toJSONString());

            AlipayTradeFastpayRefundQueryResponse response = alipayConfig.getAlipayClient().execute(request);
            if (!response.isSuccess()) {
                String message = response.getSubMsg() != null ? response.getSubMsg() : response.getMsg();
                return buildRefundStatus(order, "REFUND_QUERY_FAILED", false, false, message);
            }

            String refundStatus = response.getRefundStatus();
            if ("REFUND_SUCCESS".equals(refundStatus)) {
                markRefundSuccess(order, order.getCancelReason());
                order = orderDao.selectByOrderNo(orderNo);
                return buildRefundStatus(order, refundStatus, true, true, "退款成功");
            }

            if ("REFUND_PROCESSING".equals(refundStatus)) {
                if (order.getStatus() != ORDER_STATUS_REFUNDING) {
                    order.setStatus(ORDER_STATUS_REFUNDING);
                    orderDao.update(order);
                }
                return buildRefundStatus(order, refundStatus, false, false, "退款处理中");
            }

            return buildRefundStatus(order, refundStatus != null ? refundStatus : "REFUND_UNKNOWN", false, true, "退款未成功");
        } catch (AlipayApiException e) {
            log.error("Alipay refund query exception, orderNo={}", orderNo, e);
            return buildRefundStatus(order, "REFUND_QUERY_FAILED", false, false, "退款查询失败");
        }
    }

    private void upsertPayment(Order order, Integer status, String transactionId, Date paymentTime, String paymentMethod) {
        Payment payment = paymentDao.selectByOrderNo(order.getOrderNo());
        if (payment == null) {
            payment = new Payment();
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(resolvePayAmount(order));
            payment.setPaymentMethod(paymentMethod);
            payment.setStatus(status);
            payment.setTransactionId(transactionId);
            payment.setPaymentTime(paymentTime);
            paymentDao.insert(payment);
            return;
        }

        payment.setAmount(resolvePayAmount(order));
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(status);
        payment.setTransactionId(transactionId);
        payment.setPaymentTime(paymentTime);
        paymentDao.update(payment);
    }

    private BigDecimal resolvePayAmount(Order order) {
        return order.getPaymentAmount() != null ? order.getPaymentAmount() : order.getTotalAmount();
    }

    private String buildRefundRequestNo(String orderNo) {
        return orderNo + "-refund";
    }

    private String buildAdminRefundReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "管理员审核通过退款";
        }
        return "管理员审核通过退款: " + reason;
    }

    private String buildAdminRejectReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "管理员驳回退款申请";
        }
        return "管理员驳回退款: " + reason;
    }

    private boolean isRefundWaitingApproval(Order order) {
        if (order.getStatus() != ORDER_STATUS_REFUNDING) {
            return false;
        }
        String cancelReason = order.getCancelReason();
        return cancelReason == null || !cancelReason.startsWith("管理员审核通过退款");
    }

    private boolean isRefundRejected(Order order) {
        String cancelReason = order.getCancelReason();
        return cancelReason != null && cancelReason.startsWith("管理员驳回退款");
    }

    private int resolveRefundRejectedStatus(Order order) {
        return order.getShippingTime() != null ? ORDER_STATUS_PENDING_RECEIPT : ORDER_STATUS_PENDING_SHIPMENT;
    }

    private boolean markRefundSuccess(Order order, String reason) {
        order.setStatus(ORDER_STATUS_REFUNDED);
        order.setCancelReason(reason);
        return orderDao.update(order);
    }

    private Map<String, Object> buildRefundStatus(Order order, String refundStatus, boolean refunded, boolean finished, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getStatus());
        data.put("refundStatus", refundStatus);
        data.put("refunded", refunded);
        data.put("finished", finished);
        data.put("refundAmount", resolvePayAmount(order));
        data.put("message", message);
        return data;
    }
}
