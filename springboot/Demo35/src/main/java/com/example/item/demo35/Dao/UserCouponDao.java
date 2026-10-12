package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCouponDao {
    List<UserCoupon> selectByUserId(@Param("userId") Integer userId, @Param("status") Integer status);

    UserCoupon selectById(@Param("id") Integer id);

    UserCoupon selectByUserIdAndCouponId(@Param("userId") Integer userId, @Param("couponId") Integer couponId);

    boolean insert(UserCoupon userCoupon);

    boolean update(UserCoupon userCoupon);

    boolean delete(@Param("id") Integer id);
}