package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Coupon;
import com.example.item.demo35.entity.UserCoupon;
import com.example.item.demo35.service.CouponService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @Autowired
    private JwtUtils jwtUtils;

    private Integer getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (!jwtUtils.validateToken(actualToken)) {
            return null;
        }
        String username = jwtUtils.getUsernameFromToken(actualToken);
        return null;
    }

    @GetMapping("/available")
    public Result getAvailableCoupons(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "0") BigDecimal amount) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            List<Coupon> coupons = couponService.getAvailableCoupons(userId, amount);
            result.setSuccess(true);
            result.setMessage("获取可用优惠券成功");
            result.setData(coupons);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取可用优惠券失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/my")
    public Result getMyCoupons(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer status) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            List<UserCoupon> coupons = couponService.getMyCoupons(userId, status);
            result.setSuccess(true);
            result.setMessage("获取我的优惠券成功");
            result.setData(coupons);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取我的优惠券失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/receive")
    public Result receiveCoupon(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer couponId) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = couponService.receiveCoupon(userId, couponId);
            if (success) {
                result.setSuccess(true);
                result.setMessage("领取优惠券成功");
            } else {
                result.setSuccess(false);
                result.setMessage("领取优惠券失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("领取优惠券失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/calculate")
    public Result calculateDiscount(
            @RequestHeader("Authorization") String token,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) Integer couponId) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            Map<String, Object> discountInfo = couponService.calculateDiscount(userId, amount, couponId);
            result.setSuccess(true);
            result.setMessage("计算优惠成功");
            result.setData(discountInfo);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("计算优惠失败: " + e.getMessage());
        }
        return result;
    }
}