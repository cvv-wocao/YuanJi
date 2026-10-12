package com.example.item.demo35.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Product {
    private Integer id;
    private Integer categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private String images;
    private String thumbnail;
    private String specifications;
    private Integer status;
    private Integer isHot;
    private Integer isNew;
    private Integer isRecommend;
    private Date createTime;
    private Date updateTime;
}