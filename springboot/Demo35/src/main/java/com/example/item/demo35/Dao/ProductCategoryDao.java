package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductCategoryDao {
    List<ProductCategory> selectAllCategories();

    ProductCategory selectById(@Param("id") Integer id);

    List<ProductCategory> selectByParentId(@Param("parentId") Integer parentId);

    boolean insert(ProductCategory productCategory);

    boolean update(ProductCategory productCategory);

    boolean delete(@Param("id") Integer id);
}