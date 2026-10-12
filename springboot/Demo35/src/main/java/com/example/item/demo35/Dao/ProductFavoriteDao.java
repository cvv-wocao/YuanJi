package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.ProductFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductFavoriteDao {
    List<ProductFavorite> selectByUserId(@Param("userId") Integer userId);

    ProductFavorite selectByUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId);

    boolean insert(ProductFavorite productFavorite);

    boolean delete(@Param("userId") Integer userId, @Param("productId") Integer productId);
}