package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class UserCoupon {
    private Integer id;
    private Integer userId;
    private Integer couponId;
    private Integer orderId;
    private Integer status;
    private Date receiveTime;
    private Date useTime;
    private Date expireTime;
}