package com.example.item.demo35.service.Impl;

import com.example.item.demo35.Dao.*;
import com.example.item.demo35.config.BigDataConfig;
import com.example.item.demo35.entity.*;
import com.example.item.demo35.service.ProductService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductCategoryDao productCategoryDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductFavoriteDao productFavoriteDao;

    @Autowired
    private ProductReviewDao productReviewDao;

    //==============================Kafka========================
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Gson gson = new Gson();

    @Autowired
    private BigDataConfig bigDataConfig;

    // 商品事件发送
    private void sendProductEvent(String eventType, Product product) {
        if (!bigDataConfig.isEnabled()) {
            return; // 如果大数据功能未启用，不发送 Kafka 消息
        }
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());
            event.put("product", product);
            String message = gson.toJson(event);
            kafkaTemplate.send("product-change", message);
            System.out.println("发送 Kafka 消息: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 分类事件发送
    private void sendCategoryEvent(String eventType, ProductCategory category) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", System.currentTimeMillis());
            event.put("category", category);
            String message = gson.toJson(event);
            kafkaTemplate.send("category-change", message);
            System.out.println("发送 Kafka 消息: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //==============================Kafka========================

    // 查询方法（无事件）
    @Override
    public List<ProductCategory> getCategories() {
        return productCategoryDao.selectAllCategories();
    }

    @Override
    public List<Product> getProductList(Integer categoryId, String keyword, Integer page, Integer pageSize) {
        if (categoryId != null) {
            return productDao.selectByCategoryId(categoryId);
        } else if (keyword != null && !keyword.isEmpty()) {
            return productDao.selectByKeyword(keyword);
        } else {
            return productDao.selectAllProducts();
        }
    }

    @Override
    public Product getProductDetail(Integer id) {
        return productDao.selectById(id);
    }

    @Override
    public List<Product> getHotProducts(Integer limit) {
        return productDao.selectHotProducts(limit);
    }

    @Override
    public List<Product> getNewProducts(Integer limit) {
        return productDao.selectNewProducts(limit);
    }

    @Override
    public List<Product> getRecommendProducts(Integer limit) {
        return productDao.selectRecommendProducts(limit);
    }

    // 收藏（写操作）
    @Override
    public boolean addFavorite(Integer userId, Integer productId) {
        ProductFavorite existing = productFavoriteDao.selectByUserIdAndProductId(userId, productId);
        if (existing != null) {
            return false;
        }
        ProductFavorite favorite = new ProductFavorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        boolean result = productFavoriteDao.insert(favorite);
        if (result) {
            // 查询商品信息用于事件
            Product product = productDao.selectById(productId);
            if (product != null) {
                sendProductEvent("ADD_FAVORITE", product);
                System.out.println("商品收藏事件已发送，商品ID：" + productId);
            }
        }
        return result;
    }

    @Override
    public boolean removeFavorite(Integer userId, Integer productId) {
        Product product = productDao.selectById(productId);
        boolean result = productFavoriteDao.delete(userId, productId);
        if (result && product != null) {
            sendProductEvent("REMOVE_FAVORITE", product);
            System.out.println("取消收藏事件已发送，商品ID：" + productId);
        }
        return result;
    }

    @Override
    public List<ProductFavorite> getFavoriteList(Integer userId) {
        return productFavoriteDao.selectByUserId(userId);
    }

    // 评论（写操作）
    @Override
    public boolean addReview(ProductReview review) {
        boolean result = productReviewDao.insert(review);
        if (result && review.getProductId() != null) {
            Product product = productDao.selectById(review.getProductId());
            if (product != null) {
                sendProductEvent("ADD_REVIEW", product);
                System.out.println("商品评论事件已发送，商品ID：" + review.getProductId());
            }
        }
        return result;
    }

    @Override
    public List<ProductReview> getReviewList(Integer productId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return productReviewDao.selectByProductId(productId, offset, pageSize);
    }

    // 商品创建
    @Override
    public boolean createProduct(Product product) {
        boolean result = productDao.insert(product);
        System.out.println("创建产品: " + product);
        if (result) {
            sendProductEvent("CREATE_PRODUCT", product);
            System.out.println("创建产品阶段，已发布至Kafka");
        }
        return result;
    }

    // 商品删除
    @Override
    public boolean deleteProduct(Integer id) {
        Product product = productDao.selectById(id);
        if (product == null) {
            return false;
        }
        boolean result = productDao.delete(id);
        System.out.println("删除商品，商品id为" + product.getName());
        if (result) {
            sendProductEvent("DELETE_PRODUCT", product);
            System.out.println("删除商品阶段，已发布至Kafka");
        }
        return result;
    }

    // 商品更新
    @Override
    public boolean updateProduct(Product product) {
        boolean result = productDao.update(product);
        if (result && product.getId() != null) {
            Product updatedProduct = productDao.selectById(product.getId());
            if (updatedProduct != null) {
                sendProductEvent("UPDATE_PRODUCT", updatedProduct);
                System.out.println("更新商品事件已发送，商品ID：" + product.getId());
            }
        }
        return result;
    }

    // 设置热门
    @Override
    public boolean setProductHot(Integer id, boolean isHot) {
        Product product = productDao.selectById(id);
        if (product == null) {
            return false;
        }
        product.setIsHot(isHot ? 1 : 0);
        boolean result = productDao.update(product);
        if (result) {
            sendProductEvent("SET_HOT", product);
            System.out.println("设置热门状态事件已发送，商品ID：" + id);
        }
        return result;
    }

    // 设置新品
    @Override
    public boolean setProductNew(Integer id, boolean isNew) {
        Product product = productDao.selectById(id);
        if (product == null) {
            return false;
        }
        product.setIsNew(isNew ? 1 : 0);
        boolean result = productDao.update(product);
        if (result) {
            sendProductEvent("SET_NEW", product);
            System.out.println("设置新品状态事件已发送，商品ID：" + id);
        }
        return result;
    }

    // 设置推荐
    @Override
    public boolean setProductRecommend(Integer id, boolean isRecommend) {
        Product product = productDao.selectById(id);
        if (product == null) {
            return false;
        }
        product.setIsRecommend(isRecommend ? 1 : 0);
        boolean result = productDao.update(product);
        if (result) {
            sendProductEvent("SET_RECOMMEND", product);
            System.out.println("设置推荐状态事件已发送，商品ID：" + id);
        }
        return result;
    }

    // 分类增加
    @Override
    public boolean addCategory(ProductCategory category) {
        boolean result = productCategoryDao.insert(category);
        if (result) {
            sendCategoryEvent("ADD_CATEGORY", category);
            System.out.println("添加分类事件已发送，分类名称：" + category.getName());
        }
        return result;
    }

    // 分类删除
    @Override
    public boolean deleteCategory(Integer id) {
        ProductCategory category = productCategoryDao.selectById(id);
        if (category == null) {
            return false;
        }
        boolean result = productCategoryDao.delete(id);
        if (result) {
            sendCategoryEvent("DELETE_CATEGORY", category);
            System.out.println("删除分类事件已发送，分类名称：" + category.getName());
        }
        return result;
    }
}
