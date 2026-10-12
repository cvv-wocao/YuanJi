package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ProductReview {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private Integer userId;
    private Integer rating;
    private String content;
    private String images;
    private Integer isAnonymous;
    private Date createTime;
    private Date updateTime;
}