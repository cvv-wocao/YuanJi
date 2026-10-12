package com.example.item.demo35.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ProductCategory {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sortOrder;
    private String icon;
    private String description;
    private Date createTime;
    private Date updateTime;
}