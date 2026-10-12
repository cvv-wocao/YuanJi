package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentDao {
    boolean insert(Payment payment);

    Payment selectByOrderNo(@Param("orderNo") String orderNo);

    boolean update(Payment payment);
}
