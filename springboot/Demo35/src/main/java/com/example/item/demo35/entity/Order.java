package com.example.item.demo35.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Integer id;
    private String orderNo;
    private Integer userId;
    private BigDecimal totalAmount;
    private BigDecimal paymentAmount;
    private Integer status;
    private String paymentMethod;
    private Date paymentTime;
    private Date shippingTime;
    private Date receiveTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private String cancelReason;
    private Date createTime;
    private Date updateTime;
}