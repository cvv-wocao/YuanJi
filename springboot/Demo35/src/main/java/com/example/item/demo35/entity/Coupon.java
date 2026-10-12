package com.example.item.demo35.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Coupon {
    private Integer id;
    private String name;
    private Integer type;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal minAmount;
    private BigDecimal maxDiscount;
    private Integer totalCount;
    private Integer usedCount;
    private Integer validDays;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private String description;
    private Date createTime;
    private Date updateTime;
}