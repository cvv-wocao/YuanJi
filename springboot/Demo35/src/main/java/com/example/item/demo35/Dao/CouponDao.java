package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponDao {
    List<Coupon> selectAllCoupons();

    List<Coupon> selectAvailableCoupons(@Param("amount") java.math.BigDecimal amount);

    Coupon selectById(@Param("id") Integer id);

    boolean insert(Coupon coupon);

    boolean update(Coupon coupon);

    boolean updateUsedCount(@Param("id") Integer id, @Param("usedCount") Integer usedCount);

    boolean delete(@Param("id") Integer id);
}