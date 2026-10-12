package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.CartDao;
import com.example.item.demo35.Dao.ProductDao;
import com.example.item.demo35.entity.Cart;
import com.example.item.demo35.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired
    private CartDao cartDao;

    @Autowired
    private ProductDao productDao;

    @Override
    public boolean addToCart(Integer userId, Integer productId, Integer quantity, String specification) {
        Cart existing = cartDao.selectByUserIdAndProductId(userId, productId, specification);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            return cartDao.updateQuantity(existing.getId(), existing.getQuantity());
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setSpecification(specification);
            return cartDao.insert(cart);
        }
    }

    @Override
    public List<Cart> getCartList(Integer userId) {
        return cartDao.selectByUserId(userId);
    }

    @Override
    public boolean updateCartQuantity(Integer userId, Integer cartId, Integer quantity) {
        return cartDao.updateQuantity(cartId, quantity);
    }

    @Override
    public boolean deleteFromCart(Integer userId, Integer cartId) {
        return cartDao.delete(cartId);
    }

    @Override
    public boolean clearCart(Integer userId) {
        return cartDao.deleteByUserId(userId);
    }
}