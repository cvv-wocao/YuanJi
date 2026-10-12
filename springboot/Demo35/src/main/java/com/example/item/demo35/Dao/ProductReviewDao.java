package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductReviewDao {
    List<ProductReview> selectByProductId(@Param("productId") Integer productId, @Param("page") Integer page, @Param("pageSize") Integer pageSize);

    List<ProductReview> selectByUserId(@Param("userId") Integer userId);

    ProductReview selectById(@Param("id") Integer id);

    boolean insert(ProductReview productReview);

    boolean update(ProductReview productReview);

    boolean delete(@Param("id") Integer id);
}