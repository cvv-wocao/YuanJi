package com.example.item.demo35.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.example.item.demo35.config.AlipayConfig;
import com.example.item.demo35.entity.Order;
import com.example.item.demo35.entity.Payment;
import com.example.item.demo35.service.OrderService;
import com.example.item.demo35.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alipay")
@Slf4j
public class AlipayController {

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result createAlipayOrder(@RequestBody Map<String, String> params) {
        String orderNo = params.get("orderNo");
        if (orderNo == null || orderNo.isBlank()) {
            return Result.error("订单号不能为空");
        }
        try {
            Order order = orderService.getOrderByOrderNo(orderNo);
            if (order == null) {
                return Result.error("订单不存在");
            }
            if (order.getStatus() != 0) {
                return Result.error("订单状态不正确，无法支付");
            }

            String subject = orderService.getProductTitleByOrderNo(orderNo);
            BigDecimal amount = resolvePayAmount(order);

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(alipayConfig.getNotifyUrl());
            request.setReturnUrl(alipayConfig.getReturnUrl());

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("total_amount", amount.toString());
            bizContent.put("subject", subject);
            bizContent.put("store_id", "test_store_001");
            bizContent.put("timeout_express", "30m");
            request.setBizContent(bizContent.toJSONString());

            AlipayClient alipayClient = alipayConfig.getAlipayClient();
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.error("Create alipay order failed: {}", response.getMsg());
                return Result.error("创建支付订单失败：" + response.getMsg());
            }

            orderService.savePendingPayment(orderNo, order.getUserId(), amount, "alipay");
            orderService.updateQrCode(orderNo, response.getQrCode());
            log.info("Create alipay order success, orderNo={}, qrCode={}", orderNo, response.getQrCode());
            return Result.success("创建成功", response.getQrCode());
        } catch (AlipayApiException e) {
            log.error("Create alipay order exception", e);
            return Result.error("支付服务异常");
        }
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, String.join(",", requestParams.get(name)));
            }

            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
            if (!signVerified) {
                log.error("Alipay notify sign check failed");
                return "failure";
            }

            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");
            Date paymentTime = parseAlipayDate(params.get("gmt_payment"));

            Order order = orderService.getOrderByOrderNo(orderNo);
            if (order == null) {
                log.error("Alipay notify order not found: {}", orderNo);
                return "failure";
            }

            if (totalAmount != null && new BigDecimal(totalAmount).compareTo(resolvePayAmount(order)) != 0) {
                log.error("Alipay notify amount mismatch, orderNo={}, notifyAmount={}, localAmount={}", orderNo, totalAmount, resolvePayAmount(order));
                return "failure";
            }

            if (isSuccessTradeStatus(tradeStatus)) {
                orderService.updatePayStatus(orderNo, tradeNo, paymentTime);
                return "success";
            }

            if ("TRADE_CLOSED".equals(tradeStatus)) {
                orderService.markPaymentClosed(orderNo, tradeNo);
                return "success";
            }

            return "success";
        } catch (Exception e) {
            log.error("Handle alipay notify failed", e);
            return "failure";
        }
    }

    @GetMapping("/query/{orderNo}")
    public Result queryOrderStatus(@PathVariable String orderNo) {
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }

        Payment payment = orderService.getPaymentByOrderNo(orderNo);
        if (isPaid(order, payment)) {
            return Result.success("查询成功", buildStatusData(order, payment, "TRADE_SUCCESS", payment != null ? payment.getTransactionId() : "", "支付成功"));
        }
        if (order.getStatus() == 0 && payment == null) {
            log.warn("Alipay query skipped because no local payment record exists, orderNo={}", orderNo);
            return Result.success("查询成功", buildStatusData(order, null, "TRADE_NOT_EXIST", "", "支付宝交易不存在，请先创建支付单"));
        }

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toJSONString());

            AlipayTradeQueryResponse response = alipayConfig.getAlipayClient().execute(request);
            if (!response.isSuccess()) {
                if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                    log.warn("Alipay trade not found yet, orderNo={}, localPaymentStatus={}", orderNo, payment != null ? payment.getStatus() : null);
                    if (payment != null && Integer.valueOf(0).equals(payment.getStatus())) {
                        return Result.success("查询成功", buildStatusData(order, payment, "WAIT_BUYER_PAY", "", "等待付款"));
                    }
                    return Result.success("查询成功", buildStatusData(order, payment, "TRADE_NOT_EXIST", "", "支付宝交易不存在，请重新发起支付"));
                }
                String message = response.getSubMsg() != null ? response.getSubMsg() : response.getMsg();
                return Result.success("查询成功", buildStatusData(order, payment, "WAIT_BUYER_PAY", "", message != null ? message : "等待付款"));
            }

            String tradeStatus = response.getTradeStatus();
            String tradeNo = response.getTradeNo();
            Date paymentTime = response.getSendPayDate();

            if (isSuccessTradeStatus(tradeStatus)) {
                orderService.updatePayStatus(orderNo, tradeNo, paymentTime);
                order = orderService.getOrderByOrderNo(orderNo);
                payment = orderService.getPaymentByOrderNo(orderNo);
                return Result.success("支付成功", buildStatusData(order, payment, tradeStatus, tradeNo, "支付成功"));
            }

            if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                orderService.savePendingPayment(orderNo, order.getUserId(), resolvePayAmount(order), "alipay");
                payment = orderService.getPaymentByOrderNo(orderNo);
                return Result.success("查询成功", buildStatusData(order, payment, tradeStatus, tradeNo, "等待买家付款"));
            }

            if ("TRADE_CLOSED".equals(tradeStatus)) {
                orderService.markPaymentClosed(orderNo, tradeNo);
                payment = orderService.getPaymentByOrderNo(orderNo);
                return Result.success("查询成功", buildStatusData(order, payment, tradeStatus, tradeNo, "交易已关闭"));
            }

            return Result.success("查询成功", buildStatusData(order, payment, tradeStatus, tradeNo, "未完成支付"));
        } catch (AlipayApiException e) {
            log.error("Query alipay order failed", e);
            return Result.success("查询成功", buildStatusData(order, payment, "QUERY_EXCEPTION", null, "调用支付宝查询失败，已返回本地状态"));
        }
    }

    private Map<String, Object> buildStatusData(Order order, Payment payment, String tradeStatus, String tradeNo, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        boolean paid = isPaid(order, payment) || isSuccessTradeStatus(tradeStatus);
        boolean finished = paid || "TRADE_CLOSED".equals(tradeStatus);
        int paymentStatus = payment != null && payment.getStatus() != null ? payment.getStatus() : ("TRADE_CLOSED".equals(tradeStatus) ? 2 : (paid ? 1 : 0));

        data.put("orderNo", order.getOrderNo());
        data.put("paid", paid);
        data.put("finished", finished);
        data.put("orderStatus", order.getStatus());
        data.put("paymentStatus", paymentStatus);
        data.put("tradeStatus", tradeStatus);
        data.put("tradeNo", tradeNo != null ? tradeNo : (payment != null && payment.getTransactionId() != null ? payment.getTransactionId() : ""));
        data.put("paymentMethod", "alipay");
        data.put("paymentTime", payment != null ? payment.getPaymentTime() : order.getPaymentTime());
        data.put("message", message);
        return data;
    }

    private boolean isPaid(Order order, Payment payment) {
        if (payment != null && Integer.valueOf(1).equals(payment.getStatus())) {
            return true;
        }
        if (order == null || order.getStatus() == null) {
            return false;
        }
        return order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3 || order.getStatus() == 6;
    }

    private boolean isSuccessTradeStatus(String tradeStatus) {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }

    private BigDecimal resolvePayAmount(Order order) {
        return order.getPaymentAmount() != null ? order.getPaymentAmount() : order.getTotalAmount();
    }

    private Date parseAlipayDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value);
        } catch (ParseException e) {
            log.warn("Parse alipay date failed: {}", value);
            return null;
        }
    }
}
