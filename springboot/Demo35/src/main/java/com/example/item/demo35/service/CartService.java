package com.example.item.demo35.service;

import com.example.item.demo35.entity.Cart;

import java.util.List;

public interface CartService {
    boolean addToCart(Integer userId, Integer productId, Integer quantity, String specification);
    
    List<Cart> getCartList(Integer userId);
    
    boolean updateCartQuantity(Integer userId, Integer cartId, Integer quantity);
    
    boolean deleteFromCart(Integer userId, Integer cartId);
    
    boolean clearCart(Integer userId);
}