package com.example.item.demo35.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Payment {
    private Integer id;
    private String orderNo;        // 订单号
    private Integer userId;        // 用户ID
    private BigDecimal amount;      // 支付金额
    private String paymentMethod;   // 支付方式：wechat, alipay, bank
    private Integer status;         // 支付状态：0-待支付，1-支付成功，2-支付失败
    private String transactionId;   // 第三方交易流水号
    private Date paymentTime;       // 支付时间
    private Date createTime;        // 创建时间
}
