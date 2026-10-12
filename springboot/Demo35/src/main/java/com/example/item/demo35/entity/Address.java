package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Address {
    private Integer id;
    private Integer userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
    private Date createTime;
    private Date updateTime;
}