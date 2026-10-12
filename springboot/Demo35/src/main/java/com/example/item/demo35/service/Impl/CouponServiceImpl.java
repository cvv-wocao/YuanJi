package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.CouponDao;
import com.example.item.demo35.Dao.UserCouponDao;
import com.example.item.demo35.entity.Coupon;
import com.example.item.demo35.entity.UserCoupon;
import com.example.item.demo35.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

    @Autowired
    private CouponDao couponDao;

    @Autowired
    private UserCouponDao userCouponDao;

    @Override
    public List<Coupon> getAvailableCoupons(Integer userId, BigDecimal amount) {
        List<Coupon> availableCoupons = couponDao.selectAvailableCoupons(amount);
        List<Coupon> result = new ArrayList<>();
        
        for (Coupon coupon : availableCoupons) {
            UserCoupon existing = userCouponDao.selectByUserIdAndCouponId(userId, coupon.getId());
            if (existing == null && coupon.getUsedCount() < coupon.getTotalCount()) {
                result.add(coupon);
            }
        }
        
        return result;
    }

    @Override
    public List<UserCoupon> getMyCoupons(Integer userId, Integer status) {
        return userCouponDao.selectByUserId(userId, status);
    }

    @Override
    public boolean receiveCoupon(Integer userId, Integer couponId) {
        Coupon coupon = couponDao.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1 || coupon.getUsedCount() >= coupon.getTotalCount()) {
            return false;
        }

        UserCoupon existing = userCouponDao.selectByUserIdAndCouponId(userId, couponId);
        if (existing != null) {
            return false;
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(0);
        userCoupon.setReceiveTime(new Date());
        
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, coupon.getValidDays());
        userCoupon.setExpireTime(calendar.getTime());

        if (userCouponDao.insert(userCoupon)) {
            couponDao.updateUsedCount(couponId, coupon.getUsedCount() + 1);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> calculateDiscount(Integer userId, BigDecimal amount, Integer couponId) {
        Map<String, Object> result = new HashMap<>();
        BigDecimal discount = BigDecimal.ZERO;
        
        if (couponId != null) {
            UserCoupon userCoupon = userCouponDao.selectById(couponId);
            if (userCoupon != null && userCoupon.getUserId().equals(userId) && userCoupon.getStatus() == 0) {
                Coupon coupon = couponDao.selectById(userCoupon.getCouponId());
                if (coupon != null && amount.compareTo(coupon.getMinAmount()) >= 0) {
                    if (coupon.getType() == 1) {
                        discount = coupon.getDiscountAmount();
                    } else if (coupon.getType() == 2) {
                        discount = amount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
                        if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                            discount = coupon.getMaxDiscount();
                        }
                    }
                }
            }
        }
        
        result.put("discount", discount);
        result.put("finalAmount", amount.subtract(discount));
        return result;
    }
}