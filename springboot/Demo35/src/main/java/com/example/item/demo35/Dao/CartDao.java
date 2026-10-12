package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartDao {
    List<Cart> selectByUserId(@Param("userId") Integer userId);

    Cart selectByUserIdAndProductId(@Param("userId") Integer userId, @Param("productId") Integer productId, @Param("specification") String specification);

    boolean insert(Cart cart);

    boolean updateQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity);

    boolean delete(@Param("id") Integer id);

    boolean deleteByUserId(@Param("userId") Integer userId);
}