package com.example.item.demo35.service;

import com.example.item.demo35.entity.Coupon;
import com.example.item.demo35.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService {
    List<Coupon> getAvailableCoupons(Integer userId, BigDecimal amount);
    
    List<UserCoupon> getMyCoupons(Integer userId, Integer status);
    
    boolean receiveCoupon(Integer userId, Integer couponId);
    
    Map<String, Object> calculateDiscount(Integer userId, BigDecimal amount, Integer couponId);
}