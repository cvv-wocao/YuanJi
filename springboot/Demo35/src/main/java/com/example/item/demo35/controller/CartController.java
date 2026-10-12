package com.example.item.demo35.controller;

import com.example.item.demo35.entity.Cart;
import com.example.item.demo35.service.CartService;
import com.example.item.demo35.service.UserService;
import com.example.item.demo35.util.JwtUtils;
import com.example.item.demo35.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

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
        return userService.selectIdByName(username);
    }

    @PostMapping("/add")
    public Result addToCart(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String specification) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = cartService.addToCart(userId, productId, quantity, specification);
            if (success) {
                result.setSuccess(true);
                result.setMessage("添加购物车成功");
            } else {
                result.setSuccess(false);
                result.setMessage("添加购物车失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("添加购物车失败: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/list")
    public Result getCartList(@RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            List<Cart> carts = cartService.getCartList(userId);
            result.setSuccess(true);
            result.setMessage("获取购物车列表成功");
            result.setData(carts);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("获取购物车列表失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/update")
    public Result updateCartQuantity(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer cartId,
            @RequestParam Integer quantity) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = cartService.updateCartQuantity(userId, cartId, quantity);
            if (success) {
                result.setSuccess(true);
                result.setMessage("更新购物车成功");
            } else {
                result.setSuccess(false);
                result.setMessage("更新购物车失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("更新购物车失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/delete")
    public Result deleteFromCart(
            @RequestHeader("Authorization") String token,
            @RequestParam Integer cartId) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = cartService.deleteFromCart(userId, cartId);
            if (success) {
                result.setSuccess(true);
                result.setMessage("删除购物车商品成功");
            } else {
                result.setSuccess(false);
                result.setMessage("删除购物车商品失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("删除购物车商品失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/clear")
    public Result clearCart(@RequestHeader("Authorization") String token) {
        Result result = new Result();
        try {
            Integer userId = getUserIdFromToken(token);
            if (userId == null) {
                result.setSuccess(false);
                result.setMessage("用户未登录");
                return result;
            }
            boolean success = cartService.clearCart(userId);
            System.out.println(success);
            if (success) {
                result.setSuccess(true);
                result.setMessage("清空购物车成功");
            } else {
                result.setSuccess(false);
                System.out.println("清空购物车失败"+"=================================");
                result.setMessage("清空购物车失败");
            }
        } catch (Exception e) {
            result.setSuccess(false);
            System.out.println("清空购物车失败: " + e.getMessage());
            result.setMessage("清空购物车失败: " + e.getMessage());
        }
        return result;
    }
}
