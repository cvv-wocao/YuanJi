package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemDao {
    List<OrderItem> selectByOrderId(@Param("orderId") Integer orderId);

    List<OrderItem> selectByUserId(@Param("userId") Integer userId);

    boolean insert(OrderItem orderItem);

    boolean delete(@Param("orderId") Integer orderId);
}