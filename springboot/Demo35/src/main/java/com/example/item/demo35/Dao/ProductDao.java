package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductDao {
    List<Product> selectAllProducts();

    List<Product> selectByCategoryId(@Param("categoryId") Integer categoryId);

    List<Product> selectByKeyword(@Param("keyword") String keyword);

    List<Product> selectHotProducts(@Param("limit") Integer limit);

    List<Product> selectNewProducts(@Param("limit") Integer limit);

    List<Product> selectRecommendProducts(@Param("limit") Integer limit);

    Product selectById(@Param("id") Integer id);

    boolean insert(Product product);

    boolean update(Product product);

    boolean delete(@Param("id") Integer id);

    Integer countProducts();

    boolean updateStock(@Param("id") Integer id, @Param("quantity") Integer quantity);

    boolean updateSales(@Param("id") Integer id, @Param("sales") Integer sales);
}