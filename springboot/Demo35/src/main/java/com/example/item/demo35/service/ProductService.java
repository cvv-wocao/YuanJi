package com.example.item.demo35.service;

import com.example.item.demo35.entity.Product;
import com.example.item.demo35.entity.ProductCategory;
import com.example.item.demo35.entity.ProductFavorite;
import com.example.item.demo35.entity.ProductReview;

import java.util.List;

public interface ProductService {
    List<ProductCategory> getCategories();
    
    List<Product> getProductList(Integer categoryId, String keyword, Integer page, Integer pageSize);
    
    Product getProductDetail(Integer id);
    
    List<Product> getHotProducts(Integer limit);
    
    List<Product> getNewProducts(Integer limit);
    
    List<Product> getRecommendProducts(Integer limit);
    
    boolean addFavorite(Integer userId, Integer productId);
    
    boolean removeFavorite(Integer userId, Integer productId);
    
    List<ProductFavorite> getFavoriteList(Integer userId);
    
    boolean addReview(ProductReview review);
    
    List<ProductReview> getReviewList(Integer productId, Integer page, Integer pageSize);

    boolean createProduct(Product product);

    boolean deleteProduct(Integer id);

    boolean addCategory(ProductCategory category);

    boolean deleteCategory(Integer id);

    boolean updateProduct(Product product);

    boolean setProductHot(Integer id, boolean isHot);

    boolean setProductNew(Integer id, boolean isNew);

    boolean setProductRecommend(Integer id, boolean isRecommend);
}