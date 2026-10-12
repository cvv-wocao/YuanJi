package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Cart {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Integer quantity;
    private String specification;
    private Date createTime;
    private Date updateTime;
}