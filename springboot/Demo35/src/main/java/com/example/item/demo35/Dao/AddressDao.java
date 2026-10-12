package com.example.item.demo35.Dao;

import com.example.item.demo35.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AddressDao {
    List<Address> selectByUserId(@Param("userId") Integer userId);

    Address selectById(@Param("id") Integer id);

    Address selectDefaultByUserId(@Param("userId") Integer userId);

    boolean insert(Address address);

    boolean update(Address address);

    boolean delete(@Param("id") Integer id);

    boolean setDefault(@Param("userId") Integer userId, @Param("id") Integer id);
}