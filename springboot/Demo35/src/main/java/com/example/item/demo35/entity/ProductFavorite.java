package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ProductFavorite {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private Date createTime;
}